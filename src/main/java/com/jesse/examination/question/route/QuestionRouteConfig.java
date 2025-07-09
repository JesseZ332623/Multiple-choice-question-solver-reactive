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
            .GET("/api/question",     this.questionService::getQuestionWithOptions)
            .GET("/api/all_question", this.questionService::getAllQuestions)
            .build();
    }
}
