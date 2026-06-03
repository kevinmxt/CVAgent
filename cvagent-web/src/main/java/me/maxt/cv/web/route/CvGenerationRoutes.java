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
import me.maxt.cv.service.CvGenerationService;
import me.maxt.cv.service.ExportService;
import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.web.dto.request.CvContentUpdateRequest;
import me.maxt.cv.web.dto.request.CvGenerateRequest;
import me.maxt.cv.web.dto.response.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CV 生成 REST 路由，负责映射 HTTP 端点并调用业务服务和 Agent 模块。
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
    private final ExportService exportService;
    private final AppConfig config;
    private final AgentPromptConfig promptConfig;
    private final JobDescriptionRepository jdRepo;
    private final ExecutorService scoreExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "cv-score-worker");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造 CV 生成路由。
     *
     * @param cvGenService  CV 生成业务服务
     * @param exportService 导出服务
     * @param config        应用配置
     * @param promptConfig  Agent 提示词配置
     * @param jdRepo        岗位描述数据访问对象
     */
    public CvGenerationRoutes(CvGenerationService cvGenService, ExportService exportService,
                               AppConfig config, AgentPromptConfig promptConfig,
                               JobDescriptionRepository jdRepo) {
        this.cvGenService = cvGenService;
        this.exportService = exportService;
        this.config = config;
        this.promptConfig = promptConfig;
        this.jdRepo = jdRepo;
    }

    /**
     * 向 Javalin 实例注册所有端点。
     *
     * @param app Javalin 实例
     */
    public void register(Javalin app) {
        app.get(PREFIX, this::handleList);
        app.post(PREFIX + "/generate", this::handleGenerate);
        app.post(PREFIX + "/{id}/score", this::handleScore);
        app.get(PREFIX + "/{id}", this::handleGetById);
        app.get(PREFIX + "/{id}/history", this::handleGetHistory);
        app.get(PREFIX + "/{id}/preview", this::handlePreview);
        app.put(PREFIX + "/{id}", this::handleUpdate);
        app.post(PREFIX + "/{id}/export", this::handleExport);
        app.delete(PREFIX + "/{id}", this::handleDelete);
    }

    /**
     * 分页查询所有生成的简历列表。
     */
    private void handleList(Context ctx) {
        int page = parseIntParam(ctx, "page", 1);
        int size = parseIntParam(ctx, "size", 10);
        List<GeneratedCv> items = cvGenService.listGeneratedCvs(page, size);
        int total = cvGenService.count();
        ctx.json(new PageResult<>(items, page, size, total));
    }

    /**
     * 处理简历生成请求（仅填模板保存，不执行评分循环）。
     *
     * <p>评分需要通过 {@code POST /{id}/score} 单独触发。</p>
     */
    private void handleGenerate(Context ctx) {
        CvGenerateRequest request = ctx.bodyAsClass(CvGenerateRequest.class);
        request.validate();

        // 1. 加载关联数据
        CvGenerationService.GenerationContext context = cvGenService.loadContext(
                request.getWorkExpId(), request.getTemplateId(), request.getJdId());

        // 验证工作经历是否有足够的内容
        var workExp = context.getWorkExperience();
        boolean hasContent = (workExp.getProfessionalExp() != null && !workExp.getProfessionalExp().isEmpty())
                || (workExp.getSummary() != null && !workExp.getSummary().isEmpty())
                || (workExp.getSkills() != null && !workExp.getSkills().isEmpty())
                || (workExp.getEducation() != null && !workExp.getEducation().isEmpty());
        if (!hasContent) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "该工作经历缺少有效内容，请重新导入简历文件或手动编辑补充内容后再生成");
        }

        // 2. 填充模板生成初始 HTML 简历
        String initialCv = cvGenService.fillTemplate(
                context.getWorkExperience(), context.getTemplate());

        // 3. 持久化（无评分数据）
        GeneratedCv generatedCv = new GeneratedCv();
        generatedCv.setWorkExpId(request.getWorkExpId());
        generatedCv.setTemplateId(request.getTemplateId());
        generatedCv.setJdId(request.getJdId());
        generatedCv.setFinalContent(initialCv);
        generatedCv.setFinalScore(null);
        generatedCv.setFinalFeedback(null);
        generatedCv.setRoleScores(null);
        generatedCv.setIterationCount(0);
        generatedCv.setStatus(GeneratedCv.STATUS_DRAFT);

        GeneratedCv saved = cvGenService.saveGeneratedCv(generatedCv);

        log.info("CV 生成完成（待评分）: id={}", saved.getId());

        ctx.status(HttpStatus.CREATED);
        ctx.json(saved);
    }

    /**
     * 异步评分：将状态设为 SCORING，后台线程执行评分循环，完成后回写结果。
     */
    private void handleScore(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);

        if (GeneratedCv.STATUS_SCORING.equals(cv.getStatus())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "该简历正在评分中，请稍后再试");
        }

        // 设置评分中状态
        cv.setStatus(GeneratedCv.STATUS_SCORING);
        cvGenService.saveGeneratedCv(cv);

        // 异步执行评分
        scoreExecutor.submit(() -> {
            try {
                log.info("开始异步评分: id={}", id);

                // 加载 JD 内容
                var jd = jdRepo.findById(cv.getJdId())
                        .orElseThrow(() -> new AppException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, cv.getJdId()));

                // 执行评分循环
                CvGenerationOrchestrator orchestrator = new CvGenerationOrchestrator(
                        ChatModelProvider.createChatModel(config), promptConfig);

                CvGenerationOrchestrator.CvGenerationResult agentResult =
                        orchestrator.generate(cv.getFinalContent(), jd.getContent());

                // 回写评分结果
                cvGenService.updateScores(id,
                        agentResult.getFinalReview().getOverallScore(),
                        agentResult.getFinalReview().getCombinedFeedback(),
                        cvGenService.toRoleScoresJson(agentResult.getFinalReview().getRoleResults()),
                        agentResult.getIterationHistory().size());

                // 保存迭代记录
                for (CvGenerationOrchestrator.IterationSnapshot snapshot : agentResult.getIterationHistory()) {
                    CvGenerationRecord record = new CvGenerationRecord();
                    record.setGeneratedCvId(id);
                    record.setIteration(snapshot.getIteration());
                    record.setRoleScores(cvGenService.toRoleScoresJson(
                            snapshot.getReviewResult().getRoleResults()));
                    record.setOverallScore(snapshot.getReviewResult().getOverallScore());
                    record.setFeedback(snapshot.getReviewResult().getCombinedFeedback());
                    record.setCvSnapshot(cv.getFinalContent());
                    cvGenService.saveIterationRecord(record);
                }

                log.info("异步评分完成: id={}, score={}", id, agentResult.getFinalReview().getOverallScore());
            } catch (Exception e) {
                log.error("异步评分失败: id={}", id, e);
                try {
                    // 评分失败，恢复为草稿状态
                    cv.setStatus(GeneratedCv.STATUS_DRAFT);
                    cvGenService.saveGeneratedCv(cv);
                } catch (Exception ignored) {
                    log.error("恢复评分状态失败: id={}", id, ignored);
                }
            }
        });

        log.info("已提交异步评分任务: id={}", id);
        ctx.status(202);
        ctx.json(cv);
    }

    /**
     * 处理查询生成结果请求。
     */
    private void handleGetById(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);
        ctx.json(cv);
    }

    /**
     * 处理查询迭代历史请求。
     */
    private void handleGetHistory(Context ctx) {
        Long id = parseId(ctx);
        cvGenService.getGeneratedCv(id);
        List<CvGenerationRecord> history = cvGenService.getIterationHistory(id);
        ctx.json(history);
    }

    /**
     * 处理简历预览请求。
     */
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

    /**
     * 处理手动修改简历内容请求。
     */
    private void handleUpdate(Context ctx) {
        Long id = parseId(ctx);
        CvContentUpdateRequest request = ctx.bodyAsClass(CvContentUpdateRequest.class);
        request.validate();

        GeneratedCv updated = cvGenService.updateContent(id, request.getFinalContent());
        ctx.json(updated);
    }

    /**
     * 处理导出简历下载请求。
     */
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

    /**
     * 处理删除生成简历请求。
     */
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
