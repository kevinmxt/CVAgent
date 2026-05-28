package me.maxt.cv.web.auth;

import java.util.Set;

/**
 * 用户身份接口（预留）。
 *
 * <p>表示已认证用户的身份信息，包含用户 ID、用户名和角色集合。</p>
 *
 * @author maxt
 * @since 1.0
 */
public interface UserIdentity {

    /**
     * 获取用户唯一标识。
     *
     * @return 用户 ID
     */
    String getUserId();

    /**
     * 获取用户名。
     *
     * @return 用户名
     */
    String getUsername();

    /**
     * 获取用户拥有的角色集合。
     *
     * @return 角色集合
     */
    Set<String> getRoles();
}
