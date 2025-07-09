package com.jesse.examination.question.entity.Option;

import lombok.Getter;

/** 问题选项枚举类。*/
@Getter
public enum AnswerOption
{
    A("A"), B("B"), C("C"), D("D");

    private final String option;

    AnswerOption(String option) {
        this.option = option;
    }
}