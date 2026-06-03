package me.maxt.cv.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ChatModel 包装器，拦截所有 LLM 调用并记录 token 消耗日志。
 *
 * <p>覆盖 {@link #doChat(ChatRequest)} 方法，这是 ChatModel 中所有 chat() 重载的
 * 最终收敛点。使用 ThreadLocal 存储当前操作名称，调用完成后自动清除。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class TokenLoggingChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(TokenLoggingChatModel.class);
    private static final ThreadLocal<String> CURRENT_OPERATION = new ThreadLocal<>();

    private final ChatModel delegate;

    public TokenLoggingChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    /**
     * 设置当前操作名称（ThreadLocal），下次 LLM 调用日志将使用此名称。
     */
    public static void setOperation(String operation) {
        CURRENT_OPERATION.set(operation);
    }

    /**
     * 清除当前操作名称。
     */
    public static void clearOperation() {
        CURRENT_OPERATION.remove();
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        ChatResponse response = delegate.doChat(request);
        logTokenUsage(response);
        return response;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = delegate.chat(request);
        logTokenUsage(response);
        return response;
    }

    private void logTokenUsage(ChatResponse response) {
        TokenUsage usage = response.tokenUsage();
        if (usage != null) {
            String op = CURRENT_OPERATION.get();
            CURRENT_OPERATION.remove();
            String opLabel = (op != null && !op.isEmpty()) ? "(" + op + ")" : "";
            log.info("LLM调用{} token消耗: input={}, output={}, total={}",
                    opLabel, usage.inputTokenCount(), usage.outputTokenCount(), usage.totalTokenCount());
        }
    }
}
