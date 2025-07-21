package com.jesse.examination.user.utils.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.examination.core.file.exception.FileOperatorException;
import com.jesse.examination.core.file.service.FileTransferService;
import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.question.repository.QuestionRepository;
import com.jesse.examination.user.exception.UserArchiveOperatorFailedException;
import com.jesse.examination.user.redis.UserRedisService;
import com.jesse.examination.user.utils.UserArchiveManager;
import com.jesse.examination.user.utils.dto.AvatarImageData;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.lang.String.format;

/** 用户存档管理工具类（用户和管理员都会用到）。*/
@Slf4j
@Component
public class UserArchiveManagerImpl implements UserArchiveManager
{
    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private UserRedisService userRedisService;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ProjectProperties projectProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AVATAR_FILE_NAME        = "avatar.png";
    private static final String CORRECT_TIMES_FILE_NAME = "correct_times.json";

    private static AvatarImageData   DEFAULT_AVATAR;
    private static Map<String, Long> DEFAULT_CORRECT_MAP;

    private record
    QuestionCorrectTimes(String question_id, Long correct_times) {}

    @PostConstruct
    void setDefaultData()
    {
        try
        {
            DEFAULT_CORRECT_MAP
                = new TreeMap<>(Comparator.comparing(Long::parseLong));

            Long questionAmount
                = Objects.requireNonNull(questionRepository.count().block());

            LongStream.range(0L, questionAmount)
                .forEach((questionId) ->
                    DEFAULT_CORRECT_MAP.put(String.valueOf(questionId), 0L)
                );

            DEFAULT_AVATAR = AvatarImageData.fromBytes(
                Files.readAllBytes(
                    Paths.get(
                        projectProperties.getDefaultAvatarPath()
                    )
                )
            );
        }
        catch (IOException exception)
        {
            log.error(
                "Read default avatar failed! Cause: {}",
                exception.getMessage(), exception
            );

           DEFAULT_AVATAR = AvatarImageData.fromBytes(new byte[0]);
        }
    }

    @Override
    public Mono<AvatarImageData> getDefaultAvatarImage()
    {
        return Mono.just(
            Objects.requireNonNull(DEFAULT_AVATAR)
            ).onErrorResume(
                NullPointerException.class,
                (exception) -> {
                    log.error("DEFAULT_AVATAR is null! Please check file exists!");

                    return Mono.error(
                        new UserArchiveOperatorFailedException(
                            "DEFAULT_AVATAR is null! Please check file exists!"
                        )
                    );
                }
        );
    }

    @Override
    public Mono<AvatarImageData>
    getAvatarImageByUserName(String userName)
    {
        return Mono.fromCallable(() -> {
            Path avatarImageLocation
                = Path.of(this.projectProperties.getUserArchivePath())
                      .resolve(userName)
                      .resolve(AVATAR_FILE_NAME).normalize();

            if (!Files.exists(avatarImageLocation))
            {
                throw new FileNotFoundException(
                    format(
                        "avatar image location: %s not exist!",
                        avatarImageLocation
                    )
                );
            }

            System.out.println("Avatar location: " + avatarImageLocation);

            return AvatarImageData.fromBytes(
                Files.readAllBytes(avatarImageLocation)
            );
        }).onErrorResume((exception) -> {

            log.error(
                "Can't get avatar image by user: {}, Cause: {}",
                userName, exception.getMessage(), exception
            );

            return Mono.error(
                new UserArchiveOperatorFailedException(
                    format("Can't get avatar image by user: %s.", userName)
                )
            );
        });
    }

    @Override
    public Mono<Void>
    setUserAvatarImage(String userName, AvatarImageData avatar)
    {
        return Mono.defer(() -> {
            Path avatarImageLocation
                = Path.of(this.projectProperties.getUserArchivePath())
                .resolve(userName).normalize();

            return this.fileTransferService
                .saveDataFile(
                    avatarImageLocation, AVATAR_FILE_NAME,
                    avatar.getAvatarBytes()
                );
        }).onErrorResume((exception) -> {
            log.error(
                "Can't set avatar for user: {}! Cause: {}",
                userName, exception.getMessage(), exception
            );

            return Mono.error(
                new UserArchiveOperatorFailedException(
                    format("Can't set avatar for user: %s!", userName)
                )
            );
        });
    }

    @Override
    public Mono<Void>
    renameUserArchiveDir(String oldUserName, String newUserName)
    {
        return Mono.defer(() ->
        {
            if (oldUserName.equals(newUserName)) { return Mono.empty(); }

            Path oldArchivePath
                = Path.of(this.projectProperties.getUserArchivePath())
                      .resolve(oldUserName).normalize();
            Path newArchivePath
                = Path.of(this.projectProperties.getUserArchivePath())
                      .resolve(newUserName).normalize();

            return this.fileTransferService.renameDirectory(
                oldArchivePath, newArchivePath
            ).onErrorResume((exception) ->
                Mono.error(
                    new UserArchiveOperatorFailedException(
                        exception.getMessage()
                    )
                )
            );
        });
    }

    @Override
    public Mono<Void>
    createNewArchiveForNewUser(String newUserName)
    {
        return Mono.fromCallable(() -> {
            Path newArchivePath
                = Path.of(this.projectProperties.getUserArchivePath())
                      .resolve(newUserName).normalize();

            List<QuestionCorrectTimes> quesCorrectTimes
                = DEFAULT_CORRECT_MAP.entrySet()
                .stream()
                .map((entry) ->
                    new QuestionCorrectTimes(entry.getKey(), entry.getValue())
                ).toList();

            Mono<Void> setDefaultUserAvatar
                = this.setUserAvatarImage(newUserName, DEFAULT_AVATAR)
                      .onErrorResume((exception) -> {
                          log.error(
                              "Set default avatar for {} failed! Cause: {}",
                              newUserName, exception.getMessage(), exception
                          );

                          return Mono.error(new UserArchiveOperatorFailedException(
                              format(
                                  "Set default avatar for %s failed! Cause: %s",
                                  newUserName, exception.getMessage()
                              )
                          ));
                      });

            Mono<Void> setDefaultCorrectTimes
                = this.fileTransferService
                      .saveTextFile(
                          newArchivePath, CORRECT_TIMES_FILE_NAME,
                          this.objectMapper.writeValueAsString(quesCorrectTimes)
                      ).onErrorResume(
                    FileOperatorException.class,
                    (exception) -> {
                        log.error(exception.getMessage(), exception);

                        return Mono.error(
                            new UserArchiveOperatorFailedException(exception.getMessage())
                        );
                    }
                );

            return setDefaultUserAvatar.then(setDefaultCorrectTimes);
        }).flatMap(mono -> mono);
    }

    @Override
    public Mono<Void>
    readUserArchive(String userName)
    {
        return Mono.defer(() ->
        {
            Path archivePath
                = Path.of(this.projectProperties.getUserArchivePath())
                      .resolve(userName);

            log.info("Archive path: {}", archivePath);

            return this.fileTransferService
                       .readTextFile(archivePath, CORRECT_TIMES_FILE_NAME)
                       .flatMap((json) -> {
                            try {
                                List<Map<String, Object>> data
                                    = this.objectMapper.readValue(
                                        json, new TypeReference<>() {}
                                    );

                                Map<String, Long> finalData
                                    = data.stream().collect(
                                        Collectors.toMap(
                                            (item) ->
                                                item.get("question_id").toString(),
                                            (item) ->
                                                Long.parseLong(item.get("correct_times").toString()))
                                    );

                                log.info("Data: will save to redis:\n{}", finalData);

                                return this.userRedisService
                                    .loadUserQuestionCorrectTimes(userName, finalData)
                                    .filter((isSuccess) -> !isSuccess)
                                    .flatMap((ignore) ->
                                        Mono.error(new UserArchiveOperatorFailedException(
                                            "Sava correct times map to redis failed!")
                                        )
                                    );
                            }
                            catch (JsonProcessingException exception)
                            {
                                log.error(
                                    "Process json failed! Cause: {}",
                                    exception.getMessage(), exception
                                );

                                return Mono.error(
                                    new UserArchiveOperatorFailedException(
                                        format(
                                            "Process json failed! Cause: %s",
                                            exception.getMessage()
                                        )
                                    )
                                );
                            }
                       });
        });
    }

    @Override
    public Mono<Void>
    saveUserArchive(String userName)
    {
        return Mono.fromCallable(() -> {
            this.userRedisService
                .getUserQuestionCorrectTimes(userName)
                .flatMap((correctTimeMap) ->
                {
                    List<QuestionCorrectTimes> questionCorrectTimes
                        = correctTimeMap.entrySet()
                        .stream()
                        .map((entry) ->
                            new QuestionCorrectTimes(entry.getKey(), entry.getValue()))
                        .toList();
                    try
                    {
                        return this.fileTransferService.saveTextFile(
                            Path.of(this.projectProperties.getUserArchivePath()),
                            userName,
                            this.objectMapper.writeValueAsString(questionCorrectTimes)
                        ).onErrorResume((exception) -> {
                            log.error(
                                "Save user archive failed! Cause: {}",
                                exception.getMessage(), exception
                            );

                            return Mono.error(new UserArchiveOperatorFailedException(
                                format(
                                    "Save user archive failed! Cause: %s",
                                    exception.getMessage()
                                )
                            ));
                        });
                    }
                    catch (JsonProcessingException exception)
                    {
                        log.error(
                            "Process json failed! Cause: {}",
                            exception.getMessage(), exception
                        );

                        return Mono.error(
                            new UserArchiveOperatorFailedException(
                                format(
                                    "Process json failed! Cause: %s",
                                    exception.getMessage()
                                )
                            )
                        );
                    }
                });

            return null;
        }).then();
    }

    @Override
    public Mono<Void>
    deleteUserArchive(String userName)
    {
        return Mono.fromCallable(() -> {
            Mono<Void> correctTimesFileDelete
                = this.fileTransferService
                .deleteFile(
                    Path.of(this.projectProperties.getDefaultAvatarPath()),
                    CORRECT_TIMES_FILE_NAME
                ).onErrorResume((exception) -> {
                    log.error(
                        "Delete archive for {} failed! Cause: {}",
                        userName, exception.getMessage(), exception
                    );

                    return Mono.error(
                        new UserArchiveOperatorFailedException(
                            format(
                                "Delete archive for %s failed! Cause: %s",
                                userName, exception.getMessage()
                            )
                        )
                    );
                });

            Mono<Void> deleteUserInfo
                = this.userRedisService
                      .deleteUserInfo(userName)
                      .flatMap((isSuccess) ->
                              (isSuccess)
                                  ? null
                                  : Mono.error(
                                      new UserArchiveOperatorFailedException(
                                        "Sava correct times map to redis failed!"
                                      )
                              )
                      );

            return correctTimesFileDelete.then(deleteUserInfo);
        }).then();
    }
}
