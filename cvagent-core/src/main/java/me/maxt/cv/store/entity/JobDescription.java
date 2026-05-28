package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * 岗位描述实体，对应数据库表 {@code job_description}。
 *
 * <p>存储招聘岗位的详细信息，可从文件导入或手动创建。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class JobDescription {

    /** 主键 ID */
    private Long id;
    /** 职位标题 */
    private String title;
    /** 公司名称 */
    private String company;
    /** JD 正文内容 */
    private String content;
    /** 上传时的原始文件名 */
    private String rawFileName;
    /** 原始文件类型（txt / docx / pdf / html） */
    private String rawFileType;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getRawFileName() { return rawFileName; }
    public void setRawFileName(String rawFileName) { this.rawFileName = rawFileName; }

    public String getRawFileType() { return rawFileType; }
    public void setRawFileType(String rawFileType) { this.rawFileType = rawFileType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
