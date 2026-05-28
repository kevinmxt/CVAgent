package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.common.util.FileImportUtil;
import me.maxt.cv.store.entity.JobDescription;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;

/**
 * 岗位描述业务服务，提供岗位描述的导入、查询、编辑、删除功能。
 *
 * <p>支持从文件（txt、docx、html、pdf）导入岗位描述内容，
 * 也可直接通过 API 手动创建。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class JobDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(JobDescriptionService.class);

    private final JobDescriptionRepository repository;

    /**
     * 构造岗位描述服务。
     *
     * @param repository 岗位描述数据访问对象
     */
    public JobDescriptionService(JobDescriptionRepository repository) {
        this.repository = repository;
    }

    /**
     * 从文件导入岗位描述。
     *
     * <p>使用 Tika 提取文件文本内容，文件名作为职位标题（去除扩展名）。</p>
     *
     * @param inputStream 文件输入流
     * @param fileName    原始文件名
     * @return 导入后的岗位描述实体
     * @throws AppException 文件解析失败时抛出
     */
    public JobDescription importFromFile(InputStream inputStream, String fileName) {
        log.info("导入岗位描述: fileName={}", fileName);

        String content = FileImportUtil.extractText(inputStream, fileName);
        String fileType = FileImportUtil.detectContentType(fileName);

        JobDescription entity = new JobDescription();
        entity.setTitle(extractTitle(fileName));
        entity.setContent(content);
        entity.setRawFileName(fileName);
        entity.setRawFileType(fileType);

        return repository.insert(entity);
    }

    /**
     * 手动创建岗位描述。
     *
     * @param entity 岗位描述实体
     * @return 持久化后的实体
     */
    public JobDescription create(JobDescription entity) {
        log.info("创建岗位描述: title={}", entity.getTitle());
        return repository.insert(entity);
    }

    /**
     * 分页查询岗位描述列表。
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     * @return 岗位描述列表
     */
    public List<JobDescription> list(int page, int size) {
        int offset = (page - 1) * size;
        log.info("查询岗位描述列表: page={}, size={}", page, size);
        return repository.findAll(offset, size);
    }

    /**
     * 查询岗位描述总数。
     *
     * @return 总数
     */
    public int count() {
        return repository.count();
    }

    /**
     * 根据 ID 获取岗位描述详情。
     *
     * @param id 主键 ID
     * @return 岗位描述实体
     * @throws AppException 如果记录不存在
     */
    public JobDescription getById(Long id) {
        log.info("查询岗位描述: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_DESCRIPTION_NOT_FOUND, id));
    }

    /**
     * 编辑岗位描述。
     *
     * @param id     主键 ID
     * @param update 更新的实体
     * @return 更新后的实体
     * @throws AppException 如果记录不存在
     */
    public JobDescription update(Long id, JobDescription update) {
        JobDescription existing = getById(id);

        if (update.getTitle() != null) existing.setTitle(update.getTitle());
        if (update.getCompany() != null) existing.setCompany(update.getCompany());
        if (update.getContent() != null) existing.setContent(update.getContent());

        log.info("更新岗位描述: id={}", id);
        return repository.update(existing);
    }

    /**
     * 删除岗位描述。
     *
     * @param id 主键 ID
     * @throws AppException 如果记录不存在
     */
    public void delete(Long id) {
        getById(id);  // 确认存在
        repository.deleteById(id);
        log.info("删除岗位描述: id={}", id);
    }

    /**
     * 从文件名提取标题（去除扩展名）。
     */
    private String extractTitle(String fileName) {
        if (fileName == null) return "未命名岗位";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
