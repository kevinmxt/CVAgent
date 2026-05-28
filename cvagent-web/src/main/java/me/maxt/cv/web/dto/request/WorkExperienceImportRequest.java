package me.maxt.cv.web.dto.request;

/**
 * 工作经历导入请求（通过 multipart/form-data 上传文件，此类仅作文档用途）。
 *
 * <p>实际接收的是文件流，通过 Javalin 的 {@code ctx.uploadedFile()} 获取。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class WorkExperienceImportRequest {
    // 通过 multipart file 上传，无需 JSON body
}
