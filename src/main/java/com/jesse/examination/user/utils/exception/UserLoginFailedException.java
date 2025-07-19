package com.jesse.examination.user.utils.exception;

/** 用户登录操作的整个过程中所抛出的异常，都会 re-throw 成本异常。*/
public class UserLoginFailedException extends RuntimeException
{
    public UserLoginFailedException(String message) { super(message); }
}
