package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.CvTemplate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * 简历模板数据访问对象，提供对 {@code cv_template} 表的 CRUD 操作。
 *
 * @author maxt
 * @since 1.0
 */
public class CvTemplateRepository {

    private static final Logger log = LoggerFactory.getLogger(CvTemplateRepository.class);
    private static final String TABLE_NAME = "cv_template";

    /**
     * 插入一条新的简历模板。
     *
     * @param entity 模板实体
     * @return 持久化后的实体（包含自增 ID）
     */
    public CvTemplate insert(CvTemplate entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(TABLE_NAME))
                .columns(
                        field("NAME"), field("DESCRIPTION"), field("TEMPLATE_CONTENT"),
                        field("IS_PRESET"), field("FILE_NAME"),
                        field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getName(), entity.getDescription(), entity.getTemplateContent(),
                        entity.getIsPreset() != null && entity.getIsPreset(),
                        entity.getFileName(),
                        entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增简历模板: id={}, name={}, isPreset={}", id, entity.getName(), entity.getIsPreset());
        return entity;
    }

    /**
     * 根据 ID 查询模板。
     *
     * @param id 主键 ID
     * @return 包含实体的 Optional
     */
    public Optional<CvTemplate> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapRecord);
    }

    /**
     * 查询所有模板，预置模板优先。
     *
     * @return 模板列表
     */
    public List<CvTemplate> findAll() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(TABLE_NAME))
                .orderBy(field("IS_PRESET").desc(), field("UPDATED_AT").desc())
                .fetch();
        return mapRecords(records);
    }

    /**
     * 查询所有预置模板。
     *
     * @return 预置模板列表
     */
    public List<CvTemplate> findPresetTemplates() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(TABLE_NAME))
                .where(field("IS_PRESET").eq(true))
                .orderBy(field("UPDATED_AT").desc())
                .fetch();
        return mapRecords(records);
    }

    /**
     * 更新模板。
     *
     * @param entity 模板实体
     * @return 更新后的实体
     */
    public CvTemplate update(CvTemplate entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(TABLE_NAME))
                .set(field("NAME"), entity.getName())
                .set(field("DESCRIPTION"), entity.getDescription())
                .set(field("TEMPLATE_CONTENT"), entity.getTemplateContent())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新简历模板: id={}", entity.getId());
        return entity;
    }

    /**
     * 根据 ID 删除模板。
     *
     * @param id 主键 ID
     * @return 删除的记录数
     */
    public int deleteById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        int rows = dsl.deleteFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .execute();
        log.info("删除简历模板: id={}, 影响行数={}", id, rows);
        return rows;
    }

    /**
     * 将 JOOQ Record 映射为实体。
     */
    private CvTemplate mapRecord(Record record) {
        CvTemplate entity = new CvTemplate();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setName(record.get(field("NAME", String.class)));
        entity.setDescription(record.get(field("DESCRIPTION", String.class)));
        entity.setTemplateContent(record.get(field("TEMPLATE_CONTENT", String.class)));
        entity.setIsPreset(record.get(field("IS_PRESET", Boolean.class)));
        entity.setFileName(record.get(field("FILE_NAME", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        entity.setUpdatedAt(record.get(field("UPDATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    private List<CvTemplate> mapRecords(Result<Record> records) {
        List<CvTemplate> result = new ArrayList<>();
        for (Record record : records) {
            result.add(mapRecord(record));
        }
        return result;
    }
}
