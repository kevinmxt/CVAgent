package me.maxt.cv.service;

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

    private final WorkExperienceRepository repository;

    /**
     * 构造工作经历服务。
     *
     * @param repository 工作经历数据访问对象
     */
    public WorkExperienceService(WorkExperienceRepository repository) {
        this.repository = repository;
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
     * 解析文本内容并填充到实体的各字段中。
     *
     * <p>简单地将完整内容赋值给 professionalExp 字段作为主要工作经历。
     * 后续可扩展为更精细的 AI 辅助解析。</p>
     *
     * @param entity  工作经历实体
     * @param content 原始文本内容
     */
    private void parseContentToFields(WorkExperience entity, String content) {
        // 简化处理：将原始内容作为专业经历存储
        entity.setProfessionalExp(content);
        // email、skills、education 等字段暂时留空，后续可通过 AI 解析填充
    }
}
