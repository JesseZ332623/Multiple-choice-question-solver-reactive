package com.jesse.examination.user.utils.exception;

/** 验证码不匹配时所抛出的异常。*/
public class VarifyCodeMismatchException extends RuntimeException
{
    public VarifyCodeMismatchException(String message) {
        super(message);
    }
}
