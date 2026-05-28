package me.maxt.cv.agent;

import dev.langchain4j.model.chat.ChatModel;
import me.maxt.cv.config.AppConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatModelProvider 单元测试。
 *
 * @author maxt
 * @since 1.0
 */
class ChatModelProviderTest {

    @Test
    @DisplayName("createChatModel：默认配置返回非空 ChatModel")
    void testCreateWithDefaultConfig() {
        AppConfig config = new AppConfig();
        // OpenAiChatModel.builder() 在构建时不验证 API key，因此不会抛异常
        ChatModel model = ChatModelProvider.createChatModel(config);
        assertNotNull(model);
    }

    @Test
    @DisplayName("createChatModel：无参方法返回非空 ChatModel")
    void testCreateChatModel() {
        ChatModel model = ChatModelProvider.createChatModel();
        assertNotNull(model);
    }

    @Test
    @DisplayName("ChatModelProvider 构造方法为私有，不可实例化")
    void testPrivateConstructor() throws Exception {
        var constructor = ChatModelProvider.class.getDeclaredConstructor();
        assertFalse(constructor.canAccess(null));
    }

    @Test
    @DisplayName("createChatModel 返回的模型实现了 ChatModel 接口")
    void testModelImplementsChatModel() {
        ChatModel model = ChatModelProvider.createChatModel();
        assertNotNull(model);
        assertTrue(model instanceof ChatModel);
    }
}
