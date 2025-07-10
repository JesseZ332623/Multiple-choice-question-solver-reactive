package com.jesse.examination.score.service;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** 成绩表服务接口类。*/
public interface ScoreRecordService
{
    /** 分页查找指定用户的所有成绩记录。*/
    Mono<ServerResponse>
    findPaginatedScoreRecordByUserName(ServerRequest request);

    /** 找出指定用户名的最新成绩记录。*/
    Mono<ServerResponse>
    findLatestScoreRecordByUserName(ServerRequest request);

    /** 为指定用户插入一条新的成绩。*/
    Mono<ServerResponse>
    insertNewScoreRecordByUserId(ServerRequest request);

    /** 删除指定用户对应的所有成绩，返回删除的行数。*/
    Mono<ServerResponse>
    deleteAllScoreRecordByUserName(ServerRequest request);

    /** 清空 score_record 表。*/
    Mono<ServerResponse>
    truncateScoreRecordTable(ServerRequest request);
}
