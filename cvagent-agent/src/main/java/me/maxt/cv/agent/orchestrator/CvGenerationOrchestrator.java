package me.maxt.cv.agent.orchestrator;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.agent.TokenLoggingChatModel;
import me.maxt.cv.agent.dto.CvReviewResult;
import me.maxt.cv.agent.dto.MultiRoleReviewResult;
import me.maxt.cv.agent.reviewer.CvReviewerAgent;
import me.maxt.cv.agent.tailor.CvTailorAgent;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.config.AgentPromptConfig;
import me.maxt.cv.config.AppConfig.ReviewerRoleConfig;
import me.maxt.cv.config.AppConfig.TailorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CV 生成编排器，负责协调多角色 Agent 循环完成简历的生成和优化。
 *
 * <p>核心工作流程：
 * <ol>
 *   <li>加载角色配置，构建各评审 Agent 和优化 Agent</li>
 *   <li>迭代循环：
 *     <ul>
 *       <li>每个角色 Agent 独立评审简历，给出评分和反馈</li>
 *       <li>计算加权综合评分</li>
 *       <li>如果综合评分 >= 通过阈值，退出循环</li>
 *       <li>否则，合并反馈意见，交由优化 Agent 优化简历</li>
 *       <li>记录每次迭代的快照</li>
 *     </ul>
 *   </li>
 *   <li>达到最大迭代次数或评分达标后，返回最终结果</li>
 * </ol>
 *
 * <p>参考实现：{@code _3b_Loop_Agent_Example_States_And_Fail.java}</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CvGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CvGenerationOrchestrator.class);

    private final ChatModel chatModel;
    private final AgentPromptConfig promptConfig;

    /**
     * 构造 CV 生成编排器。
     *
     * @param chatModel    LLM 对话模型
     * @param promptConfig Agent 提示词配置
     */
    public CvGenerationOrchestrator(ChatModel chatModel, AgentPromptConfig promptConfig) {
        this.chatModel = chatModel;
        this.promptConfig = promptConfig;
    }

    /**
     * 执行简历生成流程。
     *
     * <p>使用迭代循环：多角色评审 → 检查评分 → 优化 → 重新评审，直到评分达标或达到最大迭代次数。</p>
     *
     * @param initialCv      初始简历内容（已填充模板占位符的 HTML）
     * @param jobDescription 岗位描述内容
     * @return 生成结果，包含最终简历、综合评分和迭代记录
     * @throws AppException 当 Agent 执行失败时抛出
     */
    public CvGenerationResult generate(String initialCv, String jobDescription) {
        log.info("开始 CV 生成: maxIterations={}, passScore={}",
                promptConfig.getMaxIterations(), promptConfig.getPassScore());

        // 获取角色配置和优化配置
        Map<String, ReviewerRoleConfig> roles = promptConfig.getReviewerRoles();
        TailorConfig tailorConfig = promptConfig.getTailorConfig();

        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.AGENT_EXECUTION_FAILED, "未配置评审角色");
        }

        // 迭代记录列表
        List<IterationSnapshot> iterationHistory = new ArrayList<>();

        // 构建优化 Agent
        CvTailorAgent tailor = AgenticServices.agentBuilder(CvTailorAgent.class)
                .chatModel(chatModel)
                .outputKey("cv")
                .build();

        // 构建循环 Agent
        // 在 exitCondition 中执行多角色评审并检查退出条件
        UntypedAgent loopAgent = AgenticServices
                .loopBuilder().subAgents(tailor)
                .outputKey("cvAndReview")
                .output(scope -> {
                    Map<String, Object> output = new LinkedHashMap<>();
                    output.put("cv", scope.readState("cv"));
                    return output;
                })
                .exitCondition(scope -> {
                    // 执行多角色评审（使用当前 CV 状态）
                    String currentCv = (String) scope.readState("cv");
                    MultiRoleReviewResult multiReview = performMultiRoleReview(
                            currentCv, jobDescription, roles);

                    // 记录本次迭代快照
                    iterationHistory.add(new IterationSnapshot(
                            iterationHistory.size() + 1,
                            multiReview
                    ));

                    // 将合并后的反馈写入 scope，供下一轮 tailor 使用
                    scope.writeState("cvReviewText", multiReview.getCombinedFeedback());

                    log.info("第 {} 轮评审: overallScore={}, passScore={}",
                            iterationHistory.size(), multiReview.getOverallScore(),
                            promptConfig.getPassScore());

                    return multiReview.getOverallScore() >= promptConfig.getPassScore();
                })
                .maxIterations(promptConfig.getMaxIterations())
                .build();

        // 调用循环 Agent
        try {
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("cv", initialCv);
            arguments.put("jobDescription", jobDescription);
            arguments.put("cvReviewText", "");
            arguments.put("tailorSystemPrompt", tailorConfig.getSystemPrompt());
            arguments.put("tailorUserPrompt", tailorConfig.getUserPrompt());
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) loopAgent.invoke(arguments);

            String finalCv = (String) result.get("cv");

            // 获取最后一次迭代的评审结果
            MultiRoleReviewResult finalReview = iterationHistory.isEmpty()
                    ? new MultiRoleReviewResult()
                    : iterationHistory.get(iterationHistory.size() - 1).getReviewResult();

            log.info("CV 生成完成: iterations={}, finalScore={}",
                    iterationHistory.size(), finalReview.getOverallScore());

            return new CvGenerationResult(finalCv, finalReview, iterationHistory);

        } catch (Exception e) {
            log.error("Agent 执行失败", e);
            throw new AppException(ErrorCode.CV_GENERATION_FAILED, e);
        }
    }

    /**
     * 执行多角色评审：依次调用每个角色的 Agent 获取评分和反馈，计算加权综合评分。
     *
     * @param cv             当前简历内容
     * @param jobDescription 岗位描述
     * @param roles          角色配置
     * @return 多角色综合评审结果
     */
    private MultiRoleReviewResult performMultiRoleReview(
            String cv, String jobDescription, Map<String, ReviewerRoleConfig> roles) {

        Map<String, CvReviewResult> roleResults = new LinkedHashMap<>();
        double totalWeightedScore = 0;
        double totalWeight = 0;
        StringBuilder combinedFeedback = new StringBuilder();

        // 为每个角色创建独立的 Reviewer Agent 并调用
        for (Map.Entry<String, ReviewerRoleConfig> entry : roles.entrySet()) {
            String roleKey = entry.getKey();
            ReviewerRoleConfig roleConfig = entry.getValue();

            try {
                CvReviewerAgent roleAgent = AgenticServices.agentBuilder(CvReviewerAgent.class)
                        .chatModel(chatModel)
                        .outputKey("cvReview_" + roleKey)
                        .build();

                // 预解析 systemPrompt/userPrompt 中的嵌套占位符
                // LangChain4j 只做单层模板替换，不会递归解析已替换内容中的 {{...}}
                String resolvedSystemPrompt = roleConfig.getSystemPrompt()
                        .replace("{{jobDescription}}", jobDescription);
                String resolvedUserPrompt = roleConfig.getUserPrompt()
                        .replace("{{cv}}", cv);

                TokenLoggingChatModel.setOperation("多角色评审-" + roleConfig.getName());
                CvReviewResult result = roleAgent.reviewCv(
                        cv,
                        jobDescription,
                        roleConfig.getDescription(),
                        resolvedSystemPrompt,
                        resolvedUserPrompt
                );

                result.setRoleName(roleConfig.getName());
                roleResults.put(roleKey, result);

                // 加权计算
                double weight = roleConfig.getWeight();
                totalWeightedScore += result.getScore() * weight;
                totalWeight += weight;

                // 合并反馈
                combinedFeedback.append("【").append(roleConfig.getName()).append("反馈】\n");
                combinedFeedback.append(result.getFeedback()).append("\n\n");

                log.debug("角色评审完成: role={}, score={}, weight={}", roleConfig.getName(), result.getScore(), weight);

            } catch (Exception e) {
                log.error("角色评审失败: role={}", roleConfig.getName(), e);
                // 评审失败时使用默认低分
                CvReviewResult failedResult = new CvReviewResult(roleConfig.getName(), 0.0, "评审失败: " + e.getMessage());
                roleResults.put(roleKey, failedResult);
                combinedFeedback.append("【").append(roleConfig.getName()).append("反馈】\n评审异常，请重试\n\n");
            }
        }

        // 计算加权综合评分
        double overallScore = totalWeight > 0 ? totalWeightedScore / totalWeight : 0;

        if (totalWeight > 0 && overallScore < 0.01) {
            log.warn("所有评审角色评分均为 0，请检查 AI 模型返回的评分格式是否正常");
        }

        MultiRoleReviewResult multiResult = new MultiRoleReviewResult(
                roleResults, overallScore, combinedFeedback.toString().trim());

        return multiResult;
    }

    /**
     * 执行纯评审（单轮多角色评审，不进行迭代优化）。
     *
     * @param cv             简历内容
     * @param jobDescription 岗位描述
     * @return 多角色综合评审结果
     */
    public MultiRoleReviewResult review(String cv, String jobDescription) {
        Map<String, ReviewerRoleConfig> roles = promptConfig.getReviewerRoles();
        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.AGENT_EXECUTION_FAILED, "未配置评审角色");
        }
        log.info("开始多角色评审: cvLength={}, jdLength={}", cv.length(), jobDescription.length());
        return performMultiRoleReview(cv, jobDescription, roles);
    }

    /**
     * 将合并后的评审反馈注入到优化 Agent 的提示词中并执行优化。
     *
     * @param cv             当前简历
     * @param combinedReview 合并后的评审反馈
     * @param tailorConfig   优化 Agent 配置
     * @return 优化后的简历
     */
    public String performTailoring(String cv, String combinedReview, TailorConfig tailorConfig) {
        try {
            CvTailorAgent tailor = AgenticServices.agentBuilder(CvTailorAgent.class)
                    .chatModel(chatModel)
                    .outputKey("cv")
                    .build();

            // 预解析 tailor prompt 中的嵌套占位符
            String resolvedTailorSystemPrompt = tailorConfig.getSystemPrompt()
                    .replace("{{cv}}", cv);
            String resolvedTailorUserPrompt = tailorConfig.getUserPrompt()
                    .replace("{{cv}}", cv)
                    .replace("{{cvReview}}", combinedReview);

            TokenLoggingChatModel.setOperation("简历优化");
            String tailored = tailor.tailorCv(
                    cv,
                    combinedReview,
                    resolvedTailorSystemPrompt,
                    resolvedTailorUserPrompt
            );
            log.info("简历优化完成: inputLength={}, outputLength={}", cv.length(), tailored.length());
            return tailored;
        } catch (Exception e) {
            log.error("简历优化失败", e);
            throw new AppException(ErrorCode.CV_GENERATION_FAILED, e);
        }
    }

    // ========== 内部类 ==========

    /**
     * CV 生成结果，包含最终简历、评审结果和迭代历史。
     */
    public static class CvGenerationResult {
        private final String finalCv;
        private final MultiRoleReviewResult finalReview;
        private final List<IterationSnapshot> iterationHistory;

        public CvGenerationResult(String finalCv, MultiRoleReviewResult finalReview,
                                   List<IterationSnapshot> iterationHistory) {
            this.finalCv = finalCv;
            this.finalReview = finalReview;
            this.iterationHistory = iterationHistory;
        }

        public String getFinalCv() { return finalCv; }
        public MultiRoleReviewResult getFinalReview() { return finalReview; }
        public List<IterationSnapshot> getIterationHistory() { return iterationHistory; }
    }

    /**
     * 单次迭代快照，记录迭代序号和评审结果。
     */
    public static class IterationSnapshot {
        private final int iteration;
        private final MultiRoleReviewResult reviewResult;

        public IterationSnapshot(int iteration, MultiRoleReviewResult reviewResult) {
            this.iteration = iteration;
            this.reviewResult = reviewResult;
        }

        public int getIteration() { return iteration; }
        public MultiRoleReviewResult getReviewResult() { return reviewResult; }
    }
}
