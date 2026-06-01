package me.maxt.cv.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.common.util.FileImportUtil;
import me.maxt.cv.store.entity.WorkExperience;
import me.maxt.cv.store.repository.WorkExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * 工作经历业务服务，提供导入、查询、编辑、删除等功能。
 *
 * <p>导入功能支持 txt、docx、html、pdf 等常见格式，
 * 使用 Apache Tika 自动检测并提取文本内容。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class WorkExperienceService {

    private static final Logger log = LoggerFactory.getLogger(WorkExperienceService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WorkExperienceRepository repository;
    private ChatModel chatModel;

    /**
     * 构造工作经历服务。
     *
     * @param repository 工作经历数据访问对象
     */
    public WorkExperienceService(WorkExperienceRepository repository) {
        this.repository = repository;
    }

    /**
     * 设置 AI 模型，用于智能解析简历字段。
     *
     * @param chatModel LLM 对话模型
     */
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 从文件导入工作经历。
     *
     * <p>解析文件内容后持久化到数据库，并记录原始文件信息用于追溯。</p>
     *
     * @param inputStream 文件输入流
     * @param fileName    原始文件名
     * @return 导入后的工作经历实体
     * @throws AppException 文件解析失败或格式不支持时抛出
     */
    public WorkExperience importFromFile(InputStream inputStream, String fileName) {
        log.info("导入工作经历: fileName={}", fileName);

        // 提取文本内容
        String content = FileImportUtil.extractText(inputStream, fileName);
        String fileType = FileImportUtil.detectContentType(fileName);

        // 创建实体并保存
        WorkExperience entity = new WorkExperience();
        entity.setPersonName(extractPersonName(fileName));
        entity.setRawFileName(fileName);
        entity.setRawFileType(fileType);
        entity.setRawContent(content);

        // 解析内容填充各字段
        parseContentToFields(entity, content);

        return repository.insert(entity);
    }

    /**
     * 分页查询工作经历列表。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 工作经历列表
     */
    public List<WorkExperience> list(int page, int size) {
        int offset = (page - 1) * size;
        log.info("查询工作经历列表: page={}, size={}", page, size);
        return repository.findAll(offset, size);
    }

    /**
     * 查询工作经历总数。
     *
     * @return 总数
     */
    public int count() {
        return repository.count();
    }

    /**
     * 根据 ID 获取工作经历详情。
     *
     * @param id 主键 ID
     * @return 工作经历实体
     * @throws AppException 如果记录不存在
     */
    public WorkExperience getById(Long id) {
        log.info("查询工作经历: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_EXPERIENCE_NOT_FOUND, id));
    }

    /**
     * 编辑工作经历。
     *
     * @param id     主键 ID
     * @param update 更新的实体（仅更新非空字段）
     * @return 更新后的实体
     * @throws AppException 如果记录不存在
     */
    public WorkExperience update(Long id, WorkExperience update) {
        // 确保记录存在
        WorkExperience existing = getById(id);

        // 更新非空字段
        if (update.getPersonName() != null) existing.setPersonName(update.getPersonName());
        if (update.getPersonEmail() != null) existing.setPersonEmail(update.getPersonEmail());
        if (update.getPersonPhone() != null) existing.setPersonPhone(update.getPersonPhone());
        if (update.getSummary() != null) existing.setSummary(update.getSummary());
        if (update.getSkills() != null) existing.setSkills(update.getSkills());
        if (update.getProfessionalExp() != null) existing.setProfessionalExp(update.getProfessionalExp());
        if (update.getEducation() != null) existing.setEducation(update.getEducation());

        log.info("更新工作经历: id={}", id);
        return repository.update(existing);
    }

    /**
     * 删除工作经历。
     *
     * @param id 主键 ID
     * @throws AppException 如果记录不存在
     */
    public void delete(Long id) {
        // 先确认记录存在
        getById(id);
        repository.deleteById(id);
        log.info("删除工作经历: id={}", id);
    }

    /**
     * 从文件名中提取候选人的姓名（去除扩展名）。
     *
     * @param fileName 文件名
     * @return 提取的姓名
     */
    private String extractPersonName(String fileName) {
        if (fileName == null) return "未知";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * 使用 AI 解析文本内容并填充到实体的各字段中。
     *
     * <p>如果未配置 ChatModel 则回退到简单模式（仅填充 professionalExp）。</p>
     *
     * @param entity  工作经历实体
     * @param content 原始文本内容
     */
    private void parseContentToFields(WorkExperience entity, String content) {
        if (chatModel == null) {
            log.warn("未配置 ChatModel，使用简单解析模式");
            entity.setProfessionalExp(content);
            return;
        }

        try {
            String prompt = buildExtractionPrompt(content);
            String response = chatModel.chat(prompt);
            JsonNode json = MAPPER.readTree(extractJson(response));
            applyExtractedFields(entity, json);
            log.info("AI 解析完成: email={}, phone={}",
                    entity.getPersonEmail(), entity.getPersonPhone());
        } catch (Exception e) {
            log.warn("AI 解析失败，回退到简单模式: {}", e.getMessage());
            entity.setProfessionalExp(content);
        }
    }

    private String buildExtractionPrompt(String content) {
        return """
                You are a resume parser. Extract the following fields from the resume text below.
                Return ONLY valid JSON, no explanation.

                {
                  "personEmail": "email address or null",
                  "personPhone": "phone number or null",
                  "summary": "brief self-introduction/summary paragraph or null",
                  "skills": "comma-separated skills list or null",
                  "professionalExp": "full work experience section text",
                  "education": "education background section text or null"
                }

                Resume text:
                """ + content;
    }

    private String extractJson(String response) {
        // 提取 JSON 块（处理 LLM 可能在 JSON 前后加 markdown 代码块标记的情况）
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private void applyExtractedFields(WorkExperience entity, JsonNode json) {
        if (json.has("personEmail") && !json.get("personEmail").isNull()) {
            entity.setPersonEmail(json.get("personEmail").asText());
        }
        if (json.has("personPhone") && !json.get("personPhone").isNull()) {
            entity.setPersonPhone(json.get("personPhone").asText());
        }
        if (json.has("summary") && !json.get("summary").isNull()) {
            entity.setSummary(json.get("summary").asText());
        }
        if (json.has("skills") && !json.get("skills").isNull()) {
            entity.setSkills(json.get("skills").asText());
        }
        if (json.has("professionalExp") && !json.get("professionalExp").isNull()) {
            entity.setProfessionalExp(json.get("professionalExp").asText());
        }
        if (json.has("education") && !json.get("education").isNull()) {
            entity.setEducation(json.get("education").asText());
        }
    }
}
