package me.maxt.cv.web.auth;

import io.javalin.http.Context;

/**
 * 认证提供者接口（预留，当前不实现）。
 *
 * <p>实现类需要通过请求头（如 Authorization）识别用户身份，返回
 * {@link UserIdentity} 或 null（未认证）。</p>
 *
 * @author maxt
 * @since 1.0
 */
public interface AuthProvider {

    /**
     * 从请求上下文中提取用户身份。
     *
     * @param ctx Javalin 请求上下文
     * @return 用户身份，未认证时返回 null
     */
    UserIdentity authenticate(Context ctx);

    /**
     * 检查用户是否有执行指定操作的权限。
     *
     * @param user       用户身份
     * @param permission 权限标识（如 "cv:delete"）
     * @return true 表示有权限
     */
    boolean hasPermission(UserIdentity user, String permission);
}
