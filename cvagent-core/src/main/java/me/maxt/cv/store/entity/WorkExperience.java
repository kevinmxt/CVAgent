package me.maxt.cv.store.entity;

import java.time.LocalDateTime;

/**
 * 工作经历实体，对应数据库表 {@code work_experience}。
 *
 * <p>存储从简历文档中导入的个人工作经历信息，包括基本信息、技能、
 * 工作履历、教育背景以及原始导入文件信息。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class WorkExperience {

    /** 主键 ID */
    private Long id;
    /** 姓名 */
    private String personName;
    /** 邮箱 */
    private String personEmail;
    /** 电话 */
    private String personPhone;
    /** 个人简介 */
    private String summary;
    /** 技能列表（逗号分隔或自由文本） */
    private String skills;
    /** 工作经历（Markdown 或自由文本格式） */
    private String professionalExp;
    /** 教育背景（Markdown 或自由文本格式） */
    private String education;
    /** 导入时的原始文件名 */
    private String rawFileName;
    /** 原始文件类型（txt / docx / pdf / html） */
    private String rawFileType;
    /** 导入的原始文本内容 */
    private String rawContent;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getPersonEmail() { return personEmail; }
    public void setPersonEmail(String personEmail) { this.personEmail = personEmail; }

    public String getPersonPhone() { return personPhone; }
    public void setPersonPhone(String personPhone) { this.personPhone = personPhone; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getProfessionalExp() { return professionalExp; }
    public void setProfessionalExp(String professionalExp) { this.professionalExp = professionalExp; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public String getRawFileName() { return rawFileName; }
    public void setRawFileName(String rawFileName) { this.rawFileName = rawFileName; }

    public String getRawFileType() { return rawFileType; }
    public void setRawFileType(String rawFileType) { this.rawFileType = rawFileType; }

    public String getRawContent() { return rawContent; }
    public void setRawContent(String rawContent) { this.rawContent = rawContent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
