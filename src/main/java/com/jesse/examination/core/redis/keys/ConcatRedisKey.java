package com.jesse.examination.core.redis.keys;

import static com.jesse.examination.core.redis.keys.ProjectRedisKey.*;
import static java.lang.String.format;

/** 拼合 Redis 键工具类。 */
public class ConcatRedisKey
{
    /**
     * <p>谁的所有问题答对次数哈希表？ </p>
     *
     * <p>
     *     示例：
     *     <code>user:Jesse:ques_correct_times</code>
     * </p>
     *
     */
    public static String
    correctTimesHashKey(String userName)
    {
        return format(
            "%s:%s:%s",
            USER_INFO_ROOT_KEY,
            userName, QUESTION_CORRECT_TIME
        );
    }

    /**
     * <p>谁的验证码？ </p>
     *
     * <p>
     *     示例：
     *     <code>user:Jesse:varify_code</code>
     * </p>
     *
     */
    public static String
    varifyCodeKey(String userName)
    {
        return format(
            "%s:%s:%s",
            USER_INFO_ROOT_KEY,
            userName, VARIFY_CODE
        );
    }

    /** 获取某个用户下所有键的 pattern */
    public static String
    allKeysOfUser(String userName)
    {
        return format(
            "%s:%s:*",
            USER_INFO_ROOT_KEY, userName
        );
    }

    /* 陆续按需添加拼合方法。*/
}
