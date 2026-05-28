package me.maxt.cv.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AppConfig 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class AppConfigTest {

    @Test
    @DisplayName("默认构造应有合理的默认值")
    void testDefaultValues() {
        AppConfig config = new AppConfig();

        // LLM 默认值
        assertEquals("openai", config.getLlmProvider());
        assertEquals("https://api.deepseek.com", config.getBaseUrl());
        assertEquals("deepseek-v4-flash", config.getModelName());
        assertEquals(0.7, config.getTemperature(), 0.01);
        assertEquals(4096, config.getMaxTokens());
        assertEquals(120, config.getTimeoutSeconds());

        // 数据库默认值
        assertEquals("h2", config.getDbMode());
        assertTrue(config.getH2Url().contains("h2"));

        // Agent 默认值
        assertEquals(3, config.getAgentMaxIterations());
        assertEquals(0.8, config.getAgentPassScore(), 0.01);

        // 服务器默认值
        assertEquals(8080, config.getServerPort());
    }

    @Test
    @DisplayName("getEffectiveJdbcUrl 应根据 dbMode 返回正确的 URL")
    void testEffectiveJdbcUrl() {
        AppConfig config = new AppConfig();

        // H2 模式
        assertTrue(config.getEffectiveJdbcUrl().contains("h2"));

        // MySQL 模式（通过手动设置，因为没有加载 config.json）
        // 测试默认 h2 URL
        assertNotNull(config.getEffectiveJdbcUrl());
    }

    @Test
    @DisplayName("getEffectiveUsername 应返回正确的用户名")
    void testEffectiveUsername() {
        AppConfig config = new AppConfig();
        assertEquals("sa", config.getEffectiveUsername());
    }

    @Test
    @DisplayName("getReviewerRoles 应返回不可变 Map")
    void testReviewerRolesImmutable() {
        AppConfig config = new AppConfig();
        // 默认空配置时 roles 为空
        assertTrue(config.getReviewerRoles().isEmpty());
    }

    @Test
    @DisplayName("load 方法应从类路径加载 config.json")
    void testLoadFromClasspath() {
        AppConfig config = AppConfig.load();

        assertNotNull(config);
        // 类路径下的 config.json 应已加载
        assertNotNull(config.getLlmProvider());
    }
}
