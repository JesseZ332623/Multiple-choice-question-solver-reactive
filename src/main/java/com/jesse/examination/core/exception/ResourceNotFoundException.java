package com.jesse.examination.core.exception;

/** 当查询某资源失败时，抛出本异常。*/
public class ResourceNotFoundException extends RuntimeException
{
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
