package com.jesse.examination.user.redis.impl;

import com.jesse.examination.user.redis.UserRedisService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

import static com.jesse.examination.core.redis.errorhandle.RedisGenericErrorHandle.genericErrorHandel;
import static com.jesse.examination.core.redis.keys.ConcatRedisKey.*;

/** 用户模块 Redis 服务实现类。*/
@Slf4j
@Component
public class UserRedisServiceImpl implements UserRedisService
{
    /** 响应式 Redis 模板。*/
    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    private
    ReactiveHashOperations<String, String, Long> hashOperations;

    /** 在依赖注入完成后，获取哈希数据操作器。*/
    @PostConstruct
    private void setHashOperator()
    {
        Objects.requireNonNull(this.redisTemplate);
        this.hashOperations = this.redisTemplate.opsForHash();
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
        if (userName == null || userName.isEmpty())
        {
            return genericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        if (quesCorrectTimesMap == null || quesCorrectTimesMap.isEmpty())
        {
            return genericErrorHandel(
                new IllegalArgumentException(
                    "Question Correct TimesMap not be null or empty!"
                ), null
            );
        }

        return this.hashOperations
            .putAll(
                correctTimesHashKey(userName),
                quesCorrectTimesMap)
            .timeout(Duration.ofSeconds(3L))
            .onErrorResume(
                (exception) ->
                    genericErrorHandel(exception, false)
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
        if (userName == null || userName.isEmpty())
        {
            return genericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        return this.hashOperations
            .entries(correctTimesHashKey(userName))
            .timeout(Duration.ofSeconds(3L))
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .onErrorResume((exception) ->
                genericErrorHandel(exception, new TreeMap<>())
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
     * 用户在登出且数据存回存档后，
     * 需要删除他在 Redis 中的所有信息。
     *
     * @param userName 用户名
     *
     * @return 是否成功从 Redis 中删除?
     */
    @Override
    public Mono<Boolean>
    deleteUserInfo(String userName)
    {
        if (userName == null || userName.isEmpty())
        {
            return genericErrorHandel(
                new IllegalArgumentException(
                    "User name not be null or empty!"
                ), null
            );
        }

        return this.redisTemplate
            .keys(allKeysOfUser(userName))
            .timeout(Duration.ofSeconds(3L))
            .flatMap(redisTemplate::delete)
            .then(Mono.just(true))
            .onErrorResume((exception) ->
                genericErrorHandel(exception, false)
            );
    }

    @Autowired
    public Flux<String> getAllUsers()
    {
        return this.redisTemplate.scan(
            ScanOptions.scanOptions()
                .match(allUserPatten()).build()
        )
        .map((key) -> {
                String[] subStr = key.split(":");

                log.info("{}", Arrays.toString(subStr));

                return subStr[1];
            }
        );
    }
}
