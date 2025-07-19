package com.jesse.examination.user.exception;

/** 当注册或修改用户时出现用户名重复，抛出本异常。*/
public class DuplicateUserException extends RuntimeException
{
    public DuplicateUserException(String message) {
        super(message);
    }
}
