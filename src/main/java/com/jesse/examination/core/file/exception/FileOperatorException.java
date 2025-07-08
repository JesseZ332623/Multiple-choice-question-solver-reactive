package com.jesse.examination.core.file.exception;

/** 文件操作异常。 */
public class FileOperatorException extends RuntimeException
{
    public FileOperatorException(String message, Throwable cause) {
        super(message, cause);
    }
}