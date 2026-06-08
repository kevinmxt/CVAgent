package me.maxt.cv.web.route;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import me.maxt.cv.agent.ChatModelProvider;
import me.maxt.cv.agent.orchestrator.CvGenerationOrchestrator;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.config.AgentPromptConfig;
import me.maxt.cv.config.AppConfig;
import me.maxt.cv.config.AppConfig.TailorConfig;
import me.maxt.cv.service.CvGenerationService;
import me.maxt.cv.service.CvScoringResultService;
import me.maxt.cv.service.ExportService;
import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.CvScoringResult;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.web.dto.request.CvContentUpdateRequest;
import me.maxt.cv.web.dto.request.CvGenerateRequest;
import me.maxt.cv.web.dto.response.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CV 生成 REST 路由。
 *
 * <p>所有路由以 {@code /api/v1/cv-generations} 为前缀。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvGenerationRoutes {

    private static final Logger log = LoggerFactory.getLogger(CvGenerationRoutes.class);
    private static final String PREFIX = "/api/v1/cv-generations";

    private final CvGenerationService cvGenService;
    private final CvScoringResultService scoringResultService;
    private final ExportService exportService;
    private final AppConfig config;
    private final AgentPromptConfig promptConfig;
    private final JobDescriptionRepository jdRepo;
    private final ExecutorService scoreExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cv-score-worker");
        t.setDaemon(true);
        return t;
    });

    public CvGenerationRoutes(CvGenerationService cvGenService,
                               CvScoringResultService scoringResultService,
                               ExportService exportService,
                               AppConfig config, AgentPromptConfig promptConfig,
                               JobDescriptionRepository jdRepo) {
        this.cvGenService = cvGenService;
        this.scoringResultService = scoringResultService;
        this.exportService = exportService;
        this.config = config;
        this.promptConfig = promptConfig;
        this.jdRepo = jdRepo;
    }

    public void register(Javalin app) {
        app.get(PREFIX, this::handleList);
        app.post(PREFIX + "/generate", this::handleGenerate);
        app.post(PREFIX + "/{id}/score", this::handleScore);
        app.get(PREFIX + "/{id}/scoring-results", this::handleListScoringResults);
        app.get(PREFIX + "/{id}/scoring-results/{srId}/history", this::handleGetHistory);
        app.post(PREFIX + "/{id}/optimize", this::handleOptimize);
        app.get(PREFIX + "/{id}", this::handleGetById);
        app.get(PREFIX + "/{id}/preview", this::handlePreview);
        app.put(PREFIX + "/{id}", this::handleUpdate);
        app.post(PREFIX + "/{id}/export", this::handleExport);
        app.post(PREFIX + "/{id}/duplicate", this::handleDuplicate);
        app.delete(PREFIX + "/{id}", this::handleDelete);
    }

    private void handleList(Context ctx) {
        int page = parseIntParam(ctx, "page", 1);
        int size = parseIntParam(ctx, "size", 10);
        List<GeneratedCv> items = cvGenService.listGeneratedCvs(page, size);
        int total = cvGenService.count();
        ctx.json(new PageResult<>(items, page, size, total));
    }

    private void handleGenerate(Context ctx) {
        CvGenerateRequest request = ctx.bodyAsClass(CvGenerateRequest.class);
        request.validate();

        CvGenerationService.GenerationContext context = cvGenService.loadContext(
                request.getWorkExpId(), request.getTemplateId());

        var workExp = context.getWorkExperience();
        boolean hasContent = (workExp.getProfessionalExp() != null && !workExp.getProfessionalExp().isEmpty())
                || (workExp.getSummary() != null && !workExp.getSummary().isEmpty())
                || (workExp.getSkills() != null && !workExp.getSkills().isEmpty())
                || (workExp.getEducation() != null && !workExp.getEducation().isEmpty());
        if (!hasContent) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "该工作经历缺少有效内容，请重新导入简历文件或手动编辑补充内容后再生成");
        }

        String initialCv = cvGenService.fillTemplate(
                context.getWorkExperience(), context.getTemplate());

        // 阶段一自动优化：修排版/措辞
        String optimizedCv = initialCv;
        try {
            CvGenerationOrchestrator orchestrator = new CvGenerationOrchestrator(
                    ChatModelProvider.createChatModel(config), promptConfig);
            optimizedCv = orchestrator.performTailoring(initialCv, "",
                    promptConfig.getTailorConfig());
            log.info("阶段一优化完成: length={} -> {}", initialCv.length(), optimizedCv.length());
        } catch (Exception e) {
            log.warn("阶段一优化失败，使用原始填表结果: {}", e.getMessage());
            optimizedCv = initialCv;
        }

        GeneratedCv generatedCv = new GeneratedCv();
        generatedCv.setWorkExpId(request.getWorkExpId());
        generatedCv.setTemplateId(request.getTemplateId());
        generatedCv.setFinalContent(optimizedCv);
        generatedCv.setStatus(GeneratedCv.STATUS_DRAFT);

        GeneratedCv saved = cvGenService.saveGeneratedCv(generatedCv);
        log.info("CV 生成完成: id={}", saved.getId());

        ctx.status(HttpStatus.CREATED);
        ctx.json(saved);
    }

    private void handleScore(Context ctx) {
        Long id = parseId(ctx);
        Long jdId = parseLongParam(ctx, "jdId");
        if (jdId == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "jdId 为必填项");
        }
        jdRepo.findById(jdId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, jdId));

        GeneratedCv cv = cvGenService.getGeneratedCv(id);

        // 创建评分结果记录
        CvScoringResult scoringResult = scoringResultService.create(id, jdId);

        scoreExecutor.submit(() -> {
            try {
                log.info("开始异步评分: generatedCvId={}, jdId={}, scoringResultId={}", id, jdId, scoringResult.getId());
                var jd = jdRepo.findById(jdId).orElseThrow();
                CvGenerationOrchestrator orchestrator = new CvGenerationOrchestrator(
                        ChatModelProvider.createChatModel(config), promptConfig);

                var reviewResult = orchestrator.review(cv.getFinalContent(), jd.getContent());

                Map<String, Double> scoresOnly = new LinkedHashMap<>();
                reviewResult.getRoleResults().forEach((key, val) -> scoresOnly.put(key, val.getScore()));
                String roleScoresJson = cvGenService.toRoleScoresJson(scoresOnly);
                scoringResultService.complete(scoringResult.getId(),
                        reviewResult.getOverallScore(),
                        reviewResult.getCombinedFeedback(),
                        roleScoresJson,
                        1);

                scoringResultService.saveIterationRecord(scoringResult.getId(),
                        1,
                        roleScoresJson,
                        reviewResult.getOverallScore(),
                        reviewResult.getCombinedFeedback(),
                        cv.getFinalContent());

                log.info("异步评分完成: scoringResultId={}, score={}",
                        scoringResult.getId(), reviewResult.getOverallScore());
            } catch (Exception e) {
                log.error("异步评分失败: scoringResultId={}", scoringResult.getId(), e);
                scoringResultService.fail(scoringResult.getId());
            }
        });

        log.info("已提交异步评分: scoringResultId={}", scoringResult.getId());
        ctx.status(202);
        ctx.json(scoringResult);
    }

    private void handleOptimize(Context ctx) {
        Long id = parseId(ctx);
        Long srId = parseLongParam(ctx, "srId");
        if (srId == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "srId 为必填项");
        }

        GeneratedCv cv = cvGenService.getGeneratedCv(id);
        CvScoringResult sr = scoringResultService.getById(srId);

        if (!CvScoringResult.STATUS_COMPLETED.equals(sr.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "该评分尚未完成，无法优化");
        }

        // 加载 JD 内容
        var jd = jdRepo.findById(sr.getJdId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, sr.getJdId()));

        // 阶段二优化：基于 CV + JD + 评审反馈
        TailorConfig tailorConfig = promptConfig.getPostScoringTailorConfig();
        String resolvedSystemPrompt = tailorConfig.getSystemPrompt()
                .replace("{{cv}}", cv.getFinalContent())
                .replace("{{jd}}", jd.getContent());
        String resolvedUserPrompt = tailorConfig.getUserPrompt()
                .replace("{{cv}}", cv.getFinalContent())
                .replace("{{cvReview}}", sr.getFinalFeedback() != null ? sr.getFinalFeedback() : "");

        CvGenerationOrchestrator orchestrator = new CvGenerationOrchestrator(
                ChatModelProvider.createChatModel(config), promptConfig);
        String optimized = orchestrator.performTailoring(cv.getFinalContent(),
                sr.getFinalFeedback() != null ? sr.getFinalFeedback() : "",
                tailorConfig);

        log.info("阶段二优化完成: cvId={}, srId={}, length={}", id, srId, optimized.length());

        ctx.json(java.util.Map.of("optimizedContent", optimized));
    }

    private void handleListScoringResults(Context ctx) {
        Long id = parseId(ctx);
        cvGenService.getGeneratedCv(id);
        List<CvScoringResult> results = scoringResultService.listByGeneratedCvId(id);
        ctx.json(results);
    }

    private void handleGetHistory(Context ctx) {
        Long cvId = parseId(ctx);
        cvGenService.getGeneratedCv(cvId);
        Long srId = parseLongPathParam(ctx, "srId");
        scoringResultService.getById(srId);
        List<CvGenerationRecord> history = scoringResultService.getIterationHistory(srId);
        ctx.json(history);
    }

    private void handleGetById(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);
        ctx.json(cv);
    }

    private void handlePreview(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);

        String content = cv.getFinalContent();
        if (content == null || content.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "简历内容为空");
        }

        ctx.contentType("text/html; charset=UTF-8");
        ctx.result(content);
    }

    private void handleUpdate(Context ctx) {
        Long id = parseId(ctx);
        CvContentUpdateRequest request = ctx.bodyAsClass(CvContentUpdateRequest.class);
        request.validate();

        GeneratedCv updated = cvGenService.updateContent(id, request.getFinalContent());
        ctx.json(updated);
    }

    private void handleExport(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);

        InputStream fileStream = exportService.exportAsHtml(cv);
        String fileName = exportService.generateFileName(cv);

        cvGenService.markExported(id);

        ctx.contentType(exportService.getContentType());
        ctx.header("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        ctx.result(fileStream);
    }

    private void handleDuplicate(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv result = cvGenService.duplicate(id);
        ctx.status(201);
        ctx.json(result);
    }

    private void handleDelete(Context ctx) {
        Long id = parseId(ctx);
        cvGenService.deleteGeneratedCv(id);
        ctx.status(204);
    }

    private Long parseId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "无效的 ID 格式");
        }
    }

    private Long parseLongParam(Context ctx, String key) {
        String val = ctx.queryParam(key);
        if (val == null || val.isEmpty()) return null;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, key + " 必须为有效数字");
        }
    }

    private Long parseLongPathParam(Context ctx, String key) {
        try {
            return Long.parseLong(ctx.pathParam(key));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "无效的 " + key + " 格式");
        }
    }

    private int parseIntParam(Context ctx, String key, int defaultValue) {
        String val = ctx.queryParam(key);
        if (val == null || val.isEmpty()) return defaultValue;
        try {
            int parsed = Integer.parseInt(val);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
