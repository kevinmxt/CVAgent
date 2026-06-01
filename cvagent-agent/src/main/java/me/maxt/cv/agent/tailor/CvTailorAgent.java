package me.maxt.cv.agent.tailor;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * CV 优化 Agent 接口，根据评审反馈对简历进行优化。
 *
 * <p>使用 LangChain4j AI Service 自动生成实现，提示词通过 {@code @V}
 * 参数动态注入，支持从配置文件加载和切换。</p>
 *
 * <p>核心原则：不编造事实，只基于已有信息进行优化；保持 HTML 格式完整。</p>
 *
 * @author maxt
 * @since 1.0
 */
public interface CvTailorAgent {

    /**
     * 根据多角色评审反馈优化简历。
     *
     * @param cv               当前简历内容（HTML 格式）
     * @param cvReview         合并后的多角色评审反馈
     * @param tailorSystemPrompt 优化系统提示词
     * @param tailorUserPrompt   优化用户提示词
     * @return 优化后的简历 HTML 内容
     */
    @Agent("根据多角色评审反馈优化简历，使其更符合岗位要求。不编造事实，只基于已有信息优化。")
    @SystemMessage("{{tailorSystemPrompt}}")
    @UserMessage("{{tailorUserPrompt}}")
    String tailorCv(
            @V("cv") String cv,
            @V("cvReviewText") String cvReviewText,
            @V("tailorSystemPrompt") String tailorSystemPrompt,
            @V("tailorUserPrompt") String tailorUserPrompt
    );
}
