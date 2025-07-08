package com.jesse.examination.core.redis.keys;

import org.jetbrains.annotations.NotNull;

/** 项目中需要用到的所有 Redis 键枚举类。 */
public enum ProjectRedisKey implements CharSequence
{
    /**
     * <p>某普通用户登录状态确认 Redis 键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: LOGIN_STATUS_OF_USER_[USER_NAME]
     *         V: String
     *     </pre>
     * </p>
     */
    USER_LOGIN_STATUS_KEY("LOGIN_STATUS_OF_USER_"),

    /**
     * <p>某管理员登录状态确认 Redis 键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: LOGIN_STATUS_OF_ADMIN_[ADMIN_NAME]
     *         V: String
     *     </pre>
     * </p>
     */
    ADMIN_LOGIN_STATUS_KEY("LOGIN_STATUS_OF_ADMIN_"),

    /**
     * <p>用户所有问题答对次数列表的 Redis 键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: CORRECT_TIMES_LIST_OF_[USER_NAME]
     *         V: List（准确来说是 List{@literal <List>}）
     *     </pre>
     * </p>
     */
    CORRECT_TIMES_LIST_KEY("CORRECT_TIMES_LIST_OF_"),

    /**
     * <p>验证码发起者邮箱 Redis 键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: ENTERPRISE_EMAIL_ADDRESS
     *         V: String
     *     </pre>
     * </p>
     */
    ENTERPRISE_EMAIL_ADDRESS("ENTERPRISE_EMAIL_ADDRESS"),

    /**
     * <p>来自邮箱服务提供的授权码键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: SERVICE_AUTH_CODE
     *         V: String
     *     </pre>
     * </p>
     */
    SERVICE_AUTH_CODE("SERVICE_AUTH_CODE"),

    /**
     * <p>某用户请求的验证码键。</p>
     * <p>
     *     格式为：
     *     <pre>
     *         K: VERIFY_CODE_FOR_[USER_NAME]
     *         V: String
     *     </pre>
     * </p>
     */
    USER_VERIFYCODE_KEY("VERIFY_CODE_FOR_");

    final String keyName;

    ProjectRedisKey(String name) { this.keyName = name; }

    @Override
    public int length() { return this.keyName.length(); }

    @Override
    public char charAt(int index) {
        return this.keyName.charAt(index);
    }

    @Override
    public @NotNull CharSequence
    subSequence(int start, int end) {
        return this.keyName.subSequence(start, end);
    }

    @Override
    public @NotNull String toString() {
        return this.keyName;
    }
}