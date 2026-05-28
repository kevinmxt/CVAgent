package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * 简历模板实体，对应数据库表 {@code cv_template}。
 *
 * <p>模板内容为 HTML 格式，包含占位符用于后续替换工作经历信息。
 * 预置模板不可删除。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvTemplate {

    /** 主键 ID */
    private Long id;
    /** 模板名称 */
    private String name;
    /** 模板描述 */
    private String description;
    /** HTML 模板内容（含 {{placeholder}} 占位符） */
    private String templateContent;
    /** 是否为预置模板（预置模板不可删除） */
    private Boolean isPreset;
    /** 上传时的文件名（预置模板为空） */
    private String fileName;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }

    public Boolean getIsPreset() { return isPreset; }
    public void setIsPreset(Boolean isPreset) { this.isPreset = isPreset; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
