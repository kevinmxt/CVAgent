-- ============================================
-- CVAgent MySQL 数据库初始化脚本
-- ============================================

CREATE TABLE IF NOT EXISTS work_experience (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_name      VARCHAR(100)  NOT NULL COMMENT '姓名',
    person_email     VARCHAR(200)  NULL COMMENT '邮箱',
    person_phone     VARCHAR(50)   NULL COMMENT '电话',
    summary          TEXT          NULL COMMENT '个人简介',
    skills           TEXT          NULL COMMENT '技能列表',
    professional_exp TEXT          NULL COMMENT '工作经历',
    education        TEXT          NULL COMMENT '教育背景',
    other_info       TEXT          NULL COMMENT '其他信息（AI未能归类的内容）',
    raw_file_name    VARCHAR(255)  NULL COMMENT '导入时的原始文件名',
    raw_file_type    VARCHAR(20)   NULL COMMENT '原始文件类型（txt/docx/pdf/html）',
    raw_content      LONGTEXT      NULL COMMENT '导入的原始文本内容',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作经历表';

CREATE TABLE IF NOT EXISTS cv_template (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(200)  NOT NULL COMMENT '模板名称',
    description      VARCHAR(500)  NULL COMMENT '模板描述',
    template_content LONGTEXT      NOT NULL COMMENT 'HTML模板内容（含{{placeholder}}占位符）',
    is_preset        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否为预置模板',
    file_name        VARCHAR(255)  NULL COMMENT '上传时的文件名',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历模板表';

CREATE TABLE IF NOT EXISTS job_description (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL COMMENT '职位标题',
    company          VARCHAR(200)  NULL COMMENT '公司名称',
    content          LONGTEXT      NOT NULL COMMENT 'JD 正文',
    raw_file_name    VARCHAR(255)  NULL COMMENT '上传时的文件名',
    raw_file_type    VARCHAR(20)   NULL COMMENT '文件类型',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位描述表';

CREATE TABLE IF NOT EXISTS generated_cv (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_exp_id      BIGINT        NOT NULL COMMENT '关联工作经历 ID',
    template_id      BIGINT        NOT NULL COMMENT '关联模板 ID',
    jd_id            BIGINT        NOT NULL COMMENT '关联 JD ID',
    final_content    LONGTEXT      NOT NULL COMMENT '最终生成的 HTML 简历内容',
    final_score      DOUBLE        NULL DEFAULT 0 COMMENT '最终综合评分 (0-1)',
    final_feedback   TEXT          NULL COMMENT '最终反馈意见',
    role_scores      JSON          NULL COMMENT '各角色评分快照（JSON格式）',
    iteration_count  INT           NOT NULL DEFAULT 0 COMMENT 'Agent 迭代次数',
    status           VARCHAR(20)   NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/FINAL/EXPORTED',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (work_exp_id) REFERENCES work_experience(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id)  REFERENCES cv_template(id) ON DELETE CASCADE,
    FOREIGN KEY (jd_id)        REFERENCES job_description(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成的简历表';

CREATE TABLE IF NOT EXISTS cv_generation_record (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    generated_cv_id  BIGINT        NOT NULL COMMENT '关联生成的简历 ID',
    iteration        INT           NOT NULL COMMENT '第几次迭代（从 1 开始）',
    role_scores      JSON          NULL COMMENT '各角色评分快照（JSON格式）',
    overall_score    DOUBLE        NOT NULL DEFAULT 0 COMMENT '本次迭代综合评分',
    feedback         TEXT          NULL COMMENT '本次迭代反馈',
    cv_snapshot      LONGTEXT      NOT NULL COMMENT '本次迭代后的 HTML 简历快照',
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (generated_cv_id) REFERENCES generated_cv(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CV 生成迭代记录表';

-- 索引
CREATE INDEX idx_work_exp_name ON work_experience(person_name);
CREATE INDEX idx_cv_template_preset ON cv_template(is_preset);
CREATE INDEX idx_jd_title ON job_description(title);
CREATE INDEX idx_gen_cv_status ON generated_cv(status);
CREATE INDEX idx_gen_record_cv_id ON cv_generation_record(generated_cv_id);

-- 迁移：为已有数据库添加 other_info 列（重复执行会静默跳过）
ALTER TABLE work_experience ADD COLUMN other_info TEXT NULL COMMENT '其他信息（AI未能归类的内容）';
