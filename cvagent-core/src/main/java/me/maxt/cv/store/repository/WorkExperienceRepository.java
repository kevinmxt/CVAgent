package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.WorkExperience;
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
 * 工作经历数据访问对象，提供对 {@code work_experience} 表的 CRUD 操作。
 *
 * @author maxt
 * @since 1.0
 */
public class WorkExperienceRepository {

    private static final Logger log = LoggerFactory.getLogger(WorkExperienceRepository.class);

    private static final String TABLE_NAME = "work_experience";

    /**
     * 插入一条新的工作经历记录。
     *
     * @param entity 工作经历实体
     * @return 持久化后的实体（包含自增 ID）
     */
    public WorkExperience insert(WorkExperience entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(TABLE_NAME))
                .columns(
                        field("PERSON_NAME"), field("PERSON_EMAIL"), field("PERSON_PHONE"),
                        field("SUMMARY"), field("SKILLS"), field("PROFESSIONAL_EXP"),
                        field("EDUCATION"), field("OTHER_INFO"), field("RAW_FILE_NAME"), field("RAW_FILE_TYPE"),
                        field("RAW_CONTENT"), field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getPersonName(), entity.getPersonEmail(), entity.getPersonPhone(),
                        entity.getSummary(), entity.getSkills(), entity.getProfessionalExp(),
                        entity.getEducation(), entity.getOtherInfo(), entity.getRawFileName(), entity.getRawFileType(),
                        entity.getRawContent(), entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增工作经历: id={}, name={}", id, entity.getPersonName());
        return entity;
    }

    /**
     * 根据 ID 查询工作经历。
     *
     * @param id 主键 ID
     * @return 包含实体的 Optional
     */
    public Optional<WorkExperience> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapRecord);
    }

    /**
     * 分页查询所有工作经历。
     *
     * @param offset 偏移量
     * @param limit  每页条数
     * @return 工作经历列表
     */
    public List<WorkExperience> findAll(int offset, int limit) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(TABLE_NAME))
                .orderBy(field("UPDATED_AT").desc())
                .limit(limit)
                .offset(offset)
                .fetch();
        return mapRecords(records);
    }

    /**
     * 查询工作经历总数。
     *
     * @return 总数
     */
    public int count() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        return dsl.fetchCount(table(TABLE_NAME));
    }

    /**
     * 更新工作经历。
     *
     * @param entity 工作经历实体
     * @return 更新后的实体
     */
    public WorkExperience update(WorkExperience entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(TABLE_NAME))
                .set(field("PERSON_NAME"), entity.getPersonName())
                .set(field("PERSON_EMAIL"), entity.getPersonEmail())
                .set(field("PERSON_PHONE"), entity.getPersonPhone())
                .set(field("SUMMARY"), entity.getSummary())
                .set(field("SKILLS"), entity.getSkills())
                .set(field("PROFESSIONAL_EXP"), entity.getProfessionalExp())
                .set(field("EDUCATION"), entity.getEducation())
                .set(field("OTHER_INFO"), entity.getOtherInfo())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新工作经历: id={}", entity.getId());
        return entity;
    }

    /**
     * 根据 ID 删除工作经历。
     *
     * @param id 主键 ID
     * @return 删除的记录数
     */
    public int deleteById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        int rows = dsl.deleteFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .execute();
        log.info("删除工作经历: id={}, 影响行数={}", id, rows);
        return rows;
    }

    /**
     * 将 JOOQ Record 映射为实体。
     */
    private WorkExperience mapRecord(Record record) {
        WorkExperience entity = new WorkExperience();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setPersonName(record.get(field("PERSON_NAME", String.class)));
        entity.setPersonEmail(record.get(field("PERSON_EMAIL", String.class)));
        entity.setPersonPhone(record.get(field("PERSON_PHONE", String.class)));
        entity.setSummary(record.get(field("SUMMARY", String.class)));
        entity.setSkills(record.get(field("SKILLS", String.class)));
        entity.setProfessionalExp(record.get(field("PROFESSIONAL_EXP", String.class)));
        entity.setEducation(record.get(field("EDUCATION", String.class)));
        entity.setOtherInfo(record.get(field("OTHER_INFO", String.class)));
        entity.setRawFileName(record.get(field("RAW_FILE_NAME", String.class)));
        entity.setRawFileType(record.get(field("RAW_FILE_TYPE", String.class)));
        entity.setRawContent(record.get(field("RAW_CONTENT", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        entity.setUpdatedAt(record.get(field("UPDATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    /**
     * 批量映射 Record 列表为实体列表。
     */
    private List<WorkExperience> mapRecords(Result<Record> records) {
        List<WorkExperience> result = new ArrayList<>();
        for (Record record : records) {
            result.add(mapRecord(record));
        }
        return result;
    }
}
