package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * CV 评分结果实体，对应数据库表 {@code cv_scoring_result}。
 *
 * <p>一个 CV 可以有多条评分结果（每次选择不同 JD 分别评分）。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvScoringResult {

    /** 状态：评分中 */
    public static final String STATUS_SCORING = "SCORING";
    /** 状态：评分完成 */
    public static final String STATUS_COMPLETED = "COMPLETED";
    /** 状态：评分失败 */
    public static final String STATUS_FAILED = "FAILED";

    private Long id;
    private Long generatedCvId;
    private Long jdId;
    private Double finalScore;
    private String finalFeedback;
    private String roleScores;
    private Integer iterationCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // transient display
    private transient String jdTitle;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGeneratedCvId() { return generatedCvId; }
    public void setGeneratedCvId(Long generatedCvId) { this.generatedCvId = generatedCvId; }

    public Long getJdId() { return jdId; }
    public void setJdId(Long jdId) { this.jdId = jdId; }

    public Double getFinalScore() { return finalScore; }
    public void setFinalScore(Double finalScore) { this.finalScore = finalScore; }

    public String getFinalFeedback() { return finalFeedback; }
    public void setFinalFeedback(String finalFeedback) { this.finalFeedback = finalFeedback; }

    public String getRoleScores() { return roleScores; }
    public void setRoleScores(String roleScores) { this.roleScores = roleScores; }

    public Integer getIterationCount() { return iterationCount; }
    public void setIterationCount(Integer iterationCount) { this.iterationCount = iterationCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getJdTitle() { return jdTitle; }
    public void setJdTitle(String jdTitle) { this.jdTitle = jdTitle; }
}
