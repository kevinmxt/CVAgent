package me.maxt.cv.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;

/**
 * 统一 JSON 错误响应体。
 *
 * <p>所有 API 异常都通过此格式返回给客户端，包含错误码、人类可读消息、
 * 时间戳和请求路径，便于排查问题。</p>
 *
 * @author maxt
 * @since 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** 错误码 */
    private int code;
    /** 人类可读的错误消息 */
    private String message;
    /** 错误发生时间（Unix 毫秒时间戳） */
    private long timestamp;
    /** 请求路径（便于调试） */
    private String path;

    /**
     * 构造空错误响应（用于 JSON 反序列化）。
     */
    public ErrorResponse() {
    }

    /**
     * 使用错误码和请求路径构造错误响应。
     *
     * @param errorCode 错误码枚举
     * @param path      请求路径
     */
    public ErrorResponse(ErrorCode errorCode, String path) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.timestamp = Instant.now().toEpochMilli();
        this.path = path;
    }

    /**
     * 使用自定义消息构造错误响应。
     *
     * @param code    错误码编号
     * @param message 自定义消息
     * @param path    请求路径
     */
    public ErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now().toEpochMilli();
        this.path = path;
    }

    /**
     * 将错误响应序列化为 JSON 字符串。
     *
     * @return JSON 字符串
     * @throws RuntimeException 序列化失败时抛出
     */
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"code\":1000,\"message\":\"序列化错误响应失败\"}";
        }
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
