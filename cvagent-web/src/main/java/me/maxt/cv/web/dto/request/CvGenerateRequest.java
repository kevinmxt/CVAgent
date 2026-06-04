package me.maxt.cv.web.dto.request;

/**
 * CV 生成请求 DTO。
 *
 * <p>指定要关联的工作经历、模板和岗位描述的 ID。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvGenerateRequest {

    /** 工作经历 ID（必填） */
    private Long workExpId;
    /** 简历模板 ID（必填） */
    private Long templateId;
    /** 岗位描述 ID（可选） */
    private Long jdId;

    public Long getWorkExpId() { return workExpId; }
    public void setWorkExpId(Long workExpId) { this.workExpId = workExpId; }

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public Long getJdId() { return jdId; }
    public void setJdId(Long jdId) { this.jdId = jdId; }

    /**
     * 校验必填字段是否完整。
     *
     * @throws IllegalArgumentException 如果任一必填字段为空
     */
    public void validate() {
        if (workExpId == null || templateId == null) {
            throw new IllegalArgumentException("workExpId, templateId 为必填项");
        }
    }
}
