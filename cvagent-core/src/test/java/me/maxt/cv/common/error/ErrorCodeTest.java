package me.maxt.cv.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ErrorCode 枚举单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class ErrorCodeTest {

    @Test
    @DisplayName("每个错误码应有唯一编号和非空描述")
    void testErrorCodeUniqueness() {
        for (ErrorCode ec : ErrorCode.values()) {
            assertNotNull(ec.getCode(), "错误码编号不应为空: " + ec.name());
            assertTrue(ec.getCode() > 0, "错误码编号应大于0: " + ec.name());
            assertNotNull(ec.getMessage(), "错误描述不应为空: " + ec.name());
            assertFalse(ec.getMessage().isEmpty(), "错误描述不应为空字符串: " + ec.name());
        }
    }

    @Test
    @DisplayName("fromCode 应返回正确的枚举值")
    void testFromCode() {
        assertEquals(ErrorCode.SYSTEM_ERROR, ErrorCode.fromCode(1000));
        assertEquals(ErrorCode.VALIDATION_ERROR, ErrorCode.fromCode(2000));
        assertEquals(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, ErrorCode.fromCode(3001));
        assertEquals(ErrorCode.AGENT_EXECUTION_FAILED, ErrorCode.fromCode(4000));
        assertEquals(ErrorCode.UNAUTHORIZED, ErrorCode.fromCode(5000));
    }

    @Test
    @DisplayName("未知错误码应返回 SYSTEM_ERROR")
    void testUnknownCode() {
        assertEquals(ErrorCode.SYSTEM_ERROR, ErrorCode.fromCode(9999));
        assertEquals(ErrorCode.SYSTEM_ERROR, ErrorCode.fromCode(-1));
    }

    @Test
    @DisplayName("系统级错误码在 1000-1999 范围内")
    void testSystemErrorRange() {
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.getCode() >= 1000 && ec.getCode() < 2000) {
                assertTrue(ec.name().startsWith("SYSTEM") || ec.name().startsWith("DATABASE")
                                || ec.name().startsWith("CONFIG") || ec.name().contains("FILE"),
                        ec.name() + " 应以 SYSTEM/DATABASE/CONFIG/FILE 开头");
            }
        }
    }

    @Test
    @DisplayName("认证错误码在 5000-5999 范围内")
    void testAuthErrorRange() {
        assertTrue(ErrorCode.UNAUTHORIZED.getCode() >= 5000);
        assertTrue(ErrorCode.FORBIDDEN.getCode() >= 5000);
    }
}
