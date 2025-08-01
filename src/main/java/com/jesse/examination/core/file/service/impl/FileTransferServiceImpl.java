package com.jesse.examination.core.file.service.impl;

import com.jesse.examination.core.file.service.FileTransferService;
import com.jesse.examination.core.file.exception.FileOperatorException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;

/** 文件操作核心方法实现类。*/
@Slf4j
@Component
public class FileTransferServiceImpl implements FileTransferService
{
    /** 文件操作异常信息模板。 */
    private static final String ERROR_MESSAGE_TEMPLATE
        = "File operator failed! (File: [%s], Cause: %s)";

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
    private static String getFileExtension(String fileName)
    {
        int dotIndex = fileName.lastIndexOf(".");

        return (dotIndex == -1)
                ? "UNKNOW"
                : fileName.substring(dotIndex).toUpperCase();
    }

    @Override
    public Mono<Void> saveTextFile(
        Path filePath, String fileName, String fileData)
    {
        Objects.requireNonNull(filePath, "File path cannot be null!");
        Objects.requireNonNull(fileName, "File name cannot be null!");

        return Mono.fromCallable(
            () -> {
                Path fullPath
                    = prepareDirectory(filePath).resolve(fileName).normalize();

                log.info("[saveTextFile()] Full path: {}", fullPath);

                Files.writeString(
                    fullPath, fileData,
                    StandardOpenOption.CREATE,             // 不存在则创建
                    StandardOpenOption.TRUNCATE_EXISTING   // 存在则清空
                );

                return null;
            })
            .subscribeOn(Schedulers.boundedElastic())
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
    public Mono<String>
    readTextFile(Path filePath, String fileName)
    {
        Objects.requireNonNull(filePath, "File path cannot be null!");
        Objects.requireNonNull(fileName, "File name cannot be null!");

        return Mono.fromCallable(
            () -> {
                Path fullPath
                    = prepareDirectory(filePath).resolve(fileName).normalize();

                if (!Files.exists(fullPath)) {
                    throw new FileNotFoundException(
                        format("File %s not exist!", fullPath)
                    );
                }

                return Files.readString(fullPath, StandardCharsets.UTF_8);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError((exception) ->
                log.error(
                    "[readTextFile()] Read {} file: {} failed!",
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
                    ), exception)
                )
            );
    }

    @Override
    public Mono<Void> saveDataFile(
        Path filePath, String fileName, byte[] fileData)
    {
        Objects.requireNonNull(filePath, "File path cannot be null!");
        Objects.requireNonNull(fileName, "File name cannot be null!");

        return Mono.fromCallable(
            () -> {
                Path fullPath
                    = prepareDirectory(filePath).resolve(fileName).normalize();

                log.info("[saveDataFile()] Full path: {}", fullPath);

                Files.write(
                    fullPath, fileData,
                    StandardOpenOption.CREATE,             // 不存在则创建
                    StandardOpenOption.TRUNCATE_EXISTING   // 存在则清空
                );

                return null;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError((exception) ->
            log.error(
                    "[saveDataFile()] Save {} file: {} failed!",
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
                ), exception)
            )
        ).then();
    }

    private @NotNull Mono<Void>
    cleanAllFileUnderPath(Path filePath)
    {
        return Mono.fromCallable(() -> {
            List<IOException> exceptions = new ArrayList<>();

            try (Stream<Path> paths = Files.walk(filePath))
            {
                paths.sorted(Comparator.reverseOrder())
                     .forEach((path) -> {
                         try {
                             Files.delete(path);
                         }
                         catch (IOException exception)
                         {
                             exceptions.add(exception);
                             log.error("Delete file: {} failed!", path, exception);
                         }
                     });
            }
            catch (IOException exception)
            {
                exceptions.add(exception);
                log.error("Delete file: {} failed!", filePath, exception);
            }

            if (!exceptions.isEmpty()) {
                throw new FileOperatorException(
                    format("Failed to delete files under path: %s", filePath),
                    exceptions.getFirst()
                );
            }

            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void>
    deleteFile(Path filePath, String fileName)
    {
        Objects.requireNonNull(filePath, "File path cannot be null!");
        Objects.requireNonNull(fileName, "File name cannot be null!");

        if (fileName.equals("*"))
        {
            log.info("Delete all file under path: {}", filePath);
            return this.cleanAllFileUnderPath(filePath);
        }

        return Mono.fromCallable(() -> {
            Path pullPath
                = prepareDirectory(filePath)
                    .resolve(fileName).normalize();

            if (!Files.deleteIfExists(pullPath))
            {
                throw new FileNotFoundException(
                    format(
                        "[deleteFile()] File path: %s not exist!",
                        filePath
                    )
                );
            }

            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError((exception) ->
            log.error(
                "[deleteFile()] Delete {} file: {} failed!",
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
                ), exception)
            )
        ).then();
    }

    @Override
    public Mono<Void>
    renameDirectory(Path oldPathName, Path newPathName)
    {
        Objects.requireNonNull(oldPathName, "Old file path path cannot be null!");
        Objects.requireNonNull(newPathName, "New file path name cannot be null!");

        return Mono.fromCallable(
            () -> {
                if (!Files.exists(oldPathName))
                {
                    throw new IllegalArgumentException(
                        format("renameDirectory(): source path: %s not exist!", oldPathName)
                    );
                }

                Files.createDirectories(newPathName.getParent());

                /*
                 * 新旧路径校验完后，进行文件的移动操作，Files.move() 方法在不同情况下的行为也不同：
                 *
                 *  case 1：若同盘移动则仅修改文件的元数据，速度极快。
                 *  case 2: 若跨盘移动则触发文件的复制，在目录层级很深的时候有性能问题。
                 *
                 * 这里还需要解释下面两个参数：
                 *
                 * 1. StandardCopyOption.REPLACE_EXISTING
                 *      在操作路径时，若目标路径已经存在，则直接抛出 FileAlreadyExistsException
                 *
                 * 2. StandardCopyOption.ATOMIC_MOVE
                 *      将该移动操作视为一个整体（原子化），如果期间出现任何失败，则都会回滚操作。
                 *
                 * 但当前项目的对该方法的使用不涉及跨盘的操作，所以不需要 REPLACE_EXISTING 参数。
                 */
                Files.move(
                    oldPathName, newPathName,
                    StandardCopyOption.ATOMIC_MOVE
                );

                return null;
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError((exception) ->
            log.error(
            "Rename directory {} -> {} failed!",
            oldPathName, newPathName, exception
        ))
        .onErrorResume((exception) ->
            Mono.error(
                new FileOperatorException(
                    format(
                        ERROR_MESSAGE_TEMPLATE,
                        format("%s -> %s", oldPathName, newPathName),
                        exception.getMessage()
                    ), exception
                )
            )
        ).then();
    }
}