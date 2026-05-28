package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * 生成的简历实体，对应数据库表 {@code generated_cv}。
 *
 * <p>记录每次 CV 生成的完整结果，包含关联的工作经历、模板、JD，
 * 最终 HTML 简历内容、综合评分、各角色评分明细和状态。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class GeneratedCv {

    /** 状态：草稿 */
    public static final String STATUS_DRAFT = "DRAFT";
    /** 状态：已定稿 */
    public static final String STATUS_FINAL = "FINAL";
    /** 状态：已导出 */
    public static final String STATUS_EXPORTED = "EXPORTED";

    /** 主键 ID */
    private Long id;
    /** 关联的工作经历 ID */
    private Long workExpId;
    /** 关联的模板 ID */
    private Long templateId;
    /** 关联的 JD ID */
    private Long jdId;
    /** 最终生成的 HTML 简历内容 */
    private String finalContent;
    /** 最终综合评分（0~1） */
    private Double finalScore;
    /** 最终反馈意见 */
    private String finalFeedback;
    /** 各角色评分的 JSON 快照 */
    private String roleScores;
    /** Agent 迭代次数 */
    private Integer iterationCount;
    /** 状态：DRAFT / FINAL / EXPORTED */
    private String status;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getWorkExpId() { return workExpId; }
    public void setWorkExpId(Long workExpId) { this.workExpId = workExpId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getJdId() { return jdId; }
    public void setJdId(Long jdId) { this.jdId = jdId; }

    public String getFinalContent() { return finalContent; }
    public void setFinalContent(String finalContent) { this.finalContent = finalContent; }

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
}
