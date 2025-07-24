package com.jesse.examination.question.service.impl;

import com.jesse.examination.core.exception.PaginationOffsetOutOfRangeException;
import com.jesse.examination.core.respponse.Link;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.question.dto.FullQuestionInfoDTO;
import com.jesse.examination.question.dto.QuestionWithCorrectDTO;
import com.jesse.examination.question.redis.impl.QuestionRedisServiceImpl;
import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.question.service.QuestionService;
import io.netty.handler.timeout.TimeoutException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

import static com.jesse.examination.core.respponse.ResponseBuilder.APIResponse;
import static com.jesse.examination.core.respponse.URLParamPrase.praseNumberRequestParam;
import static com.jesse.examination.core.respponse.URLParamPrase.praseRequestParam;
import static com.jesse.examination.question.route.QuestionServiceURL.QUESTION_PAGINATION_QUERY_URI;
import static com.jesse.examination.question.route.QuestionServiceURL.QUESTION_SINGLE_QUERY_URI;
import static java.lang.String.format;

/** 问题业务模块服务实现类。*/
@Slf4j
@Component
public class QuestionServiceImpl implements QuestionService
{
    @Autowired
    private ResponseBuilder responseBuilder;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionRedisServiceImpl questionRedisService;

    /**
     * 连接池预热操作，
     * 旨在检查数据库是否可达或者相关配置是否有误。
     */
    @PostConstruct
    void warmUpPool()
    {
        this.questionRepository.count()
            .timeout(Duration.ofSeconds(3))
            .retry(3)
            .subscribe();
    }

    /**
     * 本服务实现通用的错误处理类，
     * 按照不同的异常返回不同的响应体。
     */
    private Mono<ServerResponse>
    genericErrorHandle(Mono<ServerResponse> mono)
    {
        return mono.onErrorResume(
            IllegalArgumentException.class,
            (exception) ->
                this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
        ).onErrorResume(
            ResourceNotFoundException.class,
            (exception) ->
                this.responseBuilder.NOT_FOUND(exception.getMessage(), exception)
        ).onErrorResume(
            TimeoutException.class,
            (exception) ->
                this.responseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
        ).onErrorResume(DataAccessResourceFailureException.class,
            (exception) ->
                this.responseBuilder.INTERNAL_SERVER_ERROR(exception.getMessage(), exception)
        );
    }

    /**
     * 在单条查询问题数据的时候，
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
                            QUESTION_SINGLE_QUERY_URI + "?id=" +
                                ((questionId + 1 >= count) ? count : questionId + 1),
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("prev",
                            QUESTION_SINGLE_QUERY_URI + "?id=" +
                                ((questionId - 1 <= 0) ? 1 : questionId - 1),
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("first",
                            QUESTION_SINGLE_QUERY_URI + "?id=1",
                            HttpMethod.GET
                        )
                    );

                    links.add(
                        new Link("last",
                            QUESTION_SINGLE_QUERY_URI + "?id=" + count,
                            HttpMethod.GET
                        )
                    );

                    return links;
                }
            );
    }

    /**
     * 获取单条问题和所有选项（具体的 URL 和参数见文档）。
     *
     * @param request 从前端传来的 HTTP 请求体
     *
     * @throws IllegalArgumentException             当请求体的参数不存在或非法时抛出
     * @throws ResourceNotFoundException            当查询数据不存在时抛出
     * @throws TimeoutException                     当查询因某些原因超时时抛出
     * @throws DataAccessResourceFailureException   当因网络波动等原因拿不到数据库连接时抛出
     *
     * @return 组装好地响应体 Mono
     */
    @Override
    public @NotNull Mono<ServerResponse>
    getQuestionWithOptions(ServerRequest request)
    {
        Mono<ServerResponse> responseMono
            = praseNumberRequestParam(request, "id", Integer::parseInt)
            .flatMap(
                (id) ->
                    this.questionRepository.findOneQuestionWithAllOptions(id)
                        .timeout(Duration.ofSeconds(5))
                        .switchIfEmpty(
                            Mono.error(
                                new ResourceNotFoundException(
                                    format("Question (id = %d) not found!", id)
                                )
                            )
                        )
            ).flatMap((question) ->
                this.getQuestionQueryLink(question.getQuestionId())
                    .flatMap(
                        (links) ->
                            this.responseBuilder.OK(
                                question,
                                format(
                                    "Query question id = {%d} success!",
                                    question.getQuestionId()
                                ),
                                null, links
                            )
                    )
            );

        return this.genericErrorHandle(responseMono);
    }

    /**
     * 在分页查询问题完整信息时，组装响应体的 HATEOAS 元数据。
     *
     * @param page      第几页？
     * @param amount    一页几条数据？
     * @param totalItem 一共有几条数据？
     *
     * @return 承载了本次响应完整 HATEOAS 元数据的 Mono
     */
    private Mono<Set<Link>>
    getQuestionPaginationQueryLink(int page, int amount, long totalItem)
    {
        return Mono.fromCallable(
            () -> {
                int totalPage
                    = (int) Math.ceil((double) (totalItem / amount));

                Set<Link> links = new HashSet<>();

                links.add(
                    new Link("next_page",
                        QUESTION_PAGINATION_QUERY_URI +
                            "?page=" + ((page + 1 > totalPage) ? page : page + 1) +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("prev_page",
                        QUESTION_PAGINATION_QUERY_URI +
                            "?page=" + Math.max(page - 1, 1) +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("first_page",
                        QUESTION_PAGINATION_QUERY_URI +
                            "?page=1" +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("last_page",
                        QUESTION_PAGINATION_QUERY_URI +
                            "?page=" + totalPage +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                return links;
            }
        );
    }

    /**
     * 分页的查询表中问题的完整数据（具体的 URL 和参数见文档）。
     *
     * @param request 从前端传来的请求体
     *
     * @throws IllegalArgumentException             当请求体的参数不存在或非法时抛出
     * @throws PaginationOffsetOutOfRangeException  当计算出的偏移量不在数据总数范围内时抛出
     * @throws ResourceNotFoundException            当查询数据不存在时抛出
     * @throws TimeoutException                     当查询因某些原因超时时抛出
     * @throws DataAccessResourceFailureException   当因网络波动等原因拿不到数据库连接时抛出
     *
     * @return 返回组装好地响应体。
     */
    @Override
    public @NotNull Mono<ServerResponse>
    getPaginatedQuestions(ServerRequest request)
    {
        Mono<ServerResponse> responseMono
            = Mono.zip(
                praseNumberRequestParam(request, "page", Integer::parseInt),
                praseNumberRequestParam(request, "amount", Integer::parseInt)
            ).flatMap((params) ->
                this.questionRepository.count().flatMap(
                    (count) ->
                    {
                        int page   = params.getT1();
                        int amount = params.getT2();

                        int offset = (page - 1) * amount;  // 计算这一页对应的偏移量

                        if (offset > count)
                        {
                            return Mono.error(
                                new PaginationOffsetOutOfRangeException(
                                    format(
                                        "Input page (which is: %d) param is to large!", page
                                    )
                                )
                            );
                        }

                        return this.questionRepository
                            .findAllQuestionWithAllOptions(amount, offset)
                            .timeout(Duration.ofSeconds(5))
                            .switchIfEmpty(
                                Mono.error(
                                    new ResourceNotFoundException(
                                        format(
                                            "pagination param invalid! (page = %d, amount = %d)",
                                            page, amount
                                        )
                                    )
                                )
                            )
                            .collectList()
                            .flatMap((questions) ->
                                this.getQuestionPaginationQueryLink(page, amount, count)
                                    .flatMap(
                                        (links) ->
                                        {
                                            APIResponse<List<FullQuestionInfoDTO>>
                                                response = new APIResponse<>(HttpStatus.OK);

                                            for (Link link : links)
                                            {
                                                response.withLink(
                                                    link.getRel(), link.getHref(), link.getMethod()
                                                );
                                            }

                                            response.withPagination(params.getT1(), params.getT2(), count)
                                                    .setMessage(
                                                        format(
                                                            "Query questions (Page = %d, Amount = %d) complete!",
                                                            page, amount
                                                        ))
                                                    .setData(questions);

                                            return this.responseBuilder.build(
                                                (headers) ->
                                                    headers.setContentType(MediaType.APPLICATION_JSON),
                                                response
                                            );
                                        }
                                    ));
                    }
                ))
            .onErrorResume(
                PaginationOffsetOutOfRangeException.class,
                (exception) ->
                    this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
            );

        return this.genericErrorHandle(responseMono);
    }

    /**
     * 分页的获取所有问题的
     * ID，内容，正确选项，正确答案（具体的 URL 和参数见文档）。
     *
     * @param request 从前端传来的请求体
     *
     * @throws IllegalArgumentException             当请求体的参数不存在或非法时抛出
     * @throws PaginationOffsetOutOfRangeException  当计算出的偏移量不在数据总数范围内时抛出
     * @throws ResourceNotFoundException            当查询数据不存在时抛出
     * @throws TimeoutException                     当查询因某些原因超时时抛出
     * @throws DataAccessResourceFailureException   当因网络波动等原因拿不到数据库连接时抛出
     *
     * @return 返回组装好地响应体。
     */
    @Override
    public @NotNull Mono<ServerResponse>
    getAllQuestionWithCorrectOption(ServerRequest request)
    {
        Mono<ServerResponse> responseMono
            = Mono.zip(
                praseNumberRequestParam(request, "page", Integer::parseInt),
                praseNumberRequestParam(request, "amount", Integer::parseInt)
            ).flatMap((params) ->
                this.questionRepository.count().flatMap(
                    (count) ->
                    {
                        int page   = params.getT1();
                        int amount = params.getT2();

                        int offset = (page - 1) * params.getT2();                       // 计算这一页对应的偏移量

                        if (offset > count)
                        {
                            return Mono.error(
                                new PaginationOffsetOutOfRangeException(
                                    format(
                                        "Input page (which is: %d) param is to large!", page
                                    )
                                )
                            );
                        }

                        return this.questionRepository
                            .findAllQuestionWithCorrectAnswer(amount, offset)
                            .timeout(Duration.ofSeconds(5))
                            .switchIfEmpty(
                                Mono.error(
                                    new ResourceNotFoundException(
                                        format(
                                            "pagination param invalid! (page = %d, amount = %d)",
                                            page, amount
                                        )
                                    )
                                )
                            )
                            .collectList()
                            .flatMap((questions) ->
                                this.getQuestionPaginationQueryLink(page, amount, count)
                                    .flatMap(
                                        (links) ->
                                        {
                                            APIResponse<List<QuestionWithCorrectDTO>>
                                                response = new APIResponse<>(HttpStatus.OK);

                                            for (Link link : links)
                                            {
                                                response.withLink(
                                                    link.getRel(), link.getHref(), link.getMethod()
                                                );
                                            }

                                            response.withPagination(page, amount, count);
                                            response.setData(questions);
                                            response.setMessage(
                                                format(
                                                    "Query score record (Page = %d, Amount = %d) complete!",
                                                    page, amount
                                                )
                                            );

                                            return this.responseBuilder.build(
                                                (headers) ->
                                                    headers.setContentType(MediaType.APPLICATION_JSON),
                                                response
                                            );
                                        }
                                    ));
                    }
                ))
            .onErrorResume(
                PaginationOffsetOutOfRangeException.class,
                (exception) ->
                    this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
            );

        return this.genericErrorHandle(responseMono);
    }

    /** 用户在练习时答对了一道题，这题的答对次数 + 1。*/
    public Mono<ServerResponse>
    incrementUserQuestionCorrectTime(ServerRequest request)
    {
        Mono<String> userNameMono
            = praseRequestParam(request, "name");

        Mono<Long> questionIdMono
            = praseNumberRequestParam(
                request, "ques_id", Long::parseLong
            );

        Mono<ServerResponse> responseMono
            = Mono.zip(userNameMono, questionIdMono)
            .flatMap((params) ->
            {
                String userName   = params.getT1();
                Long   questionId = params.getT2();

                 return this.questionRedisService
                            .incrementUserQuestionCorrectTime(userName, questionId)
                            .flatMap((newVal) ->
                            {
                                if (newVal != -1)
                                {
                                    return this.responseBuilder.OK(
                                        newVal, format(
                                            "Set user: %s question id: %d correct times to %d complete!",
                                            userName, questionId, newVal
                                        ),
                                        null, null
                                    );
                                }
                                else
                                {
                                    return this.responseBuilder.BAD_REQUEST(
                                        format(
                                            "Set user: %s question id: %d correct times plus one failed!",
                                            userName, questionId
                                        ), null
                                    );
                                }
                            });
            });

        return this.genericErrorHandle(responseMono);
    }

    /** 将某用户的某道问题的答对次数设为 value。*/
    public Mono<ServerResponse>
    setUserQuestionCorrectTime(ServerRequest request)
    {
        Mono<String> userNameMono
            = praseRequestParam(request, "name");

        Mono<Long> questionIdMono
            = praseNumberRequestParam(
            request, "ques_id", Long::parseLong
        );

        Mono<Long> newValMono
            = praseNumberRequestParam(
                request, "value", Long::parseLong
        );

        Mono<ServerResponse> responseMono
            = Mono.zip(userNameMono, questionIdMono, newValMono)
            .flatMap((params) ->
            {
                String userName   = params.getT1();
                Long   questionId = params.getT2();
                Long   value      = params.getT3();

                return this.questionRedisService
                           .setUserQuestionCorrectTime(userName, questionId, value)
                           .flatMap((newVal) ->
                           {
                               System.out.printf(
                                   "[setUserQuestionCorrectTime()] Return val = %d%n",
                                   newVal
                               );

                               if (newVal != -1)
                               {
                                   return this.responseBuilder.OK(
                                       newVal, format(
                                           "Set user: %s question id: %d correct times to %d complete!",
                                           userName, questionId, newVal
                                       ),
                                       null, null
                                   );
                               }
                               else
                               {
                                   return this.responseBuilder.BAD_REQUEST(
                                       format(
                                           "Set user: %s question id: %d correct times failed!",
                                           userName, questionId
                                       ), null
                                   );
                               }
                           });
            });

        return this.genericErrorHandle(responseMono);
    }

    /** 将某用户所有问题的答对次数清空为 0。 */
    public Mono<ServerResponse>
    clearUserQuestionCorrectTime(ServerRequest request)
    {
        Mono<ServerResponse> responseMono
            = praseRequestParam(request, "name")
              .flatMap((userName) ->
                  this.questionRedisService
                      .clearUserQuestionCorrectTime(userName)
                      .flatMap((isSuccess) ->
                          (isSuccess)
                              ? this.responseBuilder.OK(
                                null, format("Clear user: %s question correct times complete!", userName),
                                null, null)
                              : this.responseBuilder.BAD_REQUEST(
                                    format(
                                        "Clear user: %s question correct times failed!", userName),
                                   null)
                      )
              );

        return this.genericErrorHandle(responseMono);
    }
}
