package com.jesse.examination.question.service.impl;

import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.core.respponse.Link;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.question.dto.QuestionWithOptionDTO;
import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.question.repository.OptionRepository;
import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.question.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.*;

import static com.jesse.examination.core.respponse.URLParamPrase.praseNumberRequestParam;
import static java.lang.String.format;

@Slf4j
@Component
public class QuestionServiceImpl implements QuestionService
{
    /** 问题相关操作根 URI。*/
    final private static String QUESTION_ROOT_URI
        = "/api/question";

    @Autowired
    private ProjectProperties projectProperties;

    @Autowired
    private ResponseBuilder responseBuilder;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private OptionRepository optionRepository;

    /**
     * 在单挑查询问题数据的时候，
     * 拼装响应体的 HATEOAS 元数据。
     *
     * @param questionId 问题 ID
     *
     * @return 返回承载了本次请求完整 HATEOAS 元数据的 Mono
     */
    private Mono<Set<Link>>
    getQuestionQueryLink(Integer questionId)
    {
        return this.questionRepository
            .count().map(
                (count) -> {
                    Set<Link> links = new HashSet<>();

                    links.add(
                        new Link("next",
                            projectProperties.getServerRootURL() +
                                QUESTION_ROOT_URI +
                                "?id=" +
                                ((questionId + 1 >= count) ? count : questionId + 1),
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("prev",
                            projectProperties.getServerRootURL() +
                                QUESTION_ROOT_URI +
                                "?id=" +
                                ((questionId - 1 <= 0) ? 1 : questionId - 1),
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("first",
                            projectProperties.getServerRootURL() +
                            QUESTION_ROOT_URI + "?id=1",
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("last",
                            projectProperties.getServerRootURL() +
                                QUESTION_ROOT_URI +
                                "?id=" + count,
                            HttpMethod.GET
                        )
                    );

                    return links;
                }
            );
    }

    @Override
    public @NotNull Mono<ServerResponse>
    getQuestionWithOptions(ServerRequest request)
    {
        return praseNumberRequestParam(request, "id", Integer::parseInt)
                .flatMap(
                    (id) ->
                        this.questionRepository.findById(id)
                            .switchIfEmpty(
                                Mono.error(
                                    new ResourceNotFoundException(
                                        format("Question (id = %d) not found!", id)
                                    )
                                )
                            )
                            .flatMap((question) ->
                                this.optionRepository.findByQuestionId(question.getId())
                                    .collectList()
                                    .map((options) -> {
                                        question.setOptions(options);
                                        return new QuestionWithOptionDTO(question);
                                    })
                            )
                ).flatMap((question) ->
                    this.getQuestionQueryLink(question.getId())
                    .flatMap(
                        (links) ->
                            this.responseBuilder.OK(
                            question,
                            format("Query question id = {%d} Success!", question.getId()),
                            null, links
                    )
                )
            ).onErrorResume(
                ResourceNotFoundException.class,
                (exception) ->
                    this.responseBuilder.NOT_FOUND(exception.getMessage(), exception)
            ).onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
            );
    }

    /**
     * 查询 question 表中的所有问题和正确选项，
     * 以及 options  表中对应的所有选项内容。
     */
    @Override
    public @NotNull Mono<ServerResponse>
    getAllQuestions(ServerRequest request)
    {
        return this.questionRepository.findQuestionWithAllOptions()
                   .switchIfEmpty(
                       Mono.error(
                            new ResourceNotFoundException(
                                "Question table hasn't any data!"
                            )
                       )
                   )
                   .collectList()
                   .flatMap((questions) -> {
                        // 构建响应体
                        return this.responseBuilder.OK(
                            questions,
                            format("Query all questions complete! %d rows!", questions.size()),
                            null, null
                        );
                   }
            );
    }

    /**
     * 查询 question 表中的所有问题和正确选项，
     * 以及 options  表中对应的正确选项内容。
     */
    @Override
    public @NotNull Mono<ServerResponse>
    getAllQuestionWithCorrectOption(ServerRequest request) {
        return Mono.empty();
    }
}
