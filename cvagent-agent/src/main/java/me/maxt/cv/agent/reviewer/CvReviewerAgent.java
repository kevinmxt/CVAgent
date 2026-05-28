package me.maxt.cv.agent.reviewer;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import me.maxt.cv.agent.dto.CvReviewResult;

/**
 * 通用 CV 评审 Agent 接口，使用 LangChain4j AI Service 自动生成实现。
 *
 * <p>此接口是通用的，通过 {@code @V} 注解注入系统提示词和用户提示词，
 * 实现对不同评审角色的支持。提示词从配置中动态加载，避免硬编码。</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * CvReviewerAgent agent = AgenticServices.agentBuilder(CvReviewerAgent.class)
 *         .chatModel(chatModel)
 *         .outputKey("cvReview")
 *         .build();
 * CvReviewResult result = agent.reviewCv(cv, jd, roleDesc, systemPrompt, userPrompt);
 * }</pre>
 *
 * @author maxt
 * @since 1.0
 */
public interface CvReviewerAgent {

    /**
     * 对简历进行评审。
     *
     * @param cv              简历内容（HTML 格式）
     * @param jobDescription  岗位描述
     * @param roleDescription 角色描述（动态注入到 @Agent 注解）
     * @param systemPrompt    系统提示词
     * @param userPrompt      用户提示词
     * @return 评审结果（评分 + 反馈）
     */
    @Agent("{{roleDescription}}")
    @SystemMessage("{{systemPrompt}}")
    @UserMessage("{{userPrompt}}")
    CvReviewResult reviewCv(
            @V("cv") String cv,
            @V("jobDescription") String jobDescription,
            @V("roleDescription") String roleDescription,
            @V("systemPrompt") String systemPrompt,
            @V("userPrompt") String userPrompt
    );
}
