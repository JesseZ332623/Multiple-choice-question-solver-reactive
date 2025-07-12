package com.jesse.examination.question.redis.impl;

import com.jesse.examination.question.redis.QuestionRedisService;
import io.lettuce.core.RedisCommandTimeoutException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.jesse.examination.core.redis.keys.ConcatRedisKey.allKeysOfUser;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.correctTimesHashKey;

/** 问题数据统计 Redis 服务实现类。*/
@Slf4j
@Component
public class QuestionRedisServiceImpl implements QuestionRedisService
{
    /** 响应式 Redis 模板。*/
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * <p>
     *     从模板中获取的 Redis 哈希数据操作器。
     *     他的 3 个类型参数需要我说明一下：
     * </p>
     *
     * <code><pre>
     * ReactiveHashOperations{@literal <H, HK, HV>}
     * H:  整个 Hash 结构的键类型
     * HK: 哈希结构内部的键类型
     * HV: 哈希结构内部的值类型
     * </pre></code>
     */
    private
    ReactiveHashOperations<String, String, Long> hashOperations;

    /** 在依赖注入完成后，获取哈希数据操作器。*/
    @PostConstruct
    private void setHashOperator()
    {
        Objects.requireNonNull(this.redisTemplate);
        this.hashOperations = this.redisTemplate.opsForHash();
    }

    private <T> @NotNull Mono<T>
    genericErrorHandel(Throwable exception, T fallbackValue)
    {
        switch (exception)
        {
            case RedisConnectionFailureException redisConnectionFailureException ->
                log.error(
                    "Redis connect failed! Cause: {}",
                    redisConnectionFailureException.toString()
                );
            case RedisCommandTimeoutException redisCommandTimeoutException ->
                log.warn(
                    "Redis operator time out! Cause: {}",
                    redisCommandTimeoutException.toString()
                );
            case SerializationException serializationException ->
                log.error(
                    "Data deserialization failed! Cause: {}",
                    serializationException.toString()
                );
            default ->
                log.error(
                    "Redis operator exception! Cause: {}",
                    exception.toString()
                );
        }

        return (fallbackValue != null) ? Mono.just(fallbackValue) : Mono.error(exception);
    }

    /**
     * 某用户登录时，
     * 将从该用户存档中读取所有问题答对次数数据以哈希表的形式存入 Redis。
     *
     * @param userName            用户名
     * @param quesCorrectTimesMap 从 JSON 存档文件中读出，并映射后的哈希表，
     *                            映射关系是：问题 ID -> 问题答对次数
     * @return 是否成功存入 Redis?
     */
    @Override
    public Mono<Boolean>
    loadUserQuestionCorrectTimes(
        String userName,
        Map<String, Long> quesCorrectTimesMap
    )
    {
        return this.hashOperations
                   .putAll(
                       correctTimesHashKey(userName),
                       quesCorrectTimesMap)
                   .timeout(Duration.ofSeconds(3L))
                   .onErrorResume(
                        (exception) ->
                            this.genericErrorHandel(exception, false)
                   );
    }

    /**
     * 某用户登出时，
     * 将从 Redis 中读取所有问题答对次数数据哈希表，再存回用户存档中。
     *
     * @param userName 用户名
     *
     * @return 从 Redis 中读取的所有问题答对次数数据哈希表
     */
    @Override
    public Mono<Map<String, Long>>
    getUserQuestionCorrectTimes(String userName)
    {
        return this.hashOperations
                   .entries(correctTimesHashKey(userName))
                   .timeout(Duration.ofSeconds(3L))
                   .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                   .onErrorResume((exception) ->
                       this.genericErrorHandel(exception, new TreeMap<>())
                   )
                   .map((unorderedMap) -> {
                       /*
                        * 由于 Redis 配置中键是按照 String 存储的，
                        * 这里需要准备一个 TreeMap，把键转成整数再比较。
                        */
                       TreeMap<String, Long> sortedMap = new TreeMap<>(
                           Comparator.comparing(Integer::parseInt)
                       );

                       sortedMap.putAll(unorderedMap);

                       return sortedMap;
                   });
    }

    /**
     * 用户在练习时答对了一道题，这题的答对次数 + 1。
     *
     * @param userName   用户名
     * @param questionId 问题 ID
     *
     * @return 增加后该用户本题的答对次数
     */
    @Override
    public Mono<Long> incrementUserQuestionCorrectTime(
        String userName, Long questionId
    )
    {
        return this.hashOperations
                   .increment(
                        correctTimesHashKey(userName),
                        String.valueOf(questionId), 1L
                   )
                  .timeout(Duration.ofSeconds(3L))
                  .onErrorResume((exception) ->
                      this.genericErrorHandel(exception, 0L)
                  );
    }

    /**
     * 用户在登出且数据存回存档后，
     * 需要删除他在 Redis 中的所有信息。
     *
     * @param userName 用户名
     *
     * @return 是否成功从 Redis 中删除?
     */
    @Override
    public Mono<Boolean> deleteUserInfo(String userName)
    {
        return this.redisTemplate
                   .keys(allKeysOfUser(userName))
                   .timeout(Duration.ofSeconds(3L))
                   .flatMap(redisTemplate::delete)
                   .then(Mono.just(true))
                   .onErrorReturn(false);
    }
}
