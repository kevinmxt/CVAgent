package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.store.entity.GeneratedCv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExportService 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class ExportServiceTest {

    private ExportService service;

    @BeforeEach
    void setUp() {
        service = new ExportService();
    }

    @Test
    @DisplayName("exportAsHtml 应返回有效的 HTML 输入流")
    void testExportAsHtml() throws Exception {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        cv.setFinalContent("<html><body><h1>测试简历</h1></body></html>");

        InputStream is = service.exportAsHtml(cv);
        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(content.contains("测试简历"));
        assertTrue(content.contains("<html>"));
    }

    @Test
    @DisplayName("exportAsHtml 空内容时应抛出异常")
    void testExportAsHtmlEmptyContent() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        cv.setFinalContent("");

        assertThrows(AppException.class, () -> service.exportAsHtml(cv));
    }

    @Test
    @DisplayName("exportAsHtml null 内容时应抛出异常")
    void testExportAsHtmlNullContent() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        cv.setFinalContent(null);

        assertThrows(AppException.class, () -> service.exportAsHtml(cv));
    }

    @Test
    @DisplayName("generateFileName 应生成包含日期的文件名")
    void testGenerateFileName() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);

        String fileName = service.generateFileName(cv);
        assertTrue(fileName.startsWith("简历_"));
        assertTrue(fileName.endsWith(".html"));
    }

    @Test
    @DisplayName("getContentType 应返回 text/html")
    void testGetContentType() {
        assertTrue(service.getContentType().contains("text/html"));
        assertTrue(service.getContentType().contains("UTF-8"));
    }

    @Test
    @DisplayName("markAsExported 应将状态设置为 EXPORTED")
    void testMarkAsExported() {
        GeneratedCv cv = new GeneratedCv();
        cv.setId(1L);
        service.markAsExported(cv);
        assertEquals("EXPORTED", cv.getStatus());
    }
}
