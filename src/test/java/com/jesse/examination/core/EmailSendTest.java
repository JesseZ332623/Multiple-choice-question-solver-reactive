package com.jesse.examination.core;

import com.jesse.examination.core.email.dto.EmailContent;
import com.jesse.examination.core.email.service.EmailAuthQueryService;
import com.jesse.examination.core.email.service.EmailSenderInterface;
import com.jesse.examination.core.email.utils.VarifyCodeGenerator;
import com.jesse.examination.core.properties.ProjectProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.AbstractMap;

import static com.jesse.examination.core.redis.keys.ProjectRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.SERVICE_AUTH_CODE;
import static com.jesse.examination.core.logmakers.LogMakers.EMAIL_SENDER;

/** 邮件验证码发送测试类。*/
@Slf4j
@SpringBootTest
public class EmailSendTest
{
    @Autowired
    private EmailAuthQueryService emailAuthQueryService;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Qualifier(value = "createEmailSender")
    private EmailSenderInterface emailSender;

    @Autowired
    private ProjectProperties projectProperties;

    /**
     * 在所以依赖注入都完成后，
     * 将邮箱发送人的邮箱号和服务授权码读出，按指定 key 存入 Redis。
     */
    @PostConstruct
    private void readEmailPublisherInfo()
    {
        Mono<Boolean> readPublisherInfo
            = this.emailAuthQueryService
            .findEmailPublisherInfoById(1)
            .flatMap(
                (publisherInfo) ->
                    this.redisTemplate.opsForValue()
                        .set(ENTERPRISE_EMAIL_ADDRESS.toString(), publisherInfo.getEmail())
                        .then(
                            this.redisTemplate.opsForValue()
                                .set(SERVICE_AUTH_CODE.toString(), publisherInfo.getEmailAuthCode())
                        )
            ).doOnSuccess((isSuccess) ->
                log.info(
                    "Read email publisher info to redis complete! Result: {}",
                    isSuccess
                )
            );

        StepVerifier.create(readPublisherInfo)
                    .expectNext(true)
                    .verifyComplete();
    }

    @Test
    public void EmailSenderTest()
    {
        Mono<AbstractMap.SimpleEntry<Boolean, String>> sendEmailStream
            = VarifyCodeGenerator.generateVarifyCode(
                Integer.parseInt(this.projectProperties.getVarifyCodeLength())
            ).flatMap((code) -> {
                // 尝试发送邮件
                return this.emailSender
                           .sendEmail(EmailContent.fromVarify(
                               "Jesse",
                               "zhj3191955858@gmail.com",
                               code,
                               Duration.ofMinutes(
                                    Long.parseLong(this.projectProperties.getVarifyCodeExpiration()) / 60
                               )
                           )
                    )
                    .doOnSuccess((res) ->
                        log.info(
                            EMAIL_SENDER, "[{}, {}].",
                            res.getKey(), res.getValue()
                        )
                    );
            }
        );

        StepVerifier.create(sendEmailStream)
            .expectNextMatches(AbstractMap.SimpleEntry::getKey)
            .verifyComplete();
    }
}
