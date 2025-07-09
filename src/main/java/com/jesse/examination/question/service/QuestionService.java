package com.jesse.examination.question.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface QuestionService
{

    /** 获取单条问题和所有选项。 */
    Mono<ServerResponse>
    getQuestionWithOptions(ServerRequest request);

    /**
     * 查询 question 表中的所有问题和正确选项，
     * 以及 options  表中对应的所有选项内容。
     */
    Mono<ServerResponse>
    getAllQuestions(ServerRequest request);

    /**
     * 查询 question 表中的所有问题和正确选项，
     * 以及 options  表中对应的正确选项内容。
     */
    Mono<ServerResponse>
    getAllQuestionWithCorrectOption(ServerRequest request);
}
