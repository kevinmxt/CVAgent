package me.maxt.cv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.entity.JobDescription;
import me.maxt.cv.store.entity.WorkExperience;
import me.maxt.cv.store.repository.CvTemplateRepository;
import me.maxt.cv.store.repository.GeneratedCvRepository;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.store.repository.WorkExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 简历生成业务服务，负责编排 Agent 生成流程并管理生成结果。
 *
 * <p>核心流程：
 * <ol>
 *   <li>加载关联的工作经历、模板、岗位描述</li>
 *   <li>将工作经历内容填充到 HTML 模板的占位符中，生成初始简历</li>
 *   <li>调用 Agent 模块（cvagent-agent）进行多角色评审和迭代优化</li>
 *   <li>持久化生成结果和迭代记录</li>
 * </ol>
 *
 * <p>注意：此服务依赖 cvagent-agent 模块的 CvGenerationOrchestrator。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvGenerationService {

    private static final Logger log = LoggerFactory.getLogger(CvGenerationService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WorkExperienceRepository workExpRepo;
    private final CvTemplateRepository templateRepo;
    private final JobDescriptionRepository jdRepo;
    private final GeneratedCvRepository generatedCvRepo;

    /**
     * 构造简历生成服务。
     *
     * @param workExpRepo     工作经历数据访问对象
     * @param templateRepo    模板数据访问对象
     * @param jdRepo          岗位描述数据访问对象
     * @param generatedCvRepo 生成简历数据访问对象
     */
    public CvGenerationService(WorkExperienceRepository workExpRepo,
                               CvTemplateRepository templateRepo,
                               JobDescriptionRepository jdRepo,
                               GeneratedCvRepository generatedCvRepo) {
        this.workExpRepo = workExpRepo;
        this.templateRepo = templateRepo;
        this.jdRepo = jdRepo;
        this.generatedCvRepo = generatedCvRepo;
    }

    /**
     * 将工作经历数据填充到 HTML 模板中，生成初始简历。
     *
     * <p>替换模板中的占位符：{{person_name}}、{{person_email}}、
     * {{person_phone}}、{{summary}}、{{professional_exp}}、
     * {{education}}、{{skills}}。</p>
     *
     * @param workExperience 工作经历实体
     * @param template       简历模板实体
     * @return 填充后的 HTML 字符串
     */
    public String fillTemplate(WorkExperience workExperience, CvTemplate template) {
        log.info("填充模板: templateId={}, workExpId={}", template.getId(), workExperience.getId());

        String html = template.getTemplateContent();

        // 替换占位符，null 值替换为空字符串
        html = replacePlaceholder(html, "person_name", workExperience.getPersonName());
        html = replacePlaceholder(html, "person_email", workExperience.getPersonEmail());
        html = replacePlaceholder(html, "person_phone", workExperience.getPersonPhone());
        html = replacePlaceholder(html, "summary", workExperience.getSummary());
        html = replacePlaceholder(html, "professional_exp", workExperience.getProfessionalExp());
        html = replacePlaceholder(html, "education", workExperience.getEducation());
        html = replacePlaceholder(html, "skills", workExperience.getSkills());

        return html;
    }

    /**
     * 加载生成简历所需的上下文数据。
     *
     * @param workExpId  工作经历 ID
     * @param templateId 模板 ID
     * @param jdId       岗位描述 ID
     * @return 包含三个实体的上下文对象
     * @throws AppException 如果任一关联数据不存在
     */
    public GenerationContext loadContext(Long workExpId, Long templateId, Long jdId) {
        WorkExperience workExp = workExpRepo.findById(workExpId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, workExpId));
        CvTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_TEMPLATE_NOT_FOUND, templateId));
        JobDescription jd = jdRepo.findById(jdId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, jdId));

        return new GenerationContext(workExp, template, jd);
    }

    /**
     * 保存生成结果。
     *
     * <p>如果传入的实体已有 ID，则执行更新操作；否则创建新记录。</p>
     *
     * @param generatedCv 生成简历实体
     * @return 持久化后的实体
     */
    public GeneratedCv saveGeneratedCv(GeneratedCv generatedCv) {
        if (generatedCv.getId() != null) {
            return generatedCvRepo.update(generatedCv);
        }
        return generatedCvRepo.insert(generatedCv);
    }

    /**
     * 保存迭代记录。
     *
     * @param record 迭代记录实体
     */
    public void saveIterationRecord(CvGenerationRecord record) {
        generatedCvRepo.insertRecord(record);
    }

    /**
     * 查询生成的简历详情。
     *
     * @param id 主键 ID
     * @return 生成简历实体
     * @throws AppException 如果记录不存在
     */
    public GeneratedCv getGeneratedCv(Long id) {
        return generatedCvRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GENERATED_CV_NOT_FOUND, id));
    }

    /**
     * 查询生成的简历的迭代历史。
     *
     * @param generatedCvId 生成简历 ID
     * @return 迭代记录列表
     */
    public List<CvGenerationRecord> getIterationHistory(Long generatedCvId) {
        return generatedCvRepo.findRecordsByGeneratedCvId(generatedCvId);
    }

    /**
     * 分页查询所有生成的简历。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 生成简历列表
     */
    public List<GeneratedCv> listGeneratedCvs(int page, int size) {
        int offset = (page - 1) * size;
        return generatedCvRepo.findAll(offset, size);
    }

    /**
     * 更新生成简历的手动修改。
     *
     * @param id           主键 ID
     * @param finalContent 修改后的 HTML 内容
     * @return 更新后的实体
     */
    public GeneratedCv updateContent(Long id, String finalContent) {
        GeneratedCv cv = getGeneratedCv(id);
        cv.setFinalContent(finalContent);
        cv.setStatus(GeneratedCv.STATUS_FINAL);
        log.info("手动修改生成简历内容: id={}", id);
        return generatedCvRepo.update(cv);
    }

    /**
     * 标记简历为已导出。
     *
     * @param id 主键 ID
     */
    public void markExported(Long id) {
        GeneratedCv cv = getGeneratedCv(id);
        cv.setStatus(GeneratedCv.STATUS_EXPORTED);
        generatedCvRepo.update(cv);
        log.info("标记简历为已导出: id={}", id);
    }

    /**
     * 删除生成的简历及其迭代记录。
     *
     * @param id 主键 ID
     */
    public void deleteGeneratedCv(Long id) {
        getGeneratedCv(id);  // 确认存在
        generatedCvRepo.deleteById(id);
        log.info("删除生成的简历: id={}", id);
    }

    /**
     * 将评分数据序列化为 JSON 字符串。
     *
     * @param roleScores 各角色评分映射
     * @return JSON 字符串
     */
    public String toRoleScoresJson(Map<String, ?> roleScores) {
        try {
            return MAPPER.writeValueAsString(roleScores);
        } catch (JsonProcessingException e) {
            log.error("评分数据序列化失败", e);
            return "{}";
        }
    }

    /**
     * 替换模板中的占位符。
     *
     * @param html   HTML 模板
     * @param key    占位符键（不含大括号）
     * @param value  替换值，null 时替换为空字符串
     * @return 替换后的 HTML
     */
    private String replacePlaceholder(String html, String key, String value) {
        String placeholder = "{{" + key + "}}";
        String safeValue = (value != null) ? value : "";
        return html.replace(placeholder, safeValue);
    }

    /**
     * 简历生成上下文，封装工作经历、模板和岗位描述。
     */
    public static class GenerationContext {
        private final WorkExperience workExperience;
        private final CvTemplate template;
        private final JobDescription jobDescription;

        public GenerationContext(WorkExperience workExperience, CvTemplate template, JobDescription jobDescription) {
            this.workExperience = workExperience;
            this.template = template;
            this.jobDescription = jobDescription;
        }

        public WorkExperience getWorkExperience() { return workExperience; }
        public CvTemplate getTemplate() { return template; }
        public JobDescription getJobDescription() { return jobDescription; }
    }
}
