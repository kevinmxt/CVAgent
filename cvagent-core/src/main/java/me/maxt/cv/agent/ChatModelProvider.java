package me.maxt.cv.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * ChatModel 工厂类，根据配置创建对应的 LLM 对话模型实例。
 *
 * <p>支持的 LLM 提供者（通过 {@code llm.provider} 配置切换）：</p>
 * <ul>
 *   <li>{@code openai} — 兼容 OpenAI API 的服务（DeepSeek、通义千问等）</li>
 *   <li>{@code ollama} — 本地 Ollama 服务（预留扩展）</li>
 * </ul>
 *
 * <p>扩展新提供者：在 {@code createChatModel} 中添加新的 case 分支即可。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class ChatModelProvider {

    private static final Logger log = LoggerFactory.getLogger(ChatModelProvider.class);

    private ChatModelProvider() {
        /* 工具类不可实例化 */
    }

    /**
     * 使用默认配置（自动加载 config.json）创建 ChatModel。
     *
     * @return ChatModel 实例
     */
    public static ChatModel createChatModel() {
        AppConfig config = AppConfig.load();
        return createChatModel(config);
    }

    /**
     * 根据指定配置创建 ChatModel。
     *
     * <p>根据 {@link AppConfig#getLlmProvider()} 选择对应的实现：</p>
     * <ul>
     *   <li>{@code openai} — 使用 OpenAI 兼容协议创建模型</li>
     *   <li>其他值 — 抛出 {@link ErrorCode#LLM_PROVIDER_NOT_SUPPORTED} 异常</li>
     * </ul>
     *
     * @param config 应用配置
     * @return ChatModel 实例
     * @throws AppException 当 LLM 提供者不支持时抛出
     */
    public static ChatModel createChatModel(AppConfig config) {
        String provider = config.getLlmProvider();
        log.info("创建 ChatModel: provider={}, model={}, baseUrl={}", provider, config.getModelName(), config.getBaseUrl());

        ChatModel model;
        switch (provider.toLowerCase()) {
            case "openai":
                model = createOpenAiModel(config);
                break;
            case "ollama":
                log.warn("Ollama 提供者尚未完全实现，使用 OpenAI 兼容模式连接");
                model = createOpenAiModel(config);
                break;
            default:
                ChatModel customModel = loadCustomProvider(provider);
                if (customModel != null) {
                    model = customModel;
                    break;
                }
                throw new AppException(ErrorCode.LLM_PROVIDER_NOT_SUPPORTED, provider);
        }
        return new TokenLoggingChatModel(model);
    }

    /**
     * 创建 OpenAI 兼容协议的 ChatModel。
     *
     * <p>使用 {@link OpenAiChatModel} 构建器，配置 baseUrl、apiKey、modelName、
     * temperature、maxTokens、timeout 等参数。</p>
     *
     * @param config 应用配置
     * @return OpenAiChatModel 实例
     */
    private static ChatModel createOpenAiModel(AppConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .build();
    }

    /**
     * 通过 SPI 或其他机制加载自定义 LLM 提供者（预留扩展点）。
     *
     * <p>当前默认返回 null，子类或外部模块可覆盖此方法以注册自定义提供者。</p>
     *
     * @param provider 提供者标识
     * @return ChatModel 实例，找不到则返回 null
     */
    protected static ChatModel loadCustomProvider(String provider) {
        // 预留扩展点：可通过 ServiceLoader 或反射加载自定义实现
        log.debug("尝试加载自定义 LLM 提供者: {}", provider);
        return null;
    }
}
