package me.maxt.cv.web.interceptor;

import io.javalin.Javalin;
import me.maxt.cv.common.error.AppException;
import me.maxt.cv.common.error.ErrorCode;
import me.maxt.cv.common.error.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局异常处理器，将 Java 异常映射为统一的 JSON 错误响应。
 *
 * <p>注册到 Javalin 后，所有未捕获的异常都会被拦截并转换为
 * {@link ErrorResponse} 格式返回给客户端。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    /**
     * 向 Javalin 实例注册所有异常处理器。
     *
     * @param app Javalin 实例
     */
    public void register(Javalin app) {
        // 业务异常
        app.exception(AppException.class, (e, ctx) -> {
            log.warn("业务异常: code={}, message={}, path={}",
                    e.getErrorCode().getCode(), e.getMessage(), ctx.path());
            ctx.status(400);
            ctx.json(new ErrorResponse(e.getErrorCode(), ctx.path()));
        });

        // 参数校验失败
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            log.warn("参数校验失败: {}, path={}", e.getMessage(), ctx.path());
            ctx.status(400);
            ctx.json(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getCode(),
                    e.getMessage(), ctx.path()));
        });

        // 其他未预期异常
        app.exception(Exception.class, (e, ctx) -> {
            log.error("未预期异常: path={}", ctx.path(), e);
            ctx.status(500);
            ctx.json(new ErrorResponse(ErrorCode.SYSTEM_ERROR, ctx.path()));
        });
    }
}
