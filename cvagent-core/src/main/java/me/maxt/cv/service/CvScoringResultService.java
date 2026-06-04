package me.maxt.cv.service;

import me.maxt.cv.store.entity.CvGenerationRecord;
import me.maxt.cv.store.entity.CvScoringResult;
import me.maxt.cv.store.repository.CvScoringResultRepository;
import me.maxt.cv.store.repository.GeneratedCvRepository;
import me.maxt.cv.store.repository.JobDescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * CV 评分结果业务服务。
 *
 * @author maxt
 * @since 1.0
 */
public class CvScoringResultService {

    private static final Logger log = LoggerFactory.getLogger(CvScoringResultService.class);

    private final CvScoringResultRepository repository;
    private final GeneratedCvRepository generatedCvRepo;
    private final JobDescriptionRepository jdRepo;

    public CvScoringResultService(CvScoringResultRepository repository,
                                   GeneratedCvRepository generatedCvRepo,
                                   JobDescriptionRepository jdRepo) {
        this.repository = repository;
        this.generatedCvRepo = generatedCvRepo;
        this.jdRepo = jdRepo;
    }

    public CvScoringResult create(Long generatedCvId, Long jdId) {
        CvScoringResult result = new CvScoringResult();
        result.setGeneratedCvId(generatedCvId);
        result.setJdId(jdId);
        result.setStatus(CvScoringResult.STATUS_SCORING);
        result.setIterationCount(0);
        log.info("创建评分结果: generatedCvId={}, jdId={}", generatedCvId, jdId);
        return repository.insert(result);
    }

    public CvScoringResult complete(Long id, Double finalScore, String finalFeedback,
                                     String roleScores, int iterationCount) {
        CvScoringResult result = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分结果不存在: " + id));
        result.setFinalScore(finalScore);
        result.setFinalFeedback(finalFeedback);
        result.setRoleScores(roleScores);
        result.setIterationCount(iterationCount);
        result.setStatus(CvScoringResult.STATUS_COMPLETED);
        log.info("评分完成: id={}, score={}, iterations={}", id, finalScore, iterationCount);
        return repository.update(result);
    }

    public void fail(Long id) {
        CvScoringResult result = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分结果不存在: " + id));
        result.setStatus(CvScoringResult.STATUS_FAILED);
        repository.update(result);
        log.info("评分失败: id={}", id);
    }

    public List<CvScoringResult> listByGeneratedCvId(Long generatedCvId) {
        List<CvScoringResult> list = repository.findByGeneratedCvId(generatedCvId);
        for (CvScoringResult sr : list) {
            jdRepo.findById(sr.getJdId()).ifPresent(jd -> sr.setJdTitle(jd.getTitle()));
        }
        return list;
    }

    public CvScoringResult getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("评分结果不存在: " + id));
    }

    public void saveIterationRecord(Long scoringResultId, int iteration,
                                     String roleScores, double overallScore,
                                     String feedback, String cvSnapshot) {
        CvGenerationRecord record = new CvGenerationRecord();
        record.setScoringResultId(scoringResultId);
        record.setIteration(iteration);
        record.setRoleScores(roleScores);
        record.setOverallScore(overallScore);
        record.setFeedback(feedback);
        record.setCvSnapshot(cvSnapshot);
        generatedCvRepo.insertRecord(record);
    }

    public List<CvGenerationRecord> getIterationHistory(Long scoringResultId) {
        return generatedCvRepo.findRecordsByScoringResultId(scoringResultId);
    }
}
