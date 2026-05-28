package me.maxt.cv.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppException 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class AppExceptionTest {

    @Test
    @DisplayName("使用 ErrorCode 构造异常应有正确消息")
    void testConstructorWithErrorCode() {
        AppException ex = new AppException(ErrorCode.SYSTEM_ERROR);
        assertEquals(ErrorCode.SYSTEM_ERROR, ex.getErrorCode());
        assertEquals(ErrorCode.SYSTEM_ERROR.getMessage(), ex.getMessage());
    }

    @Test
    @DisplayName("使用 ErrorCode 和参数构造异常应包含错误码信息")
    void testConstructorWithArgs() {
        AppException ex = new AppException(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, 123L);
        assertEquals(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, ex.getErrorCode());
        // 消息模板不含格式化占位符时，参数仅存储在 args 中
        assertEquals("工作经历不存在", ex.getMessage());
        assertArrayEquals(new Object[]{123L}, ex.getArgs());
    }

    @Test
    @DisplayName("使用 ErrorCode 和原始异常构造应保留因果关系")
    void testConstructorWithCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        AppException ex = new AppException(ErrorCode.AGENT_EXECUTION_FAILED, cause);
        assertEquals(ErrorCode.AGENT_EXECUTION_FAILED, ex.getErrorCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("使用 ErrorCode、参数和原始异常构造应保留所有信息")
    void testConstructorWithArgsAndCause() {
        RuntimeException cause = new RuntimeException("原始异常");
        AppException ex = new AppException(ErrorCode.CV_GENERATION_FAILED, cause, "test-id");
        assertEquals(ErrorCode.CV_GENERATION_FAILED, ex.getErrorCode());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("AppException 应为 RuntimeException 子类")
    void testIsRuntimeException() {
        AppException ex = new AppException(ErrorCode.SYSTEM_ERROR);
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    @DisplayName("ErrorResponse 序列化应包含正确字段")
    void testErrorResponseToJson() {
        ErrorResponse response = new ErrorResponse(ErrorCode.CV_TEMPLATE_NOT_FOUND, "/api/v1/cv-templates/999");
        String json = response.toJson();
        assertTrue(json.contains("3002"));
        assertTrue(json.contains("简历模板不存在"));
        assertTrue(json.contains("timestamp"));
    }

    @Test
    @DisplayName("ErrorResponse 自定义消息构造")
    void testErrorResponseCustom() {
        ErrorResponse response = new ErrorResponse(1000, "自定义错误", "/test");
        assertEquals(1000, response.getCode());
        assertEquals("自定义错误", response.getMessage());
        assertEquals("/test", response.getPath());
    }
}
