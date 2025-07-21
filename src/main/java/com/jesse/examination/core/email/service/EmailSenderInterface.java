package com.jesse.examination.core.email.service;

import com.jesse.examination.core.email.dto.EmailContent;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

/** 封装了 javax.mail 库的响应式邮件发送器接口。*/
public interface EmailSenderInterface
{
    Mono<Void>
    sendEmail(@NotNull EmailContent emailContent);
}
