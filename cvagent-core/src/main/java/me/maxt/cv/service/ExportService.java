package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.GeneratedCv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 简历导出服务，负责将生成的 HTML 简历导出为可下载的文件。
 *
 * <p>当前支持导出为 HTML 文件格式，后续可扩展为 PDF 等格式。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    /**
     * 将生成的简历导出为 HTML 文件输入流。
     *
     * <p>返回的输入流可直接写入 HTTP 响应体，配合 Content-Disposition
     * 头实现浏览器下载。</p>
     *
     * @param generatedCv 生成简历实体
     * @return HTML 内容的输入流
     * @throws AppException 如果简历内容为空
     */
    public InputStream exportAsHtml(GeneratedCv generatedCv) {
        String content = generatedCv.getFinalContent();
        if (content == null || content.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "简历内容为空，无法导出");
        }

        log.info("导出简历: id={}, contentLength={}", generatedCv.getId(), content.length());

        // 确保 HTML 内容以 UTF-8 编码
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 生成导出文件名。
     *
     * @param generatedCv 生成简历实体
     * @return 文件名（如 "简历_后端工程师_20260528.html"）
     */
    public String generateFileName(GeneratedCv generatedCv) {
        String dateStr = java.time.LocalDate.now().toString().replace("-", "");
        return "简历_" + dateStr + ".html";
    }

    /**
     * 获取 HTML 文件的 Content-Type。
     *
     * @return "text/html; charset=UTF-8"
     */
    public String getContentType() {
        return "text/html; charset=UTF-8";
    }

    /**
     * 标记简历为已导出状态。
     *
     * @param generatedCv 生成简历实体
     */
    public void markAsExported(GeneratedCv generatedCv) {
        generatedCv.setStatus(GeneratedCv.STATUS_EXPORTED);
        log.info("简历已标记为导出状态: id={}", generatedCv.getId());
    }
}
