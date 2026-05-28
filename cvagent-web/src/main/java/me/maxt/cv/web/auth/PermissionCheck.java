package me.maxt.cv.web.auth;

/**
 * 权限检查接口（预留，函数式接口）。
 *
 * <p>用于检查用户对特定资源是否有执行特定操作的权限。</p>
 *
 * @author maxt
 * @since 1.0
 */
@FunctionalInterface
public interface PermissionCheck {

    /**
     * 检查用户是否有执行操作的权限。
     *
     * @param user     用户身份
     * @param resource 资源标识（如 "cv"、"template"）
     * @param action   操作类型（如 "read"、"create"、"update"、"delete"）
     * @return true 表示有权限
     */
    boolean check(UserIdentity user, String resource, String action);
}
