package com.jesse.examination.core;

import com.jesse.examination.core.email.dto.EmailContent;
import com.jesse.examination.core.email.exception.EmailException;
import com.jesse.examination.core.email.service.EmailAuthQueryService;
import com.jesse.examination.core.email.service.EmailSenderInterface;
import com.jesse.examination.core.email.utils.VerifyCodeGenerator;
import com.jesse.examination.core.properties.ProjectProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.FileNotFoundException;
import java.time.Duration;

import static com.jesse.examination.core.email.exception.EmailException.ErrorType.ATTACHMENT_NOT_EXIST;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.SERVICE_AUTH_CODE;
import static com.jesse.examination.core.logmakers.LogMakers.EMAIL_SENDER;

/** 邮件发送测试类。*/
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
    public void VerifyCodeEmailSenderTest()
    {
        Mono<Void> sendEmailStream
            = VerifyCodeGenerator.generateVerifyCode(
                Integer.parseInt(this.projectProperties.getVarifyCodeLength()))
            .flatMap((code) -> {
                final String userName  = "Peter-Griffin";
                final String userEmail = "zhuhaojin@139erp.com";
                // 尝试发送邮件
                return this.emailSender
                           .sendEmail(EmailContent.fromVarify(
                               userName, userEmail, code,
                               Duration.ofMinutes(
                                    Long.parseLong(
                                        this.projectProperties.getVarifyCodeExpiration()) / 60
                               )
                           ))
                    .doOnSuccess((res) ->
                        log.info(
                            EMAIL_SENDER,
                            "Send email to {}, {} success!",
                            userName, userEmail
                        )
                    );
            }
        );

        StepVerifier.create(sendEmailStream).verifyComplete();
    }

    @Test
    public void AttachmentEmailSenderTest()
    {
        Mono<Void> sendEmailStream
            = Mono.defer(() -> {
                final String userName  = "Peter-Griffin";
                final String userEmail = "zhuhaojin@139erp.com";

            try
            {
                return
                this.emailSender
                    .sendEmail(
                        EmailContent
                            .formWithAttachment(
                                userName, userEmail,
                                "出生皮特！温州江南皮革厂倒闭了！",
                                "E:\\图片素材\\Family-Guy Avatar\\Perter 头像.png"
                            )
                    )
                    .doOnSuccess((res) ->
                        log.info(
                            EMAIL_SENDER,
                            "Send email with attachment to {}, {} success!",
                            userName, userEmail
                        )
                    )
                    .timeout(Duration.ofSeconds(10L));
            }
            catch (FileNotFoundException e) {
                return Mono.error(e);
            }
        })
        .onErrorResume(
            FileNotFoundException.class,
            (exception) -> {
                log.error("{}", exception.getMessage(), exception);
                return Mono.error(
                    new EmailException(
                        ATTACHMENT_NOT_EXIST,
                        exception.getMessage(), exception
                    )
                );
            }
        );

        StepVerifier.create(sendEmailStream)
                    .verifyComplete();
    }
}
