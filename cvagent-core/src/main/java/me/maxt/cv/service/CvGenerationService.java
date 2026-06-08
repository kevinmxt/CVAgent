package me.maxt.cv.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.agent.TokenLoggingChatModel;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.entity.GeneratedCv;
import me.maxt.cv.store.entity.WorkExperience;
import me.maxt.cv.store.repository.CvTemplateRepository;
import me.maxt.cv.store.repository.GeneratedCvRepository;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import me.maxt.cv.store.repository.WorkExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final WorkExperienceRepository workExpRepo;
    private final CvTemplateRepository templateRepo;
    private final JobDescriptionRepository jdRepo;
    private final GeneratedCvRepository generatedCvRepo;
    private ChatModel chatModel;
    private int maxRawContentLength = 8000;

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
     * 设置 AI 模型，用于智能填充模板中未匹配的占位符。
     *
     * @param chatModel LLM 对话模型
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 设置 AI 智能填充时原始内容的最大字符数，从 maxTokens 配置推导。
     *
     * @param maxRawContentLength 最大字符数
     */
    public void setMaxRawContentLength(int maxRawContentLength) {
        this.maxRawContentLength = maxRawContentLength;
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

        // 替换已知占位符，null 值替换为空字符串
        html = replacePlaceholder(html, "person_name", workExperience.getPersonName());
        html = replacePlaceholder(html, "person_email", workExperience.getPersonEmail());
        html = replacePlaceholder(html, "person_phone", workExperience.getPersonPhone());
        html = replacePlaceholder(html, "summary", workExperience.getSummary());
        html = replacePlaceholder(html, "professional_exp", workExperience.getProfessionalExp());
        html = replacePlaceholder(html, "education", workExperience.getEducation());
        html = replacePlaceholder(html, "skills", workExperience.getSkills());
        html = replacePlaceholder(html, "other_info", workExperience.getOtherInfo());

        // AI 智能填充：检测并处理已知字段未覆盖的占位符
        String rawContent = workExperience.getRawContent();
        if (chatModel != null && rawContent != null && !rawContent.isBlank()
                && hasUnresolvedPlaceholders(html)) {
            html = aiSmartFill(html, rawContent);
        }

        return html;
    }

    /**
     * 加载生成简历所需的上下文数据（工作经历 + 模板）。
     *
     * @param workExpId  工作经历 ID
     * @param templateId 模板 ID
     * @return 上下文对象
     * @throws AppException 如果任一关联数据不存在
     */
    public GenerationContext loadContext(Long workExpId, Long templateId) {
        WorkExperience workExp = workExpRepo.findById(workExpId)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, workExpId));
        CvTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.CV_TEMPLATE_NOT_FOUND, templateId));

        return new GenerationContext(workExp, template);
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
     * 查询生成的简历详情。
     */
    public GeneratedCv getGeneratedCv(Long id) {
        return generatedCvRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.GENERATED_CV_NOT_FOUND, id));
    }

    /**
     * 分页查询所有生成的简历，并填充关联实体的显示名称。
     *
     * @param page 页码
     * @param size 每页条数
     * @return 生成简历列表（含 workExpName / templateName / jdTitle）
     */
    public List<GeneratedCv> listGeneratedCvs(int page, int size) {
        int offset = (page - 1) * size;
        List<GeneratedCv> list = generatedCvRepo.findAll(offset, size);
        for (GeneratedCv cv : list) {
            workExpRepo.findById(cv.getWorkExpId())
                    .ifPresent(we -> cv.setWorkExpName(we.getPersonName()));
            templateRepo.findById(cv.getTemplateId())
                    .ifPresent(t -> cv.setTemplateName(t.getName()));
        }
        return list;
    }

    /**
     * 查询生成简历总数。
     *
     * @return 总数
     */
    public int count() {
        return generatedCvRepo.count();
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
     * 复制生成的简历（浅复制，不复制评分结果）。
     *
     * @param id 主键 ID
     * @return 新创建的生成简历实体
     * @throws AppException 如果记录不存在
     */
    public GeneratedCv duplicate(Long id) {
        GeneratedCv existing = getGeneratedCv(id);
        existing.setId(null);
        existing.setStatus(GeneratedCv.STATUS_DRAFT);
        log.info("复制生成简历: id={}, newWorkExpId={}, newTemplateId={}",
                id, existing.getWorkExpId(), existing.getTemplateId());
        return generatedCvRepo.insert(existing);
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
     * 将角色评分映射序列化为 JSON 字符串。
     *
     * @param roleScores 各角色评分映射（key=角色标识, value=0~1 的评分）
     * @return JSON 字符串，如 {"hr":0.8,"techExpert":0.7}
     */
    public String toRoleScoresJson(Map<String, Double> roleScores) {
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
     * 检测模板中是否还有未替换的占位符。
     */
    private boolean hasUnresolvedPlaceholders(String html) {
        return PLACEHOLDER_PATTERN.matcher(html).find();
    }

    /**
     * 使用 AI 从原始简历内容中提取信息，填充模板中尚未替换的占位符。
     *
     * <p>当模板包含自定义占位符（除 8 个已知字段外）时，此方法调用 LLM
     * 根据 rawContent 智能匹配并填充。避免因固定字段不匹配导致内容丢失。</p>
     *
     * @param html       部分填充后的 HTML（可能含有未替换的占位符）
     * @param rawContent 原始简历全文内容
     * @return 填充完成的 HTML
     */
    private String aiSmartFill(String html, String rawContent) {
        if (chatModel == null) {
            return html;
        }
        try {
            String prompt = """
                    你是一个简历填充助手。以下是：
                    1. 一份包含 {{占位符}} 的 HTML 简历模板（部分占位符已填充）
                    2. 候选人的完整原始简历内容

                    请将模板中所有剩余的 {{占位符}} 替换为原始简历中对应的信息。
                    规则：
                    - 从原始简历中智能匹配每个占位符对应的内容
                    - 如果原始简历中确实没有对应信息，将占位符替换为空字符串
                    - 保持 HTML 结构和 CSS 样式不变
                    - 只输出完整的 HTML 代码，不要包含任何解释

                    模板：
                    """ + html + """

                    原始简历内容：
                    """ + truncateRawContent(rawContent);

            TokenLoggingChatModel.setOperation("模板占位符智能填充");
            String response = chatModel.chat(prompt);
            if (response == null) {
                log.warn("AI 智能填充返回为空，保留未填充的占位符");
                return html;
            }
            return extractHtmlFromResponse(response);
        } catch (Exception e) {
            log.warn("AI 智能填充失败: {}", e.getMessage());
            return html;
        }
    }

    /**
     * 从 AI 响应中提取 HTML 内容。
     */
    private String extractHtmlFromResponse(String response) {
        int start = response.indexOf("<!DOCTYPE");
        if (start < 0) start = response.indexOf("<html");
        if (start >= 0) {
            int end = response.lastIndexOf("</html>");
            if (end > start) return response.substring(start, end + 7);
        }
        int codeStart = response.indexOf("```html");
        if (codeStart >= 0) {
            codeStart += 7;
            int codeEnd = response.indexOf("```", codeStart);
            if (codeEnd > codeStart) return response.substring(codeStart, codeEnd).trim();
        }
        return response;
    }

    /**
     * 截断原始内容避免超出 AI 上下文限制。
     */
    private String truncateRawContent(String rawContent) {
        if (rawContent == null) return "";
        if (rawContent.length() <= maxRawContentLength) return rawContent;
        return rawContent.substring(0, maxRawContentLength) + "\n...(内容已截断)";
    }

    /**
     * 简历生成上下文，封装工作经历和模板。
     */
    public static class GenerationContext {
        private final WorkExperience workExperience;
        private final CvTemplate template;

        public GenerationContext(WorkExperience workExperience, CvTemplate template) {
            this.workExperience = workExperience;
            this.template = template;
        }

        public WorkExperience getWorkExperience() { return workExperience; }
        public CvTemplate getTemplate() { return template; }
    }
}
