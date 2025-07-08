package com.jesse.examination.core.file.service.impl;

import com.jesse.examination.core.file.service.FileTransferService;
import com.jesse.examination.core.file.exception.FileOperatorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import static java.lang.String.format;

@Slf4j
@Component
public class FileTransferServiceImpl implements FileTransferService
{
    /** 文件操作异常信息模板。 */
    private static final String ERROR_MESSAGE_TEMPLATE
        = "File operator failed! (File name: [%s], Cause: %s.)";

    /**
     * 检查传入的文件路径是否存在，不存在则创建之。
     */
    private static Path
    prepareDirectory(Path path) throws IOException
    {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        return path;
    }

    /** 提取文件的扩展名并转成大写。 */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        return (dotIndex == -1)
                ? "UNKNOW"
                : fileName.substring(dotIndex).toUpperCase();
    }

    @Override
    public Mono<Void> saveTextFile(
        Path filePath, String fileName, String fileData)
    {
        Objects.requireNonNull(filePath, "File path cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");

        return Mono.fromCallable(
            () -> {
                Path fullPath
                    = prepareDirectory(filePath).resolve(fileName).normalize();

                Files.writeString(
                    fullPath, fileData,
                    StandardOpenOption.CREATE,             // 不存在则创建
                    StandardOpenOption.TRUNCATE_EXISTING   // 存在则清空
                );

                return null;
            }
        ).subscribeOn(Schedulers.boundedElastic())
        .doOnError((exception) ->
             log.error(
                 "[saveTextFile()] Save {} file: {} failed!",
                 fileName,
                 getFileExtension(fileName),
                 exception
             )
        )
        .onErrorResume((exception) ->
            Mono.error(new FileOperatorException(
                format(
                    ERROR_MESSAGE_TEMPLATE,
                    fileName, exception.getMessage()
                ), exception
        ))).then();
    }

    @Override
    public Mono<Void> saveDataFile(
        Path filePath, String fileName, byte[] fileData)
    {
        Objects.requireNonNull(filePath, "File path cannot be null");
        Objects.requireNonNull(fileName, "File name cannot be null");

        return Mono.fromCallable(
            () -> {
                Path fullPath
                    = prepareDirectory(filePath).resolve(fileName).normalize();

                Files.write(
                    fullPath, fileData,
                    StandardOpenOption.CREATE,             // 不存在则创建
                    StandardOpenOption.TRUNCATE_EXISTING   // 存在则清空
                );

                return null;
            }
        ).subscribeOn(Schedulers.boundedElastic())
            .doOnError((exception) ->
            log.error(
                    "Save {} file: {} failed!",
                    fileName,
                    getFileExtension(fileName),
                    exception
                )
            )
            .onErrorResume((exception) ->
                Mono.error(new FileOperatorException(
                    format(
                        ERROR_MESSAGE_TEMPLATE,
                        fileName, exception.getMessage()
                    ), exception
                ))).then();
    }
}