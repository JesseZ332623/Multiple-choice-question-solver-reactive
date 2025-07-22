package com.jesse.examination.core.redis.keys;

import org.jetbrains.annotations.NotNull;

/** 项目中需要用到的所有 Redis 键枚举类。*/
public enum ProjectRedisKey implements CharSequence
{
    /**
     * <p>用户数据存储根键</p>
     *
     * <p>我所设想的 Redis 用户数据存储树形图因该是这样的：</p>
     * <code><pre>
     * user
     *   |—— Jesse
     *   |      |—— verify_code (String)
     *   |      |—— ques_correct_times (HashMap)
     *   |—— Peter
     *   |      |—— verify_code (String)
     *   |      |—— ques_correct_times (HashMap)
     *   |—— Mike
     *   |      |—— verify_code (String)
     *   |      |—— ques_correct_times (HashMap)
     *   |
     *   ......
     *</pre></code>
     *
     * <p>
     *     相比起以前版本所有数据分开存储，
     *     合理规划 key 的设计令数据关系更清晰。
     * </p>
     */
    USER_INFO_ROOT_KEY("user"),

    /** 用户问题答对次数哈希表子键。 */
    QUESTION_CORRECT_TIME("ques-correct-times"),

    /** 用户验证码子键。 */
    VARIFY_CODE("verify-code"),

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
    SERVICE_AUTH_CODE("SERVICE_AUTH_CODE");

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