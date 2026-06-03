package me.maxt.cv.common.util;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 文件导入工具类，使用 Apache Tika 统一解析多种格式的文件内容。
 *
 * <p>支持的文件格式：txt、docx、html、pdf。
 * Tika 会自动检测文件类型并调用对应的解析器提取文本内容。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class FileImportUtil {

    private static final Logger log = LoggerFactory.getLogger(FileImportUtil.class);
    private static final Tika TIKA = new Tika();

    /** 最大文件大小限制：10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /** 支持的文件扩展名（用于校验） */
    private static final String[] SUPPORTED_EXTENSIONS = {".txt", ".docx", ".doc", ".html", ".htm", ".pdf"};

    private FileImportUtil() {
        /* 工具类不可实例化 */
    }

    /**
     * 从输入流中提取文本内容。
     *
     * <p>使用 Apache Tika 自动检测文件类型并提取文本，支持 txt、docx、html、pdf 等格式。</p>
     *
     * @param inputStream 文件输入流
     * @param fileName    原始文件名（用于日志和类型推断）
     * @return 提取到的文本内容
     * @throws AppException 文件为空、格式不支持或解析失败时抛出
     */
    public static String extractText(InputStream inputStream, String fileName) {
        // 校验文件扩展名
        if (fileName != null && !isSupported(fileName)) {
            log.warn("不支持的文件类型: {}", fileName);
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, fileName);
        }

        try {
            // 读取全部字节到数组，以便多次使用（如果需要）
            byte[] bytes = inputStream.readAllBytes();

            // 文件为空
            if (bytes.length == 0) {
                throw new AppException(ErrorCode.FILE_EMPTY);
            }

            // 文件大小限制
            if (bytes.length > MAX_FILE_SIZE) {
                throw new AppException(ErrorCode.FILE_TOO_LARGE);
            }

            // 对于文本格式文件（txt/html），直接用 UTF-8 读取，避免 Tika 剥离 HTML 标签
            if (isRawText(fileName)) {
                log.info("直接读取文本文件: {}", fileName);
                return new String(bytes, StandardCharsets.UTF_8);
            }

            // 使用 Tika 解析
            log.info("使用 Tika 解析文件: {}, 大小: {} bytes", fileName, bytes.length);
            String content = TIKA.parseToString(new java.io.ByteArrayInputStream(bytes));
            log.info("文件解析完成: {}, 提取字符数: {}", fileName, content.length());
            return content;

        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件读取失败: {}", fileName, e);
            throw new AppException(ErrorCode.FILE_OPERATION_ERROR, e, fileName);
        } catch (Exception e) {
            log.error("文件解析异常: {}", fileName, e);
            throw new AppException(ErrorCode.FILE_OPERATION_ERROR, e, fileName);
        }
    }

    /**
     * 检测文件的内容类型（MIME Type）。
     *
     * @param fileName 文件名
     * @return MIME 类型字符串，如 "text/plain"、"application/pdf"
     */
    public static String detectContentType(String fileName) {
        return TIKA.detect(fileName);
    }

    /**
     * 判断文件扩展名是否受支持。
     *
     * @param fileName 文件名
     * @return true 表示支持
     */
    private static boolean isSupported(String fileName) {
        String lower = fileName.toLowerCase();
        for (String ext : SUPPORTED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为文本格式文件（txt、html），这些文件应直接 UTF-8 读取而非通过 Tika 解析。
     *
     * @param fileName 文件名
     * @return true 表示文本格式
     */
    private static boolean isRawText(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".html") || lower.endsWith(".htm");
    }
}
