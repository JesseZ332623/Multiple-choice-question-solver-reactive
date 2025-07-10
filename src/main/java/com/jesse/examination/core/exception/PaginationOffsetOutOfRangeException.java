package com.jesse.examination.core.exception;

/** 当计算出的偏移量不在数据总数范围内时抛出。*/
public class PaginationOffsetOutOfRangeException extends RuntimeException
{
    public PaginationOffsetOutOfRangeException(String message) {
        super(message);
    }
}
