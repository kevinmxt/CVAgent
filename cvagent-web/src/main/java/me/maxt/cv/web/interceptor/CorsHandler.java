package me.maxt.cv.web.interceptor;

import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * CORS 跨域处理器，允许前端应用跨域访问 API。
 *
 * <p>开发阶段允许所有来源，生产环境应限制为具体的域名。</p>
 *
 * @author maxt
 * @since 1.0
 */
public class CorsHandler implements Handler {

    @Override
    public void handle(Context ctx) {
        ctx.header("Access-Control-Allow-Origin", "*");
        ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // 预检请求直接返回 200
        if (io.javalin.http.HandlerType.OPTIONS.equals(ctx.method())) {
            ctx.status(200);
            return;
        }
        // 继续处理
    }
}
