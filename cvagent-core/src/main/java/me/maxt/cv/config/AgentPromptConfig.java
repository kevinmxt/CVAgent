package me.maxt.cv.config;

import me.maxt.cv.config.AppConfig.ReviewerRoleConfig;
import me.maxt.cv.config.AppConfig.TailorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 提示词配置加载器，负责从 AppConfig 中读取各角色的 Prompt 配置。
 *
 * <p>当 config.json 中未配置某角色的提示词时，自动使用硬编码的中文默认值。
 * 支持通过 {@code agent.reviewerRoles} 配置段动态扩展评审角色。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class AgentPromptConfig {

    private static final Logger log = LoggerFactory.getLogger(AgentPromptConfig.class);

    private final AppConfig config;

    /**
     * 使用 AppConfig 构造提示词配置加载器。
     *
     * @param config 应用配置
     */
    public AgentPromptConfig(AppConfig config) {
        this.config = config;
    }

    /**
     * 获取所有评审角色的配置。
     *
     * <p>如果配置文件中未定义任何角色，则返回默认的 HR、技术专家、团队领导三个角色。</p>
     *
     * @return 角色配置映射，key 为角色标识
     */
    public Map<String, ReviewerRoleConfig> getReviewerRoles() {
        Map<String, ReviewerRoleConfig> roles = new LinkedHashMap<>(config.getReviewerRoles());
        if (roles.isEmpty()) {
            log.info("未配置评审角色，使用默认角色（HR、技术专家、团队领导）");
            roles = getDefaultReviewerRoles();
        }
        return Collections.unmodifiableMap(roles);
    }

    /**
     * 获取阶段一优化 Agent 配置（生成时自动调用），未配置时使用默认值。
     */
    public TailorConfig getTailorConfig() {
        TailorConfig tailor = config.getTailorConfig();
        if (tailor.getSystemPrompt() == null || tailor.getSystemPrompt().isEmpty()) {
            tailor.setSystemPrompt(getDefaultTailorSystemPrompt());
        }
        if (tailor.getUserPrompt() == null || tailor.getUserPrompt().isEmpty()) {
            tailor.setUserPrompt(getDefaultTailorUserPrompt());
        }
        return tailor;
    }

    /**
     * 获取阶段二优化 Agent 配置（评分后手动触发），未配置时使用默认值。
     */
    public TailorConfig getPostScoringTailorConfig() {
        TailorConfig tailor = config.getPostScoringTailorConfig();
        if (tailor == null) {
            tailor = new TailorConfig();
        }
        if (tailor.getSystemPrompt() == null || tailor.getSystemPrompt().isEmpty()) {
            tailor.setSystemPrompt(getPostScoringTailorSystemPrompt());
        }
        if (tailor.getUserPrompt() == null || tailor.getUserPrompt().isEmpty()) {
            tailor.setUserPrompt(getPostScoringTailorUserPrompt());
        }
        return tailor;
    }

    /**
     * 获取 Agent 最大迭代次数。
     *
     * @return 最大迭代次数
     */
    public int getMaxIterations() {
        return config.getAgentMaxIterations();
    }

    /**
     * 获取面试通过评分阈值。
     *
     * @return 评分阈值（0~1）
     */
    public double getPassScore() {
        return config.getAgentPassScore();
    }

    // ========== 默认提示词 ==========

    /**
     * 构建默认的三个评审角色配置。
     */
    private Map<String, ReviewerRoleConfig> getDefaultReviewerRoles() {
        Map<String, ReviewerRoleConfig> roles = new LinkedHashMap<>();

        roles.put("hr", buildRole("hr", "HR", "从HR角度评审候选人的软技能和文化匹配度", 0.3,
                "你是一个资深的HR招聘专家。请从以下角度评估候选人与岗位的匹配度：\n" +
                "1) 沟通表达能力\n" +
                "2) 团队协作经验\n" +
                "3) 企业文化匹配度\n" +
                "4) 职业发展潜力\n\n" +
                "请给出0-1的评分和详细的反馈意见。\n\n" +
                "评分标准：\n" +
                "- 0至0.3：不匹配\n" +
                "- 0.3至0.6：部分匹配\n" +
                "- 0.6至0.8：较好匹配\n" +
                "- 0.8至1.0：高度匹配\n\n" +
                "岗位描述：\n{{jobDescription}}",
                "请评审这份简历（从HR视角）：\n\n{{cv}}"));

        roles.put("techExpert", buildRole("techExpert", "技术专家", "从技术专家角度评审候选人的技术能力", 0.4,
                "你是一个资深技术专家。请从技术角度评估候选人：\n" +
                "1) 技术栈匹配度\n" +
                "2) 项目经验深度\n" +
                "3) 架构设计能力\n" +
                "4) 问题解决能力\n\n" +
                "请给出0-1的评分和详细的反馈意见。\n\n" +
                "评分标准：\n" +
                "- 0至0.3：不匹配\n" +
                "- 0.3至0.6：部分匹配\n" +
                "- 0.6至0.8：较好匹配\n" +
                "- 0.8至1.0：高度匹配\n\n" +
                "岗位描述：\n{{jobDescription}}",
                "请评审这份简历（从技术视角）：\n\n{{cv}}"));

        roles.put("teamLeader", buildRole("teamLeader", "团队领导", "从团队领导角度评审候选人的综合能力", 0.3,
                "你是一个团队领导/技术经理。请评估候选人：\n" +
                "1) 独立工作能力\n" +
                "2) 业务理解能力\n" +
                "3) 成长潜力\n" +
                "4) 团队贡献预期\n\n" +
                "请给出0-1的评分和详细的反馈意见。\n\n" +
                "评分标准：\n" +
                "- 0至0.3：不匹配\n" +
                "- 0.3至0.6：部分匹配\n" +
                "- 0.6至0.8：较好匹配\n" +
                "- 0.8至1.0：高度匹配\n\n" +
                "岗位描述：\n{{jobDescription}}",
                "请评审这份简历（从团队领导视角）：\n\n{{cv}}"));

        return roles;
    }

    /**
     * 构建单个角色配置。
     */
    private ReviewerRoleConfig buildRole(String key, String name, String description,
                                          double weight, String systemPrompt, String userPrompt) {
        ReviewerRoleConfig role = new ReviewerRoleConfig();
        role.setRoleKey(key);
        role.setName(name);
        role.setDescription(description);
        role.setWeight(weight);
        role.setSystemPrompt(systemPrompt);
        role.setUserPrompt(userPrompt);
        return role;
    }

    /**
     * 获取默认的阶段一优化 Agent 系统提示词（生成时自动调用，修排版/措辞）。
     */
    private String getDefaultTailorSystemPrompt() {
        return "你是一个专业的简历排版和文字润色专家。请对以下简历进行润色优化。\n\n" +
               "规则：\n" +
               "1. 仅修正排版错误、调整遣词造句，使表达更专业流畅\n" +
               "2. 不编造不存在的工作经历或技能\n" +
               "3. 尽量保持原模板的 HTML 结构和 CSS 样式不变\n" +
               "4. 直接返回完整的 HTML 代码，不要任何解释";
    }

    /**
     * 获取默认的阶段一优化 Agent 用户提示词。
     */
    private String getDefaultTailorUserPrompt() {
        return "原始简历：\n{{cv}}\n\n请润色优化这份简历的排版和文字表达。";
    }

    /**
     * 获取阶段二优化 Agent 系统提示词（评分后手动触发，基于 CV + JD + 评审反馈）。
     */
    public String getPostScoringTailorSystemPrompt() {
        TailorConfig tailor = config.getTailorConfig();
        // 阶段二配置在 config.json 的 tailorPostScoring 段
        String sp = tailor.getSystemPrompt();
        // 复用 stage 1 config 的 system prompt 作为判断：如果 config.json 里没配阶段二，用代码默认值
        // 实际用 postScoringTailorConfig 字段
        return "你是一个专业的简历优化专家。请根据岗位描述和多角色评审反馈对简历进行优化。\n\n" +
               "规则：\n" +
               "1. 基于已有简历内容进行优化，不编造不存在的工作经历或技能\n" +
               "2. HTML 结构和 CSS 样式可适当调整以提升视觉效果\n" +
               "3. 针对反馈中提到的问题进行改进，突出与岗位匹配的经验和技能\n" +
               "4. 用词专业，排版清晰\n" +
               "5. 直接返回完整的 HTML 代码，不要任何解释\n\n" +
               "岗位描述：\n{{jd}}";
    }

    /**
     * 获取阶段二优化 Agent 用户提示词。
     */
    public String getPostScoringTailorUserPrompt() {
        return "原始简历（必须在此基础上优化，不能重写）：\n{{cv}}\n\n" +
               "多角色评审反馈：\n{{cvReview}}\n\n" +
               "请根据岗位描述和评审反馈优化这份简历。";
    }
}
