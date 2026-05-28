package me.maxt.cv.common.error;

/**
 * 应用全局异常基类，所有业务异常应继承此类或直接使用。
 *
 * <p>携带 {@link ErrorCode} 和可选的格式化参数，用于在全局异常处理器中
 * 生成统一的错误响应。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class AppException extends RuntimeException {

    /** 错误码 */
    private final ErrorCode errorCode;
    /** 用于消息格式化的参数 */
    private final transient Object[] args;

    /**
     * 使用错误码构造异常。
     *
     * @param errorCode 错误码
     */
    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    /**
     * 使用错误码和消息参数构造异常，参数通过 {@link String#format} 格式化到消息中。
     *
     * @param errorCode 错误码
     * @param args      消息格式化参数
     */
    public AppException(ErrorCode errorCode, Object... args) {
        super(formatMessage(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 使用错误码和原始异常构造异常。
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     */
    public AppException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = new Object[0];
    }

    /**
     * 使用错误码、消息参数和原始异常构造异常。
     *
     * @param errorCode 错误码
     * @param cause     原始异常
     * @param args      消息格式化参数
     */
    public AppException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(formatMessage(errorCode.getMessage(), args), cause);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码枚举
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取消息格式化参数。
     *
     * @return 参数数组
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 使用参数格式化消息模板。
     *
     * @param template 消息模板
     * @param args     参数
     * @return 格式化后的消息
     */
    private static String formatMessage(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        try {
            return String.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }
}
