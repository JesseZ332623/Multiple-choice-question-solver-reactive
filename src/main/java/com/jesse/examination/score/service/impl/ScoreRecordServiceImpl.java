package com.jesse.examination.score.service.impl;

import com.jesse.examination.core.exception.PaginationOffsetOutOfRangeException;
import com.jesse.examination.core.exception.ResourceNotFoundException;
import com.jesse.examination.core.respponse.Link;
import com.jesse.examination.core.respponse.ResponseBuilder;
import com.jesse.examination.score.dto.ScoreRecordQueryDTO;
import com.jesse.examination.score.repository.ScoreRecordRepository;
import com.jesse.examination.score.service.ScoreRecordService;
import io.netty.handler.timeout.TimeoutException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jesse.examination.core.respponse.URLParamPrase.praseNumberRequestParam;
import static com.jesse.examination.core.respponse.URLParamPrase.praseRequestParam;
import static com.jesse.examination.score.route.ScoreServiceURL.*;
import static java.lang.String.format;

/** 用户成绩服务实现类。（2024-07-10 未完成）*/
@Slf4j
@Component
public class ScoreRecordServiceImpl implements ScoreRecordService
{
    @Autowired
    private ResponseBuilder responseBuilder;

    @Autowired
    private ScoreRecordRepository scoreRecordRepository;

    /**
     * 在分页查询指定用户所有成绩时，组装响应体的 HATEOAS 元数据。
     *
     * @param page      第几页？
     * @param amount    一页几条数据？
     * @param totalItem 一共有几条数据？
     *
     * @return 承载了本次响应完整 HATEOAS 元数据的 Mono
     */
    private @NotNull Mono<Set<Link>>
    getScorePaginationQueryLink(String name, int page, int amount, long totalItem)
    {
        return Mono.fromCallable(
            () -> {
                /*
                 * 分页的计算可以更加简洁，使用 Math.ceil() 方法，
                 * 若总数据量除以每页数据量不是整数（还有多余的数据不足一整页），就向上取整。
                 */
                int totalPage
                    = (int) Math.ceil((double) (totalItem / amount));

                Set<Link> links = new HashSet<>();

                links.add(
                    new Link("next_page",
                        PAGINATED_SCORE_QUERY_URI +
                            "?name=" + name +
                            "&page=" + ((page + 1 > totalPage) ? page : page + 1) +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("prev_page",
                        PAGINATED_SCORE_QUERY_URI +
                            "?name=" + name +
                            "&page=" + Math.max(page - 1, 1) +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("first_page",
                        PAGINATED_SCORE_QUERY_URI +
                            "?name=" + name +
                            "&page=1" +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                links.add(
                    new Link("last_page",
                        PAGINATED_SCORE_QUERY_URI +
                            "?name=" + name +
                            "&page=" + totalPage +
                            "&amount=" + amount,
                        HttpMethod.GET
                    )
                );

                return links;
            }
        );
    }

    @Override
    public Mono<ServerResponse>
    findPaginatedScoreRecordByUserName(ServerRequest request)
    {
        Mono<String> nameMono
            = praseRequestParam(request, "name");
        Mono<Integer> pageMono
            = praseNumberRequestParam(request, "page", Integer::parseInt);
        Mono<Integer> amountMono
            = praseNumberRequestParam(request, "amount", Integer::parseInt);

        return Mono.zip(nameMono, pageMono, amountMono)
            .flatMap((params) ->
            {
                final String  name = params.getT1();
                return this.scoreRecordRepository.findScoreAmountByUserName(name)
                    .timeout(Duration.ofSeconds(5))
                    .flatMap(
                    (totalItem) ->
                    {
                        System.out.printf("Total item of %s = %d%n", name, totalItem);

                        final Integer page   = params.getT2();
                        final Integer amount = params.getT3();

                        int offset = (page - 1) * amount;

                        if (offset > totalItem)
                        {
                            return Mono.error(
                                new PaginationOffsetOutOfRangeException(
                                    format(
                                        "Input page (which is: %d) param is to large!", page
                                    )
                                )
                            );
                        }

                        return this.scoreRecordRepository
                            .findPaginatedScoreRecordByUserName(name, amount, offset)
                            .timeout(Duration.ofSeconds(5))
                            .switchIfEmpty(
                                Mono.error(
                                    new ResourceNotFoundException(
                                        format(
                                            "pagination param invalid! " +
                                            "Your param: (userName = %s, page = %d, amount = %d)",
                                            name, page, amount
                                        )
                                    )
                                )
                            )
                            .collectList()
                            .flatMap((scores) ->
                                this.getScorePaginationQueryLink(name, page, amount, totalItem).flatMap(
                                    (links) ->
                                    {
                                        ResponseBuilder.APIResponse<List<ScoreRecordQueryDTO>>
                                            response = new ResponseBuilder.APIResponse<>(HttpStatus.OK);

                                        for (Link link : links)
                                        {
                                            response.withLink(
                                                link.getRel(), link.getHref(), link.getMethod()
                                            );
                                        }

                                        response.withPagination(page, amount, totalItem);
                                        response.setData(scores);
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
                );
            })
            .onErrorResume(
                IllegalArgumentException.class,
                (exception) ->
                    this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
            )
            .onErrorResume(
                PaginationOffsetOutOfRangeException.class,
                (exception) ->
                    this.responseBuilder.BAD_REQUEST(exception.getMessage(), exception)
            )
            .onErrorResume(
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

    @Override
    public Mono<ServerResponse>
    findLatestScoreRecordByUserName(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    insertNewScoreRecordByUserId(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    deleteAllScoreRecordByUserName(ServerRequest request) {
        return null;
    }

    @Override
    public Mono<ServerResponse>
    truncateScoreRecordTable(ServerRequest request) {
        return null;
    }
}
