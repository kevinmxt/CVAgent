package me.maxt.cv.store.repository;

import me.maxt.cv.store.datasource.DataSourceConfig;
import me.maxt.cv.store.entity.CvScoringResult;
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
 * CV 评分结果数据访问对象。
 *
 * @author maxt
 * @since 1.0
 */
public class CvScoringResultRepository {

    private static final Logger log = LoggerFactory.getLogger(CvScoringResultRepository.class);
    private static final String TABLE_NAME = "cv_scoring_result";

    public CvScoringResult insert(CvScoringResult entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        Long id = dsl.insertInto(table(TABLE_NAME))
                .columns(
                        field("GENERATED_CV_ID"), field("JD_ID"),
                        field("FINAL_SCORE"), field("FINAL_FEEDBACK"),
                        field("ROLE_SCORES"), field("ITERATION_COUNT"),
                        field("STATUS"), field("CREATED_AT"), field("UPDATED_AT")
                )
                .values(
                        entity.getGeneratedCvId(), entity.getJdId(),
                        entity.getFinalScore(), entity.getFinalFeedback(),
                        entity.getRoleScores(), entity.getIterationCount(),
                        entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt()
                )
                .returning(field("ID"))
                .fetchOne()
                .getValue(field("ID", Long.class));

        entity.setId(id);
        log.info("新增评分结果: id={}, generatedCvId={}, jdId={}", id, entity.getGeneratedCvId(), entity.getJdId());
        return entity;
    }

    public Optional<CvScoringResult> findById(Long id) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Record record = dsl.selectFrom(table(TABLE_NAME))
                .where(field("ID").eq(id))
                .fetchOne();
        return Optional.ofNullable(record).map(this::map);
    }

    public List<CvScoringResult> findByGeneratedCvId(Long generatedCvId) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        Result<Record> records = dsl.selectFrom(table(TABLE_NAME))
                .where(field("GENERATED_CV_ID").eq(generatedCvId))
                .orderBy(field("CREATED_AT").desc())
                .fetch();
        return mapList(records);
    }

    public CvScoringResult update(CvScoringResult entity) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        entity.setUpdatedAt(LocalDateTime.now());

        dsl.update(table(TABLE_NAME))
                .set(field("FINAL_SCORE"), entity.getFinalScore())
                .set(field("FINAL_FEEDBACK"), entity.getFinalFeedback())
                .set(field("ROLE_SCORES"), entity.getRoleScores())
                .set(field("ITERATION_COUNT"), entity.getIterationCount())
                .set(field("STATUS"), entity.getStatus())
                .set(field("UPDATED_AT"), entity.getUpdatedAt())
                .where(field("ID").eq(entity.getId()))
                .execute();

        log.info("更新评分结果: id={}, status={}", entity.getId(), entity.getStatus());
        return entity;
    }

    public int deleteByGeneratedCvId(Long generatedCvId) {
        DSLContext dsl = DataSourceConfig.getDSLContext();
        // iteration records cascade via FK
        int rows = dsl.deleteFrom(table(TABLE_NAME))
                .where(field("GENERATED_CV_ID").eq(generatedCvId))
                .execute();
        log.info("删除评分结果: generatedCvId={}, rows={}", generatedCvId, rows);
        return rows;
    }

    private CvScoringResult map(Record record) {
        CvScoringResult entity = new CvScoringResult();
        entity.setId(record.get(field("ID", Long.class)));
        entity.setGeneratedCvId(record.get(field("GENERATED_CV_ID", Long.class)));
        entity.setJdId(record.get(field("JD_ID", Long.class)));
        entity.setFinalScore(record.get(field("FINAL_SCORE", Double.class)));
        entity.setFinalFeedback(record.get(field("FINAL_FEEDBACK", String.class)));
        entity.setRoleScores(record.get(field("ROLE_SCORES", String.class)));
        entity.setIterationCount(record.get(field("ITERATION_COUNT", Integer.class)));
        entity.setStatus(record.get(field("STATUS", String.class)));
        entity.setCreatedAt(record.get(field("CREATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        entity.setUpdatedAt(record.get(field("UPDATED_AT", java.sql.Timestamp.class)).toLocalDateTime());
        return entity;
    }

    private List<CvScoringResult> mapList(Result<Record> records) {
        List<CvScoringResult> result = new ArrayList<>();
        for (Record record : records) {
            result.add(map(record));
        }
        return result;
    }
}
