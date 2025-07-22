package com.jesse.examination.core;

import com.jesse.examination.core.file.service.FileTransferService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.nio.file.Paths;
import static com.jesse.examination.core.logmakers.LogMakers.FILE_OPERATOR;


/** 文件传输测试类。*/
@Slf4j
@SpringBootTest
class FileTransferTest
{
    @Value("${file.upload.test-dir}")
    private String storageFilePath;

    @Autowired
    private FileTransferService fileTransferService;

    /** 提取文件的扩展名并转成大写。 */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");

        return (dotIndex == -1)
            ? "UNKNOW"
            : fileName.substring(dotIndex).toUpperCase();
    }

    @Test
    public void TestTextFileTransfer()
    {
        final String fileName = "test.json";
        final String testJson = """
            [
                {"id": 11, "name": "Jesse", "date": "2024-08-12"},
                {"id": 11, "name": "Jesse", "date": "2023-08-12"},
                {"id": 11, "name": "Jesse", "date": "2022-08-12"},
                {"id": 11, "name": "Jesse", "date": "2021-08-12"},
                {"id": 11, "name": "Jesse", "date": "2020-08-12"},
                {"id": 11, "name": "Jesse", "date": "2019-08-12"}
            ]
            """;

        Mono<Void> saveTextFileStream
            = this.fileTransferService.saveTextFile(
                Paths.get(storageFilePath).normalize(), fileName, testJson
            ).doOnSuccess((ignore) ->
                log.info(
                    "[TestTextFileTransfer()] Save {} file {} complete!",
                    getFileExtension(fileName),
                    Paths.get(storageFilePath).resolve(fileName).normalize()
                )
            );

        StepVerifier.create(saveTextFileStream).verifyComplete();
    }

    @Test
    public void TestDataFileTransfer()
    {
        final String fileName = "test.dat";
        final byte[] testData = new byte[16];

        for (int index = 0; index < testData.length; ++index) {
            testData[index] = (byte) index;
        }

        Mono<Void> saveDataFileStream
            = this.fileTransferService.saveDataFile(
                Paths.get(storageFilePath).normalize(),
                fileName, testData
        ).doOnSuccess((ignore) ->
            log.info(
                "[TestDataFileTransfer()] Save {} file {} complete!",
                getFileExtension(fileName),
                Paths.get(storageFilePath).resolve(fileName).normalize()
            )
        );

        StepVerifier.create(saveDataFileStream).verifyComplete();
    }

    private void directoryRenameHandle(String oldName, String newName)
    {
        Path oldPath
            = Path.of(storageFilePath).resolve(oldName).normalize();

        Path newPath
            = Path.of(storageFilePath).resolve(newName).normalize();

        Mono<Void> renameDirectoryStream
            = this.fileTransferService
                  .renameDirectory(oldPath, newPath)
                  .doOnSuccess((ignore) ->
                      log.info(
                          FILE_OPERATOR,
                          "[TestDirectoryRename()] Rename path: {} -> {} complete!",
                          oldPath, newPath
                      )
                  )
                 .doOnError((e) ->
                     log.error(FILE_OPERATOR, "Opps!", e)
                 );

        StepVerifier.create(renameDirectoryStream).verifyComplete();
    }

    @Test
    public void TestDirectoryRename()
    {
        this.directoryRenameHandle("Jesse", "Jesse_EC");
        this.directoryRenameHandle("Jesse_EC", "Jesse");
    }
}
