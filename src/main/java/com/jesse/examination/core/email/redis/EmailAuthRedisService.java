package com.jesse.examination.core.email.redis;

import com.jesse.examination.core.email.service.EmailAuthQueryService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.time.Duration;

import static com.jesse.examination.core.redis.errorhandle.RedisGenericErrorHandle.redisGenericErrorHandel;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.SERVICE_AUTH_CODE;

/** 邮箱服务授权码服务类。*/
@Slf4j
public class EmailAuthRedisService
{
    @Autowired
    private EmailAuthQueryService emailAuthQueryService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    /**
     * 在所以依赖注入都完成后，
     * 将邮箱发送人的邮箱号和服务授权码读出，按指定 key 存入 Redis（自动执行）。
     */
    @PostConstruct
    private void readEmailPublisherInfo()
    {
        this.emailAuthQueryService
            .findEmailPublisherInfoById(1)
            .flatMap(
                (publisherInfo) ->
                    this.redisTemplate.opsForValue()
                        .set(ENTERPRISE_EMAIL_ADDRESS.toString(), publisherInfo.getEmail())
                        .then(this.redisTemplate.opsForValue()
                                 .set(SERVICE_AUTH_CODE.toString(), publisherInfo.getEmailAuthCode())
                        )
            )
            .timeout(Duration.ofSeconds(5L))
            .onErrorResume((exception) ->
                redisGenericErrorHandel(exception, null))
            .doOnSuccess((isSuccess) ->
                log.info(
                    "Read email publisher info to redis complete! Result: {}",
                    isSuccess
                )
            ).subscribe();
    }
}
