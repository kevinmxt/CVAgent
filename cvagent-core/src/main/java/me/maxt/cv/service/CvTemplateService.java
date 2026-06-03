package me.maxt.cv.service;

import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.common.util.FileImportUtil;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.repository.CvTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * 简历模板业务服务，提供模板的增删改查功能。
 *
 * <p>预置模板（isPreset=true）不可删除。模板内容为 HTML 格式，
 * 包含 {{placeholder}} 占位符，在生成简历时替换为实际内容。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvTemplateService {

    private static final Logger log = LoggerFactory.getLogger(CvTemplateService.class);

    private final CvTemplateRepository repository;
    private ChatModel chatModel;

    /**
     * 构造简历模板服务。
     *
     * @param repository 模板数据访问对象
     */
    public CvTemplateService(CvTemplateRepository repository) {
        this.repository = repository;
    }

    /**
     * 设置 AI 模型，用于智能生成模板。
     *
     * @param chatModel LLM 对话模型
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 查询所有模板列表（含预置模板）。
     *
     * @return 模板列表
     */
    public List<CvTemplate> listAll() {
        log.info("查询所有简历模板");
        return repository.findAll();
    }

    /**
     * 根据 ID 获取模板详情。
     *
     * @param id 主键 ID
     * @return 模板实体
     * @throws AppException 如果模板不存在
     */
    public CvTemplate getById(Long id) {
        log.info("查询简历模板: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CV_TEMPLATE_NOT_FOUND, id));
    }

    /**
     * 创建新模板。
     *
     * <p>新创建的模板默认为非预置模板，可被删除。</p>
     *
     * @param template 模板实体
     * @return 持久化后的模板
     */
    public CvTemplate create(CvTemplate template) {
        // 新创建的模板不是预置模板
        template.setIsPreset(false);
        log.info("创建简历模板: name={}", template.getName());
        return repository.insert(template);
    }

    /**
     * 更新模板。
     *
     * @param id     主键 ID
     * @param update 更新的实体（仅更新非空字段）
     * @return 更新后的模板
     * @throws AppException 如果模板不存在
     */
    public CvTemplate update(Long id, CvTemplate update) {
        CvTemplate existing = getById(id);

        if (update.getName() != null) existing.setName(update.getName());
        if (update.getDescription() != null) existing.setDescription(update.getDescription());
        if (update.getTemplateContent() != null) existing.setTemplateContent(update.getTemplateContent());

        log.info("更新简历模板: id={}", id);
        return repository.update(existing);
    }

    /**
     * 删除模板。
     *
     * <p>预置模板不可删除，会抛出 {@link ErrorCode#CANNOT_DELETE_PRESET_TEMPLATE} 异常。</p>
     *
     * @param id 主键 ID
     * @throws AppException 如果模板不存在或是预置模板
     */
    public void delete(Long id) {
        CvTemplate existing = getById(id);

        // 预置模板不可删除
        if (Boolean.TRUE.equals(existing.getIsPreset())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_PRESET_TEMPLATE, id);
        }

        repository.deleteById(id);
        log.info("删除简历模板: id={}", id);
    }

    /**
     * 从文件导入简历模板，使用 AI 分析简历结构并自动生成 HTML 模板。
     *
     * @param inputStream 文件输入流
     * @param fileName    原始文件名
     * @return 生成的模板实体
     * @throws AppException 文件解析失败或 AI 生成失败时抛出
     */
    public CvTemplate importFromFile(InputStream inputStream, String fileName) {
        log.info("导入简历模板: fileName={}", fileName);

        String content = FileImportUtil.extractText(inputStream, fileName);

        if (chatModel == null) {
            throw new AppException(ErrorCode.CONFIG_ERROR, "AI 模型未配置，无法自动生成模板");
        }

        String prompt = buildTemplateGenerationPrompt(content);
        String response = chatModel.chat(prompt);
        if (response == null) {
            throw new AppException(ErrorCode.AGENT_EXECUTION_FAILED, "AI 模型返回为空，请检查 API 配置");
        }
        String htmlTemplate = extractHtml(response);

        if (htmlTemplate == null || htmlTemplate.isBlank()) {
            throw new AppException(ErrorCode.CV_GENERATION_FAILED, "AI 未能生成有效的 HTML 模板，请重试");
        }

        String templateName = extractPersonName(fileName) + "_模板";

        CvTemplate template = new CvTemplate();
        template.setName(templateName);
        template.setDescription("从文件「" + fileName + "」自动生成的简历模板");
        template.setTemplateContent(htmlTemplate);
        template.setIsPreset(false);
        template.setFileName(fileName);

        log.info("AI 模板生成完成: name={}, htmlLength={}", templateName, htmlTemplate.length());
        return repository.insert(template);
    }

    private String buildTemplateGenerationPrompt(String content) {
        boolean isHtml = content.contains("<!DOCTYPE") || content.contains("<html");

        if (isHtml) {
            return """
                    你是一个专业的简历 HTML 模板设计师。

                    请根据以下 HTML 简历代码，将其转换为可复用的模板。

                    要求：
                    1. 保留原始 HTML 的完整结构和 CSS 样式（布局、字体、颜色、间距等），只修改内容部分
                    2. 将实际个人信息替换为以下占位符,注意不要重复,多个相同部分合并：
                       - {{person_name}} 替换真实姓名
                       - {{person_email}} 替换邮箱
                       - {{person_phone}} 替换电话
                       - {{summary}} 替换个人简介
                       - {{professional_exp}} 替换工作经历
                       - {{education}} 替换教育背景
                       - {{skills}} 替换技能列表
                    3. 对于其他内容区域（如证书、语言、项目等），保留其原始章节标题或是属性名称，并将属性内容替换为 {{以下划线分隔的属性名占位符}} 占位符,例如:{{sex}},{{salary_expect}}
                    4. 输出必须是完整的 HTML 文档，保持原有的 DOCTYPE、meta 标签和 style 标签
                    5. 不要包含任何解释性文字，只输出 HTML 代码

                    原始 HTML 简历：
                    """ + content;
        }

        return """
                你是一个专业的简历 HTML 模板设计师。

                请根据以下简历文本内容，生成一个精美的 HTML 简历模板。

                要求：
                1. 分析文本内容的结构，识别出以下信息区域：个人信息（姓名、邮箱、电话）、个人简介、工作经历、教育背景、技能列表，以及任何其他区域（如证书、语言、项目、爱好等）
                2. 将实际个人信息替换为以下占位符（保留原始布局和样式）,注意不要重复,多个相同部分合并：
                   - {{person_name}} 替换真实姓名
                   - {{person_email}} 替换邮箱
                   - {{person_phone}} 替换电话
                   - {{summary}} 替换个人简介
                   - {{professional_exp}} 替换工作经历
                   - {{education}} 替换教育背景
                   - {{skills}} 替换技能列表
                3. 对于其他未在上述列表中的内容区域（如证书、语言、项目等），保留其原始章节标题或是属性名称，并将属性内容替换为 {{以下划线分隔的属性名占位符}} 占位符,例如:{{sex}},{{salary_expect}}
                4. 设计专业的 CSS 样式（使用 style 标签内嵌），包含：
                   - 整洁的布局
                   - 合适的字体、间距、颜色
                   - 清晰的章节分隔
                5. 输出必须是完整的、可直接在浏览器中预览的 HTML 文档，包含 <!DOCTYPE html> 声明
                6. 不要包含任何解释性文字，只输出 HTML 代码

                原始简历文本：
                """ + content;
    }

    private String extractHtml(String response) {
        int start = response.indexOf("<!DOCTYPE html");
        if (start < 0) {
            start = response.indexOf("<html");
        }
        if (start >= 0) {
            int end = response.lastIndexOf("</html>");
            if (end > start) {
                return response.substring(start, end + 7);
            }
        }
        // Try markdown code block
        int codeStart = response.indexOf("```html");
        if (codeStart >= 0) {
            codeStart += 7;
            int codeEnd = response.indexOf("```", codeStart);
            if (codeEnd > codeStart) {
                return response.substring(codeStart, codeEnd).trim();
            }
        }
        codeStart = response.indexOf("```");
        if (codeStart >= 0) {
            codeStart += 3;
            int codeEnd = response.indexOf("```", codeStart);
            if (codeEnd > codeStart) {
                return response.substring(codeStart, codeEnd).trim();
            }
        }
        throw new AppException(ErrorCode.CV_GENERATION_FAILED, "AI 未能生成有效的 HTML 模板");
    }

    private String extractPersonName(String fileName) {
        if (fileName == null) return "未命名";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
