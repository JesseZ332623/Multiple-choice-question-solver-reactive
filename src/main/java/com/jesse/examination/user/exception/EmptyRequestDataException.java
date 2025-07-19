package com.jesse.examination.user.exception;

/** 当用户发起 POST 请求但是请求体中没有携带任何数据时抛出本异常。*/
public class EmptyRequestDataException extends RuntimeException
{
    public EmptyRequestDataException(String message) {
        super(message);
    }
}
