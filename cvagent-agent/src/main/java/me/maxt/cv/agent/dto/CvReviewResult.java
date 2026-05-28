package me.maxt.cv.agent.dto;

import dev.langchain4j.model.output.structured.Description;

/**
 * 单个评审角色的评审结果 DTO。
 *
 * <p>包含角色名称、评分（0~1）和详细的反馈意见。
 * 使用 LangChain4j 的 @Description 注解支持结构化输出解析。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvReviewResult {

    /** 角色名称，如 "HR"、"技术专家"、"团队领导" */
    @Description("角色名称")
    private String roleName;

    /** 该角色的评分（0~1），越高表示越匹配 */
    @Description("Score from 0 to 1 how likely this reviewer would invite the candidate")
    private double score;

    /** 详细的反馈意见，包含优点和需要改进的地方 */
    @Description("Feedback on the CV, what is good, what needs improvement, what skills are missing")
    private String feedback;

    /**
     * 无参构造方法（JSON 反序列化需要）。
     */
    public CvReviewResult() {
    }

    /**
     * 带参构造方法。
     *
     * @param roleName 角色名称
     * @param score    评分
     * @param feedback 反馈意见
     */
    public CvReviewResult(String roleName, double score, String feedback) {
        this.roleName = roleName;
        this.score = score;
        this.feedback = feedback;
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    @Override
    public String toString() {
        return "CvReviewResult{role='" + roleName + "', score=" + score + "}";
    }
}
