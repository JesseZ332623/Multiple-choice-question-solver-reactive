package com.jesse.examination.user.utils.exception;

/** 密码验证失败时抛出本异常。*/
public class PasswordMissmatchException extends RuntimeException
{
    public PasswordMissmatchException(String message) {
        super(message);
    }
}
