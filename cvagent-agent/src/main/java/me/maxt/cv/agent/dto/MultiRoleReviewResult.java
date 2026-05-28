package me.maxt.cv.agent.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 多角色综合评审结果 DTO。
 *
 * <p>包含各角色的独立评审结果、加权综合评分和合并后的反馈意见。
 * 综合评分 = Σ(各角色评分 × 角色权重)。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class MultiRoleReviewResult {

    /** 各角色的评审结果，key 为角色标识 */
    private Map<String, CvReviewResult> roleResults;

    /** 加权综合评分（0~1） */
    private double overallScore;

    /** 合并后的反馈意见（按角色分组） */
    private String combinedFeedback;

    /**
     * 无参构造方法。
     */
    public MultiRoleReviewResult() {
        this.roleResults = new LinkedHashMap<>();
    }

    /**
     * 带参构造方法。
     *
     * @param roleResults      各角色评审结果
     * @param overallScore     加权综合评分
     * @param combinedFeedback 合并后的反馈
     */
    public MultiRoleReviewResult(Map<String, CvReviewResult> roleResults, double overallScore, String combinedFeedback) {
        this.roleResults = roleResults;
        this.overallScore = overallScore;
        this.combinedFeedback = combinedFeedback;
    }

    /**
     * 添加一个角色的评审结果。
     *
     * @param roleKey 角色标识
     * @param result  评审结果
     */
    public void addRoleResult(String roleKey, CvReviewResult result) {
        this.roleResults.put(roleKey, result);
    }

    public Map<String, CvReviewResult> getRoleResults() { return roleResults; }
    public void setRoleResults(Map<String, CvReviewResult> roleResults) { this.roleResults = roleResults; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double overallScore) { this.overallScore = overallScore; }

    public String getCombinedFeedback() { return combinedFeedback; }
    public void setCombinedFeedback(String combinedFeedback) { this.combinedFeedback = combinedFeedback; }

    @Override
    public String toString() {
        return "MultiRoleReviewResult{overallScore=" + overallScore + ", roles=" + roleResults.keySet() + "}";
    }
}
