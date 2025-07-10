package com.jesse.examination.question.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface QuestionService
{
    /** 获取单条问题和其所有选项。 */
    Mono<ServerResponse>
    getQuestionWithOptions(ServerRequest request);

    /** 分页查询问题和其所有选项。*/
    Mono<ServerResponse>
    getPaginatedQuestions(ServerRequest request);

    /** 分页查询问题和其正确答案。*/
    Mono<ServerResponse>
    getAllQuestionWithCorrectOption(ServerRequest request);
}
