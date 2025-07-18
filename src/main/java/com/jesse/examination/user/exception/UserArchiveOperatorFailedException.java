package com.jesse.examination.user.exception;

/** 在对用户存档操作中出现的任何错误，都会 re-throw 成本异常。 */
public class UserArchiveOperatorFailedException extends RuntimeException
{
    public UserArchiveOperatorFailedException(String message) {
        super(message);
    }
}
