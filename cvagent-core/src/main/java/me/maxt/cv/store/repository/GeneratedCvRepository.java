package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.GeneratedCv;
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
 * 生成的简历数据访问对象，提供对 {@code generated_cv} 和
 * {@code cv_generation_record} 表的 CRUD 操作。
 *
 * @author maxt
 * @since 1.0
 */
public class GeneratedCvRepository {

    private static final Logger log = LoggerFactory.getLogger(GeneratedCvRepository.class);
    private static final String CV_TABLE = "generated_cv";
    private static final String RECORD_TABLE = "cv_generation_record";

    // ========== GeneratedCv 操作 ==========

    /**
     * 插入一条生成简历记录。
     *
     * @param entity 生成简历实体
     * @return 持久化后的实体（包含自增 ID）
     */
    public GeneratedCv insert(GeneratedCv entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(CV_TABLE))
                .columns(
                        field("WORK_EXP_ID"), field("TEMPLATE_ID"), field("JD_ID"),
                        field("FINAL_CONTENT"), field("FINAL_SCORE"), field("FINAL_FEEDBACK"),
                        field("ROLE_SCORES"), field("ITERATION_COUNT"), field("STATUS"),
                        field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getWorkExpId(), entity.getTemplateId(), entity.getJdId(),
                        entity.getFinalContent(), entity.getFinalScore(), entity.getFinalFeedback(),
                        entity.getRoleScores(), entity.getIterationCount(), entity.getStatus(),
                        entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增生成的简历: id={}, score={}, iterations={}", id, entity.getFinalScore(), entity.getIterationCount());
        return entity;
    }

    /**
     * 根据 ID 查询生成的简历。
     *
     * @param id 主键 ID
     * @return 包含实体的 Optional
     */
    public Optional<GeneratedCv> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(CV_TABLE))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapGeneratedCv);
    }

    /**
     * 分页查询所有生成的简历。
     *
     * @param offset 偏移量
     * @param limit  每页条数
     * @return 生成简历列表
     */
    public List<GeneratedCv> findAll(int offset, int limit) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(CV_TABLE))
                .orderBy(field("UPDATED_AT").desc())
                .limit(limit)
                .offset(offset)
                .fetch();
        return mapGeneratedCvs(records);
    }

    /**
     * 查询生成简历总数。
     *
     * @return 总数
     */
    public int count() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        return dsl.fetchCount(table(CV_TABLE));
    }

    /**
     * 更新生成简历的状态和内容。
     *
     * @param entity 生成简历实体
     * @return 更新后的实体
     */
    public GeneratedCv update(GeneratedCv entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(CV_TABLE))
                .set(field("FINAL_CONTENT"), entity.getFinalContent())
                .set(field("FINAL_SCORE"), entity.getFinalScore())
                .set(field("FINAL_FEEDBACK"), entity.getFinalFeedback())
                .set(field("ROLE_SCORES"), entity.getRoleScores())
                .set(field("STATUS"), entity.getStatus())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新生成的简历: id={}, status={}", entity.getId(), entity.getStatus());
        return entity;
    }

    /**
     * 根据 ID 删除生成简历（级联删除迭代记录）。
     *
     * @param id 主键 ID
     * @return 删除的记录数
     */
    public int deleteById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        // 先删除关联的迭代记录
        dsl.deleteFrom(table(RECORD_TABLE))
                .where(field("GENERATED_CV_ID").eq(id))
                .execute();
        // 再删除主记录
        int rows = dsl.deleteFrom(table(CV_TABLE))
                .where(field("ID").eq(id))
                .execute();
        log.info("删除生成的简历: id={}, 影响行数={}", id, rows);
        return rows;
    }

    // ========== CvGenerationRecord 操作 ==========

    /**
     * 插入一条迭代记录。
     *
     * @param record 迭代记录实体
     * @return 持久化后的实体
     */
    public CvGenerationRecord insertRecord(CvGenerationRecord record) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        record.setCreatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(RECORD_TABLE))
                .columns(
                        field("GENERATED_CV_ID"), field("ITERATION"),
                        field("ROLE_SCORES"), field("OVERALL_SCORE"),
                        field("FEEDBACK"), field("CV_SNAPSHOT"),
                        field("CREATED_AT")
                )
                .values(
                        record.getGeneratedCvId(), record.getIteration(),
                        record.getRoleScores(), record.getOverallScore(),
                        record.getFeedback(), record.getCvSnapshot(),
                        record.getCreatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        record.setId(id);
        return record;
    }

    /**
     * 查询某个生成简历的所有迭代记录。
     *
     * @param generatedCvId 生成简历 ID
     * @return 迭代记录列表（按迭代序号升序）
     */
    public List<CvGenerationRecord> findRecordsByGeneratedCvId(Long generatedCvId) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(RECORD_TABLE))
                .where(field("GENERATED_CV_ID").eq(generatedCvId))
                .orderBy(field("ITERATION").asc())
                .fetch();
        return mapGenerationRecords(records);
    }

    // ========== 映射方法 ==========

    private GeneratedCv mapGeneratedCv(Record record) {
        GeneratedCv entity = new GeneratedCv();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setWorkExpId(record.get(field("WORK_EXP_ID", Long.class)));
        entity.setTemplateId(record.get(field("TEMPLATE_ID", Long.class)));
        entity.setJdId(record.get(field("JD_ID", Long.class)));
        entity.setFinalContent(record.get(field("FINAL_CONTENT", String.class)));
        entity.setFinalScore(record.get(field("FINAL_SCORE", Double.class)));
        entity.setFinalFeedback(record.get(field("FINAL_FEEDBACK", String.class)));
        entity.setRoleScores(record.get(field("ROLE_SCORES", String.class)));
        entity.setIterationCount(record.get(field("ITERATION_COUNT", Integer.class)));
        entity.setStatus(record.get(field("STATUS", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        entity.setUpdatedAt(record.get(field("UPDATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    private List<GeneratedCv> mapGeneratedCvs(Result<Record> records) {
        List<GeneratedCv> result = new ArrayList<>();
        for (Record record : records) {
            result.add(mapGeneratedCv(record));
        }
        return result;
    }

    private CvGenerationRecord mapGenerationRecord(Record record) {
        CvGenerationRecord entity = new CvGenerationRecord();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setGeneratedCvId(record.get(field("GENERATED_CV_ID", Long.class)));
        entity.setIteration(record.get(field("ITERATION", Integer.class)));
        entity.setRoleScores(record.get(field("ROLE_SCORES", String.class)));
        entity.setOverallScore(record.get(field("OVERALL_SCORE", Double.class)));
        entity.setFeedback(record.get(field("FEEDBACK", String.class)));
        entity.setCvSnapshot(record.get(field("CV_SNAPSHOT", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    private List<CvGenerationRecord> mapGenerationRecords(Result<Record> records) {
        List<CvGenerationRecord> result = new ArrayList<>();
        for (Record record : records) {
            result.add(mapGenerationRecord(record));
        }
        return result;
    }
}
