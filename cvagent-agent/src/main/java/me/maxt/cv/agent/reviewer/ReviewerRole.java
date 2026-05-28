package me.maxt.cv.agent.reviewer;

/**
 * 评审角色抽象接口，定义角色的基本属性和提示词。
 *
 * <p>每个评审角色负责从特定视角（如 HR、技术专家、团队领导）评估简历，
 * 给出独立的评分和反馈意见。通过实现此接口可以扩展新的评审角色。</p>
 *
 * <p>角色的权重决定了其在综合评分中的占比，所有角色的权重之和应为 1.0。</p>
 *
 * @author maxt
 * @since 1.0
 */
public interface ReviewerRole {

    /**
     * 获取角色标识（key），如 "hr"、"techExpert"。
     *
     * @return 角色标识
     */
    String getRoleKey();

    /**
     * 获取角色显示名称，如 "HR"、"技术专家"。
     *
     * @return 角色名称
     */
    String getRoleName();

    /**
     * 获取角色描述，说明该角色的评审角度。
     *
     * @return 角色描述
     */
    String getDescription();

    /**
     * 获取系统提示词（SystemMessage），定义评审规则和标准。
     *
     * <p>提示词中可使用 {{jobDescription}} 占位符，运行时会被替换为实际 JD 内容。</p>
     *
     * @return 系统提示词
     */
    String getSystemPrompt();

    /**
     * 获取用户提示词（UserMessage），包含具体的评审请求。
     *
     * <p>提示词中可使用 {{cv}} 占位符，运行时会被替换为实际简历内容。</p>
     *
     * @return 用户提示词
     */
    String getUserPrompt();

    /**
     * 获取该角色在综合评分中的权重（0~1）。
     *
     * <p>所有活跃角色的权重之和应为 1.0。</p>
     *
     * @return 评分权重
     */
    double getWeight();
}
