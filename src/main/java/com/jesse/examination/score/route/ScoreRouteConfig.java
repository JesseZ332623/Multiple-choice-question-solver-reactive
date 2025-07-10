package com.jesse.examination.score.route;

import com.jesse.examination.score.service.ScoreRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.examination.score.route.ScoreServiceURL.*;

/** 用户成绩模块路由函数配置类。*/
@Configuration
public class ScoreRouteConfig
{
    @Autowired
    private ScoreRecordService scoreRecordService;

    @Bean
    RouterFunction<ServerResponse>
    scoreRouteFunction()
    {
        return RouterFunctions
            .route()
            .GET(PAGINATED_SCORE_QUERY_URI, scoreRecordService::findPaginatedScoreRecordByUserName)
            .build();
    }
}
