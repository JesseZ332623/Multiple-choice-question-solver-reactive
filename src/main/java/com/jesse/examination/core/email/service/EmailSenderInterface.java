package com.jesse.examination.core.email.service;

import com.jesse.examination.core.email.dto.EmailContent;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Mono;

import java.util.AbstractMap;

/**  */
public interface EmailSenderInterface
{
    Mono<AbstractMap.SimpleEntry<Boolean, String>>
    sendEmail(@NotNull EmailContent emailContent);
}
