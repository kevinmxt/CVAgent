package me.maxt.cv.service;

import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.store.entity.CvTemplate;
import me.maxt.cv.store.repository.CvTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * 构造简历模板服务。
     *
     * @param repository 模板数据访问对象
     */
    public CvTemplateService(CvTemplateRepository repository) {
        this.repository = repository;
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
}
