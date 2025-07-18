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
     * 文本文件存储读取方法（响应式）。
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     */
    Mono<String>
    readTextFile(Path filePath, String fileName);

    /**
     * 非文本文件存储通用方法（响应式）。
     *
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param fileData 文件数据（这里是字节数组）
     */
    Mono<Void>
    saveDataFile(Path filePath, String fileName, byte[] fileData);

    /**删除指定文件方法（响应式）。*/
    Mono<Void>
    deleteFile(Path filePath, String fileName);

    /**
     * 重命名路径（响应式）。
     *
     * @param oldPathName 旧路径名，需要确保其存在
     * @param newPathName 新路径名，需要确保其不存在（√）
     */
    Mono<Void>
    renameDirectory(Path oldPathName, Path newPathName);
}