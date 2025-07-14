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

    /** 用户在练习时答对了一道题，这题的答对次数 + 1。 */
    Mono<ServerResponse>
    incrementUserQuestionCorrectTime(ServerRequest request);

    /** 将某用户的某道问题的答对次数设为 value。 */
    Mono<ServerResponse>
    setUserQuestionCorrectTime(ServerRequest request);

    /** 将某用户所有问题的答对次数清空为 0。 */
    Mono<ServerResponse>
    clearUserQuestionCorrectTime(ServerRequest request);
}
