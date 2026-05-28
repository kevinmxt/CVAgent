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
import me.maxt.cv.web.dto.request.CvContentUpdateRequest;
import me.maxt.cv.web.dto.request.CvGenerateRequest;
import me.maxt.cv.web.dto.response.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

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

    /**
     * 构造 CV 生成路由。
     *
     * @param cvGenService  CV 生成业务服务
     * @param exportService 导出服务
     * @param config        应用配置
     * @param promptConfig  Agent 提示词配置
     */
    public CvGenerationRoutes(CvGenerationService cvGenService, ExportService exportService,
                               AppConfig config, AgentPromptConfig promptConfig) {
        this.cvGenService = cvGenService;
        this.exportService = exportService;
        this.config = config;
        this.promptConfig = promptConfig;
    }

    /**
     * 向 Javalin 实例注册所有端点。
     *
     * @param app Javalin 实例
     */
    public void register(Javalin app) {
        app.post(PREFIX + "/generate", this::handleGenerate);
        app.get(PREFIX + "/{id}", this::handleGetById);
        app.get(PREFIX + "/{id}/history", this::handleGetHistory);
        app.get(PREFIX + "/{id}/preview", this::handlePreview);
        app.put(PREFIX + "/{id}", this::handleUpdate);
        app.post(PREFIX + "/{id}/export", this::handleExport);
        app.delete(PREFIX + "/{id}", this::handleDelete);
    }

    /**
     * 处理简历生成请求。
     *
     * <p>接收 JSON body 指定关联的工作经历、模板和 JD 的 ID。
     * 加载素材 → 填充模板 → 调用 Agent 生成 → 持久化结果。</p>
     */
    private void handleGenerate(Context ctx) {
        CvGenerateRequest request = ctx.bodyAsClass(CvGenerateRequest.class);
        request.validate();

        // 1. 加载关联数据
        CvGenerationService.GenerationContext context = cvGenService.loadContext(
                request.getWorkExpId(), request.getTemplateId(), request.getJdId());

        // 2. 填充模板生成初始 HTML 简历
        String initialCv = cvGenService.fillTemplate(
                context.getWorkExperience(), context.getTemplate());

        // 3. 调用 Agent 生成
        CvGenerationOrchestrator orchestrator = new CvGenerationOrchestrator(
                ChatModelProvider.createChatModel(config), promptConfig);

        CvGenerationOrchestrator.CvGenerationResult agentResult =
                orchestrator.generate(initialCv, context.getJobDescription().getContent());

        // 4. 持久化生成结果
        GeneratedCv generatedCv = new GeneratedCv();
        generatedCv.setWorkExpId(request.getWorkExpId());
        generatedCv.setTemplateId(request.getTemplateId());
        generatedCv.setJdId(request.getJdId());
        generatedCv.setFinalContent(agentResult.getFinalCv());
        generatedCv.setFinalScore(agentResult.getFinalReview().getOverallScore());
        generatedCv.setFinalFeedback(agentResult.getFinalReview().getCombinedFeedback());
        generatedCv.setRoleScores(cvGenService.toRoleScoresJson(
                agentResult.getFinalReview().getRoleResults()));
        generatedCv.setIterationCount(agentResult.getIterationHistory().size());
        generatedCv.setStatus(GeneratedCv.STATUS_DRAFT);

        GeneratedCv saved = cvGenService.saveGeneratedCv(generatedCv);

        // 5. 持久化迭代记录
        for (CvGenerationOrchestrator.IterationSnapshot snapshot : agentResult.getIterationHistory()) {
            CvGenerationRecord record = new CvGenerationRecord();
            record.setGeneratedCvId(saved.getId());
            record.setIteration(snapshot.getIteration());
            record.setRoleScores(cvGenService.toRoleScoresJson(
                    snapshot.getReviewResult().getRoleResults()));
            record.setOverallScore(snapshot.getReviewResult().getOverallScore());
            record.setFeedback(snapshot.getReviewResult().getCombinedFeedback());
            // 这里简化处理，迭代快照的 CV 不做单独存储（因 Agent 中间态不便获取）
            record.setCvSnapshot(saved.getFinalContent());
            cvGenService.saveIterationRecord(record);
        }

        log.info("CV 生成完成: id={}, score={}, iterations={}",
                saved.getId(), saved.getFinalScore(), saved.getIterationCount());

        ctx.status(HttpStatus.CREATED);
        ctx.json(saved);
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
        // 确认生成记录存在
        cvGenService.getGeneratedCv(id);
        List<CvGenerationRecord> history = cvGenService.getIterationHistory(id);
        ctx.json(history);
    }

    /**
     * 处理简历预览请求。
     *
     * <p>返回 text/html 格式，浏览器可直接渲染。</p>
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
     *
     * <p>将 HTML 简历内容作为文件下载，文件名格式为 "简历_日期.html"。</p>
     */
    private void handleExport(Context ctx) {
        Long id = parseId(ctx);
        GeneratedCv cv = cvGenService.getGeneratedCv(id);

        InputStream fileStream = exportService.exportAsHtml(cv);
        String fileName = exportService.generateFileName(cv);

        // 标记为已导出
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
}
