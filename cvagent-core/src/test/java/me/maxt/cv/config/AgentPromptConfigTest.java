package me.maxt.cv.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AgentPromptConfig 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class AgentPromptConfigTest {

    @Test
    @DisplayName("未配置角色时应返回默认的三个角色")
    void testDefaultRoles() {
        AppConfig config = new AppConfig();
        AgentPromptConfig promptConfig = new AgentPromptConfig(config);

        var roles = promptConfig.getReviewerRoles();

        // 应包含默认的三个角色
        assertEquals(3, roles.size());
        assertTrue(roles.containsKey("hr"));
        assertTrue(roles.containsKey("techExpert"));
        assertTrue(roles.containsKey("teamLeader"));

        // HR 角色权重应为 0.3
        assertEquals(0.3, roles.get("hr").getWeight(), 0.01);
        // 技术专家角色权重应为 0.4
        assertEquals(0.4, roles.get("techExpert").getWeight(), 0.01);
        // 团队领导角色权重应为 0.3
        assertEquals(0.3, roles.get("teamLeader").getWeight(), 0.01);
    }

    @Test
    @DisplayName("默认角色的提示词应包含中文内容")
    void testDefaultPromptsContainChinese() {
        AppConfig config = new AppConfig();
        AgentPromptConfig promptConfig = new AgentPromptConfig(config);

        var roles = promptConfig.getReviewerRoles();
        var hrRole = roles.get("hr");

        assertNotNull(hrRole.getSystemPrompt());
        assertTrue(hrRole.getSystemPrompt().contains("HR"));
        assertTrue(hrRole.getUserPrompt().contains("{{cv}}"));

        var techRole = roles.get("techExpert");
        assertTrue(techRole.getSystemPrompt().contains("技术"));
    }

    @Test
    @DisplayName("getMaxIterations 和 getPassScore 应返回配置值")
    void testAgentSettings() {
        AppConfig config = new AppConfig();
        AgentPromptConfig promptConfig = new AgentPromptConfig(config);

        assertEquals(1, promptConfig.getMaxIterations());
        assertEquals(0.8, promptConfig.getPassScore(), 0.01);
    }

    @Test
    @DisplayName("getTailorConfig 应在未配置时返回默认提示词")
    void testDefaultTailorConfig() {
        AppConfig config = new AppConfig();
        AgentPromptConfig promptConfig = new AgentPromptConfig(config);

        var tailor = promptConfig.getTailorConfig();
        assertNotNull(tailor.getSystemPrompt());
        assertNotNull(tailor.getUserPrompt());
        assertTrue(tailor.getSystemPrompt().contains("简历优化"));
    }
}
