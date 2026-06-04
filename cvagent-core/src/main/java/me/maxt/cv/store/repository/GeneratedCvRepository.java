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
 * 生成的简历及迭代记录数据访问对象。
 *
 * @author maxt
 * @since 1.0
 */
public class GeneratedCvRepository {

    private static final Logger log = LoggerFactory.getLogger(GeneratedCvRepository.class);
    private static final String CV_TABLE = "generated_cv";
    private static final String RECORD_TABLE = "cv_generation_record";

    // ========== GeneratedCv ==========

    public GeneratedCv insert(GeneratedCv entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(CV_TABLE))
                .columns(
                        field("WORK_EXP_ID"), field("TEMPLATE_ID"),
                        field("FINAL_CONTENT"), field("STATUS"),
                        field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getWorkExpId(), entity.getTemplateId(),
                        entity.getFinalContent(), entity.getStatus(),
                        entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增生成的简历: id={}", id);
        return entity;
    }

    public Optional<GeneratedCv> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(CV_TABLE))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::mapGeneratedCv);
    }

    public List<GeneratedCv> findAll(int offset, int limit) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(CV_TABLE))
                .orderBy(field("UPDATED_AT").desc())
                .limit(limit)
                .offset(offset)
                .fetch();
        return mapGeneratedCvs(records);
    }

    public int count() {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        return dsl.fetchCount(table(CV_TABLE));
    }

    public GeneratedCv update(GeneratedCv entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(CV_TABLE))
                .set(field("FINAL_CONTENT"), entity.getFinalContent())
                .set(field("STATUS"), entity.getStatus())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新生成的简历: id={}, status={}", entity.getId(), entity.getStatus());
        return entity;
    }

    public int deleteById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        // scoring results + iteration records cascade via FK
        int rows = dsl.deleteFrom(table(CV_TABLE))
                .where(field("ID").eq(id))
                .execute();
        log.info("删除生成的简历: id={}, rows={}", id, rows);
        return rows;
    }

    // ========== CvGenerationRecord (迭代记录，归属于评分结果) ==========

    public CvGenerationRecord insertRecord(CvGenerationRecord record) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        record.setCreatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(RECORD_TABLE))
                .columns(
                        field("SCORING_RESULT_ID"), field("ITERATION"),
                        field("ROLE_SCORES"), field("OVERALL_SCORE"),
                        field("FEEDBACK"), field("CV_SNAPSHOT"),
                        field("CREATED_AT")
                )
                .values(
                        record.getScoringResultId(), record.getIteration(),
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

    public List<CvGenerationRecord> findRecordsByScoringResultId(Long scoringResultId) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(RECORD_TABLE))
                .where(field("SCORING_RESULT_ID").eq(scoringResultId))
                .orderBy(field("ITERATION").asc())
                .fetch();
        return mapGenerationRecords(records);
    }

    // ========== 映射 ==========

    private GeneratedCv mapGeneratedCv(Record record) {
        GeneratedCv entity = new GeneratedCv();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setWorkExpId(record.get(field("WORK_EXP_ID", Long.class)));
        entity.setTemplateId(record.get(field("TEMPLATE_ID", Long.class)));
        entity.setFinalContent(record.get(field("FINAL_CONTENT", String.class)));
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
        entity.setScoringResultId(record.get(field("SCORING_RESULT_ID", Long.class)));
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
