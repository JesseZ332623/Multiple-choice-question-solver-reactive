package com.jesse.examination.core.file.service;

import reactor.core.publisher.Mono;

import java.nio.file.Path;

/** 文件操作核心接口。 */
public interface FileTransferService
{
    /**
     * 文本文件存储通用方法（响应式）。
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param fileData 文件数据（通常是 JSON 字符串）
     */
    Mono<Void>
    saveTextFile(Path filePath, String fileName, String fileData);

    /**
     * 非文本文件存储通用方法（响应式）。
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param fileData 文件数据（这里是字节数组）
     */
    Mono<Void>
    saveDataFile(Path filePath, String fileName, byte[] fileData);
}