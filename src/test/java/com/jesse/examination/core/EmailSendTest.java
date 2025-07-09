package com.jesse.examination.core;

import com.jesse.examination.core.email.dto.EmailContent;
import com.jesse.examination.core.email.service.EmailAuthQueryService;
import com.jesse.examination.core.email.service.EmailSenderInterface;
import com.jesse.examination.core.email.utils.VarifyCodeGenerator;
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

import java.util.AbstractMap;

import static com.jesse.examination.core.redis.keys.ProjectRedisKey.ENTERPRISE_EMAIL_ADDRESS;
import static com.jesse.examination.core.redis.keys.ProjectRedisKey.SERVICE_AUTH_CODE;
import static com.jesse.examination.core.logmakers.LogMakers.EMAIL_SENDER;
import static java.lang.String.format;

/** 邮件验证码发送测试类。 */
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

    /**
     * 在所以依赖注入都完成后，
     * 将邮箱发送人的邮箱号和服务授权码读出，按指定 key 存入 Redis。
     */
    @PostConstruct
    void readEmailPublisherInfo()
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

    private @NotNull Mono<EmailContent>
    getEmailContentDTO(String userName, String userEmail, String varifyCode)
    {
        EmailContent emailContent = new EmailContent();

        emailContent.setTo(userEmail);
        emailContent.setSubject("用户：" + userName + " 请查收您的验证码。");
        emailContent.setTextBody(
            format(
                "用户：%s 您的验证码是：[%s]，" +
                    "请在 %d 分钟内完成验证，超过 %d 分钟后验证码自动失效！",
                userName, varifyCode, 3, 3
            )
        );

        // 暂时没有附件
        emailContent.setAttachmentPath(null);

        return Mono.just(emailContent);
    }

    @Test
    void EmailSenderTest()
    {
        Mono<AbstractMap.SimpleEntry<Boolean, String>> sendEmailStream
            = VarifyCodeGenerator.generateVarifyCode(8).flatMap(
            (code) -> {
                // 尝试发送邮件
                return this.getEmailContentDTO(
                        "Peter",
                        "zhj3191955858@gmail.com",
                        code
                    ).flatMap(
                        (content) ->
                            this.emailSender.sendEmail(content)
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
