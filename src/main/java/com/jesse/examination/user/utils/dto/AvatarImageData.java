package com.jesse.examination.user.utils.dto;

import lombok.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 为了适应 Java 的泛型，
 * 头像图片字节数组需要稍微包装一下，
 * 毕竟 {@literal Mono<Byte[]>}
 * 相比起 {@literal Mono<AvatarImageData>} 实在是太古怪了。
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AvatarImageData
{
    @Getter
    private byte @NotNull [] avatarBytes;

    @Contract("_ -> new")
    public static @NotNull AvatarImageData
    fromBytes(byte @NotNull [] bytes) {
        return new AvatarImageData(bytes);
    }
}
