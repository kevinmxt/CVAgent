package me.maxt.cv.common.util;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileImportUtil 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class FileImportUtilTest {

    @Test
    @DisplayName("extractText：解析 TXT 文件")
    void testExtractTxtText() {
        String content = "这是一份测试简历\n姓名：张三\n技能：Java, Spring";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String result = FileImportUtil.extractText(is, "test.txt");

        assertTrue(result.contains("测试简历"));
        assertTrue(result.contains("张三"));
    }

    @Test
    @DisplayName("extractText：不支持的文件扩展名应抛出异常")
    void testUnsupportedExtension() {
        InputStream is = new ByteArrayInputStream("test".getBytes());

        AppException ex = assertThrows(AppException.class,
                () -> FileImportUtil.extractText(is, "test.exe"));
        assertEquals(ErrorCode.FILE_TYPE_NOT_SUPPORTED, ex.getErrorCode());
    }

    @Test
    @DisplayName("extractText：空文件内容应抛出异常")
    void testEmptyFile() {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        AppException ex = assertThrows(AppException.class,
                () -> FileImportUtil.extractText(is, "empty.txt"));
        assertEquals(ErrorCode.FILE_EMPTY, ex.getErrorCode());
    }

    @Test
    @DisplayName("extractText：解析 HTML 文件")
    void testExtractHtmlText() {
        String html = "<html><body><h1>简历</h1><p>张三的简历</p></body></html>";
        InputStream is = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));

        // Tika 会提取 HTML 中的文本内容
        String result = FileImportUtil.extractText(is, "resume.html");
        // HTML 解析后应包含文本内容（Tika 提取纯文本）
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    @DisplayName("detectContentType：检测文件类型")
    void testDetectContentType() {
        assertEquals("text/plain", FileImportUtil.detectContentType("test.txt"));
        assertEquals("text/html", FileImportUtil.detectContentType("test.html"));
        assertEquals("application/pdf", FileImportUtil.detectContentType("test.pdf"));
    }

    @Test
    @DisplayName("detectContentType：支持 docx 扩展名")
    void testSupportsDocxExtension() {
        String result = FileImportUtil.detectContentType("resume.docx");
        // Tika 返回的 MIME 类型可能是多种格式
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
