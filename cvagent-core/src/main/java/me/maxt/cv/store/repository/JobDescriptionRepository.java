package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.JobDescription;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * 岗位描述数据访问对象，提供对 {@code job_description} 表的 CRUD 操作。
 *
 * @author maxt
 * @since 1.0
 */
public class JobDescriptionRepository {

    private static final Logger log = LoggerFactory.getLogger(JobDescriptionRepository.class);
    private static final String TABLE_NAME = "job_description";

    /**
     * 插入一条新的岗位描述。
     *
     * @param entity 岗位描述实体
     * @return 持久化后的实体（包含自增 ID）
     */
    public JobDescription insert(JobDescription entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(TABLE_NAME))
                .columns(
                        field("TITLE"), field("COMPANY"), field("CONTENT"),
                        field("RAW_FILE_NAME"), field("RAW_FILE_TYPE"),
                        field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getTitle(), entity.getCompany(), entity.getContent(),
                        entity.getRawFileName(), entity.getRawFileType(),
                        entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增岗位描述: id={}, title={}", id, entity.getTitle());
        return entity;
    }

    /**
     * 根据 ID 查询岗位描述。
     *
     * @param id 主键 ID
     * @return 包含实体的 Optional
     */
    public Optional<JobDescription> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapRecord);
    }

    /**
     * 分页查询所有岗位描述。
     *
     * @param offset 偏移量
     * @param limit  每页条数
     * @return 岗位描述列表
     */
    public List<JobDescription> findAll(int offset, int limit) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(TABLE_NAME))
                .orderBy(field("UPDATED_AT").desc())
                .limit(limit)
                .offset(offset)
                .fetch();
        return mapRecords(records);
    }

    /**
     * 查询岗位描述总数。
     *
     * @return 总数
     */
    public int count() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        return dsl.fetchCount(table(TABLE_NAME));
    }

    /**
     * 更新岗位描述。
     *
     * @param entity 岗位描述实体
     * @return 更新后的实体
     */
    public JobDescription update(JobDescription entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(TABLE_NAME))
                .set(field("TITLE"), entity.getTitle())
                .set(field("COMPANY"), entity.getCompany())
                .set(field("CONTENT"), entity.getContent())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新岗位描述: id={}", entity.getId());
        return entity;
    }

    /**
     * 根据 ID 删除岗位描述。
     *
     * @param id 主键 ID
     * @return 删除的记录数
     */
    public int deleteById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        int rows = dsl.deleteFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .execute();
        log.info("删除岗位描述: id={}, 影响行数={}", id, rows);
        return rows;
    }

    /**
     * 将 JOOQ Record 映射为实体。
     */
    private JobDescription mapRecord(Record record) {
        JobDescription entity = new JobDescription();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setTitle(record.get(field("TITLE", String.class)));
        entity.setCompany(record.get(field("COMPANY", String.class)));
        entity.setContent(record.get(field("CONTENT", String.class)));
        entity.setRawFileName(record.get(field("RAW_FILE_NAME", String.class)));
        entity.setRawFileType(record.get(field("RAW_FILE_TYPE", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        entity.setUpdatedAt(record.get(field("UPDATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    private List<JobDescription> mapRecords(Result<Record> records) {
        List<JobDescription> result = new ArrayList<>();
        for (Record record : records) {
            result.add(mapRecord(record));
        }
        return result;
    }
}
