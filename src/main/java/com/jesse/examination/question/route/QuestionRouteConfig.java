package com.jesse.examination.question.route;

import com.jesse.examination.question.service.QuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static com.jesse.examination.question.route.QuestionServiceURL.*;

/** 问题查询模块路由函数配置类。 */
@Slf4j
@Configuration
@EnableWebFlux
public class QuestionRouteConfig
{
    @Autowired
    private QuestionService questionService;

    @Bean
    public RouterFunction<ServerResponse>
    questionRouteFunction()
    {
        return RouterFunctions.route()
            .GET(SINGLE_QUERY_URI,     this.questionService::getQuestionWithOptions)
            .GET(PAGINATION_QUERY_URI, this.questionService::getPaginatedQuestions)
            .GET(PAGINATION_QUERY_WITH_CORRECT_URI, this.questionService::getAllQuestionWithCorrectOption)
            .PUT(INCREMENT_USER_QUESTION_CORRECT_TIME_URI, this.questionService::incrementUserQuestionCorrectTime)
            .PUT(SET_USER_QUESTION_CORRECT_TIME_URI, this.questionService::setUserQuestionCorrectTime)
            .PUT(CLEAR_USER_QUESTION_CORRECT_TIME_URI, this.questionService::clearUserQuestionCorrectTime)
            .build();
    }
}
