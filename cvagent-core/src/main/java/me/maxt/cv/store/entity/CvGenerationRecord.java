package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * CV 生成迭代记录实体，对应数据库表 {@code cv_generation_record}。
 *
 * <p>记录 Agent 每次迭代的完整快照，包括各角色评分、综合评分、
 * 反馈意见和当时的 HTML 简历内容，便于回溯生成过程。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvGenerationRecord {

    /** 主键 ID */
    private Long id;
    /** 关联的生成简历 ID */
    private Long generatedCvId;
    /** 第几次迭代（从 1 开始） */
    private Integer iteration;
    /** 各角色评分的 JSON 快照 */
    private String roleScores;
    /** 本次迭代综合评分 */
    private Double overallScore;
    /** 本次迭代反馈意见 */
    private String feedback;
    /** 本次迭代后的 HTML 简历快照 */
    private String cvSnapshot;
    /** 创建时间 */
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGeneratedCvId() { return generatedCvId; }
    public void setGeneratedCvId(Long generatedCvId) { this.generatedCvId = generatedCvId; }

    public Integer getIteration() { return iteration; }
    public void setIteration(Integer iteration) { this.iteration = iteration; }

    public String getRoleScores() { return roleScores; }
    public void setRoleScores(String roleScores) { this.roleScores = roleScores; }

    public Double getOverallScore() { return overallScore; }
    public void setOverallScore(Double overallScore) { this.overallScore = overallScore; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getCvSnapshot() { return cvSnapshot; }
    public void setCvSnapshot(String cvSnapshot) { this.cvSnapshot = cvSnapshot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
