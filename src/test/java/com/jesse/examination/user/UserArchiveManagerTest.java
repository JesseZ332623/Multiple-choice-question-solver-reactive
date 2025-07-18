package com.jesse.examination.user;

import com.jesse.examination.core.properties.ProjectProperties;
import com.jesse.examination.user.utils.UserArchiveManager;
import com.jesse.examination.user.utils.dto.AvatarImageData;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.String.format;

/** 用户模块存档管理器服务测试类。*/
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserArchiveManagerTest
{
    @Autowired
    private UserArchiveManager userArchiveManager;

    @Autowired
    private ReactiveRedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProjectProperties projectProperties;

    private static final List<String> TEST_USERS
        = List.of("Jesse", "Peter", "Lois", "Cris", "Meg");

    public Mono<String> cleanRedis()
    {
        return this.redisTemplate.getConnectionFactory()
            .getReactiveConnection()
            .serverCommands()
            .flushAll(RedisServerCommands.FlushOption.ASYNC);
    }

    private String getAvatarDataForHex(@NotNull AvatarImageData data)
    {
        return IntStream.range(0, data.getAvatarBytes().length)
                  .mapToObj((index) ->
                      format("%02X", data.getAvatarBytes()[index] & 0xFF))
                  .collect(Collectors.joining(" "));
    }

    private void cleanAllArchiveAfterTest()
    {
        try (Stream<Path> paths
                 = Files.walk(
                     Path.of(projectProperties.getUserArchivePath()))
        )
        {
            paths.sorted(Comparator.reverseOrder())
                 .forEach(path -> {
                     try {
                         log.info("Delete file: {}", path);
                         Files.delete(path);
                     }
                     catch (IOException exception)
                     {
                         log.error(
                             "Delete file: {} failed! Cause: {}",
                             path, exception.getMessage(), exception
                         );

                         throw new RuntimeException(exception);
                     }
                 });

        }
        catch (IOException exception)
        {
            log.error(
                "Clean all archive file failed! Cause: {}",
                exception.getMessage(), exception
            );

            throw new RuntimeException(exception);
        }
    }

    @Test
    @Order(1)
    void TestGetDefaultAvatarImage()
    {
        Mono<AvatarImageData> getDefaultAvatar
            = this.userArchiveManager
                  .getDefaultAvatarImage()
                  .doOnSuccess((defaultAvatar) ->
                      System.out.println(this.getAvatarDataForHex(defaultAvatar))
                  ).onErrorResume((exception) -> {
                      log.error("{}", exception.getMessage(), exception);
                      return Mono.error(exception);
                  });

        StepVerifier.create(getDefaultAvatar)
                    .expectNextMatches((avatarData) ->
                        avatarData.getAvatarBytes() != null)
                    .verifyComplete();
    }

    @Test
    @Order(2)
    void TestCreateNewArchiveForNewUser()
    {
        Flux<Void> createNewAvatars
            = Flux.fromIterable(TEST_USERS)
            .flatMap((name) -> {

                Mono<Void> createNewAvatarForOnce =
                this.userArchiveManager
                    .createNewArchiveForNewUser(name)
                    .doOnSuccess((ignore) ->
                        log.info("Create new achievement for use: {} complete.", name)
                    );

                StepVerifier.create(createNewAvatarForOnce).verifyComplete();

                return createNewAvatarForOnce;
            });

        StepVerifier.create(createNewAvatars).verifyComplete();
    }

    @Test
    @Order(3)
    void TestGetAvatarImageByUserName()
    {
        TEST_USERS.forEach(
            (name) -> {
                Mono<AvatarImageData> getAvatarImageByUserName =
                    this.userArchiveManager
                        .getAvatarImageByUserName(name)
                        .cache();

                StepVerifier.create(getAvatarImageByUserName)
                    .consumeNextWith((avatar) -> {
                        log.info("Avatar bytes for user: {}", name);
                        System.out.println(this.getAvatarDataForHex(avatar));
                    })
                    .verifyComplete();
            }
        );
    }

    @Test
    @Order(4)
    void TestReadUserArchive()
    {
        TEST_USERS.forEach(
            (name) -> {
                Mono<Void> readOneUserArchive
                    = this.userArchiveManager
                          .readUserArchive(name)
                          .doOnSuccess((ignore) ->
                              log.info("Read archive of {} complete!", name)
                          );

                StepVerifier.create(readOneUserArchive)
                            .verifyComplete();
            }
        );

        StepVerifier.create(this.cleanRedis())
                    .consumeNextWith(System.out::println)
                    .verifyComplete();
    }

    @Test
    @Order(5)
    void TestDeleteUserArchive()
    {
        TEST_USERS.forEach(
            (name) -> {
                Mono<Void> deleteOneUserArchive
                    = this.userArchiveManager
                          .deleteUserArchive(name)
                          .doOnSuccess((ignore) ->
                              log.info("Delete archive of {} complete!", name)
                          );

                StepVerifier.create(deleteOneUserArchive)
                            .verifyComplete();
            }
        );
    }

    @Test
    @Order(6)
    void TestRenameUserArchiveDir()
    {
        this.TestCreateNewArchiveForNewUser();

        List<String> renameUser
            = List.of(
                "Jesse_EC", "Peter_Griffin",
                "Lois_Griffin", "Cris_666", "Meg_888"
        );

        Map<String, String> oldAndNewName
            = IntStream.range(0, renameUser.size())
                     .mapToObj((index) ->
                         Map.entry(TEST_USERS.get(index), renameUser.get(index)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Mono<List<String>> renameAvatars
            = Mono.just(oldAndNewName)
                  .map((names) -> {
                          names.forEach((oldName, newName) -> {
                              Mono<Void> renameOneAvatars
                                  = this.userArchiveManager
                                  .renameUserArchiveDir(oldName, newName)
                                  .doOnSuccess((ignore) ->
                                      log.info("Modify archive name {} -> {}", oldName, newName)
                                  );

                              StepVerifier.create(renameOneAvatars).verifyComplete();
                          });
                          return names.values().stream().toList();
                      });

        StepVerifier.create(renameAvatars)
                    .expectNextMatches((newUser) -> newUser.size() == renameUser.size())
                    .verifyComplete();

        this.cleanAllArchiveAfterTest();
    }
}
