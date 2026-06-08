package me.maxt.cv.web.route;

import io.javalin.Javalin;
import io.javalin.http.Context;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.service.CvTemplateService;
import me.maxt.cv.store.entity.CvTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 简历模板 REST 路由，负责映射 HTTP 端点并调用业务服务。
 *
 * <p>所有路由以 {@code /api/v1/cv-templates} 为前缀。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvTemplateRoutes {

    private static final Logger log = LoggerFactory.getLogger(CvTemplateRoutes.class);
    private static final String PREFIX = "/api/v1/cv-templates";

    private final CvTemplateService service;

    /**
     * 构造模板路由。
     *
     * @param service 模板业务服务
     */
    public CvTemplateRoutes(CvTemplateService service) {
        this.service = service;
    }

    /**
     * 向 Javalin 实例注册所有端点。
     *
     * @param app Javalin 实例
     */
    public void register(Javalin app) {
        app.get(PREFIX, this::handleList);
        app.get(PREFIX + "/{id}", this::handleGetById);
        app.post(PREFIX, this::handleCreate);
        app.post(PREFIX + "/import", this::handleImport);
        app.put(PREFIX + "/{id}", this::handleUpdate);
        app.post(PREFIX + "/{id}/duplicate", this::handleDuplicate);
        app.delete(PREFIX + "/{id}", this::handleDelete);
    }

    /**
     * 处理列表查询请求（含预置模板）。
     */
    private void handleList(Context ctx) {
        List<CvTemplate> templates = service.listAll();
        ctx.json(templates);
    }

    /**
     * 处理根据 ID 查询详情请求。
     */
    private void handleGetById(Context ctx) {
        Long id = parseId(ctx);
        CvTemplate template = service.getById(id);
        ctx.json(template);
    }

    /**
     * 处理创建模板请求。
     */
    private void handleCreate(Context ctx) {
        CvTemplate template = ctx.bodyAsClass(CvTemplate.class);
        CvTemplate result = service.create(template);
        ctx.status(201);
        ctx.json(result);
    }

    /**
     * 处理更新模板请求。
     */
    private void handleUpdate(Context ctx) {
        Long id = parseId(ctx);
        CvTemplate update = ctx.bodyAsClass(CvTemplate.class);
        CvTemplate result = service.update(id, update);
        ctx.json(result);
    }

    /**
     * 处理复制模板请求。
     */
    private void handleDuplicate(Context ctx) {
        Long id = parseId(ctx);
        CvTemplate result = service.duplicate(id);
        ctx.status(201);
        ctx.json(result);
    }

    /**
     * 处理删除模板请求（预置模板不可删除）。
     */
    private void handleDelete(Context ctx) {
        Long id = parseId(ctx);
        service.delete(id);
        ctx.status(204);
    }

    /**
     * 处理从文件导入模板请求，使用 AI 自动生成 HTML 模板。
     */
    private void handleImport(Context ctx) {
        var uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "请上传文件，字段名为 file");
        }
        try (var inputStream = uploadedFile.content()) {
            CvTemplate result = service.importFromFile(inputStream, uploadedFile.filename());
            ctx.status(201);
            ctx.json(result);
        } catch (java.io.IOException e) {
            log.error("文件读取失败", e);
            throw new AppException(ErrorCode.FILE_OPERATION_ERROR, e);
        }
    }

    private Long parseId(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "无效的 ID 格式");
        }
    }
}
