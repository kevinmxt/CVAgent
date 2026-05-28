package me.maxt.cv.web.route;

import io.javalin.Javalin;
import io.javalin.http.Context;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.service.JobDescriptionService;
import me.maxt.cv.store.entity.JobDescription;
import me.maxt.cv.web.dto.response.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 岗位描述 REST 路由，负责映射 HTTP 端点并调用业务服务。
 *
 * <p>所有路由以 {@code /api/v1/job-descriptions} 为前缀。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class JobDescriptionRoutes {

    private static final Logger log = LoggerFactory.getLogger(JobDescriptionRoutes.class);
    private static final String PREFIX = "/api/v1/job-descriptions";

    private final JobDescriptionService service;

    /**
     * 构造岗位描述路由。
     *
     * @param service 岗位描述业务服务
     */
    public JobDescriptionRoutes(JobDescriptionService service) {
        this.service = service;
    }

    /**
     * 向 Javalin 实例注册所有端点。
     *
     * @param app Javalin 实例
     */
    public void register(Javalin app) {
        app.post(PREFIX + "/import", this::handleImport);
        app.get(PREFIX, this::handleList);
        app.get(PREFIX + "/{id}", this::handleGetById);
        app.put(PREFIX + "/{id}", this::handleUpdate);
        app.delete(PREFIX + "/{id}", this::handleDelete);
    }

    /**
     * 处理文件导入请求。
     */
    private void handleImport(Context ctx) {
        var uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "请上传文件，字段名为 file");
        }
        try (var inputStream = uploadedFile.content()) {
            JobDescription result = service.importFromFile(inputStream, uploadedFile.filename());
            ctx.status(201);
            ctx.json(result);
        } catch (java.io.IOException e) {
            log.error("文件读取失败", e);
            throw new AppException(ErrorCode.FILE_OPERATION_ERROR, e);
        } catch (Exception e) {
            log.error("岗位描述导入失败", e);
            throw e;
        }
    }

    /**
     * 处理分页查询请求。
     */
    private void handleList(Context ctx) {
        int page = parseIntParam(ctx, "page", 1);
        int size = parseIntParam(ctx, "size", 10);

        List<JobDescription> items = service.list(page, size);
        int total = service.count();

        ctx.json(new PageResult<>(items, page, size, total));
    }

    /**
     * 处理根据 ID 查询详情请求。
     */
    private void handleGetById(Context ctx) {
        Long id = parseId(ctx);
        JobDescription entity = service.getById(id);
        ctx.json(entity);
    }

    /**
     * 处理编辑请求。
     */
    private void handleUpdate(Context ctx) {
        Long id = parseId(ctx);
        JobDescription update = ctx.bodyAsClass(JobDescription.class);
        JobDescription result = service.update(id, update);
        ctx.json(result);
    }

    /**
     * 处理删除请求。
     */
    private void handleDelete(Context ctx) {
        Long id = parseId(ctx);
        service.delete(id);
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
        String value = ctx.queryParam(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
