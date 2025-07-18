package com.jesse.examination.user.utils;

import com.jesse.examination.user.utils.dto.AvatarImageData;
import reactor.core.publisher.Mono;

/** 用户存档管理器接口。*/
public interface UserArchiveManager
{
    /** 获取用户默认头像。*/
    Mono<AvatarImageData>
    getDefaultAvatarImage();

    /** 获取指定用户头像数据。*/
    Mono<AvatarImageData>
    getAvatarImageByUserName(String userName);

    /** 设置指定用户头像数据。*/
    Mono<Void>
    setUserAvatarImage(String userName, AvatarImageData avatar);

    /**
     * 用户更改自己用户数据时，若更改了用户名，
     * 则将已经存在的旧存档名 oldUserName 修改成存档名 newUserName。
     */
    Mono<Void>
    renameUserArchiveDir(String oldUserName, String newUserName);

    /** 为新用户创建存档。*/
    Mono<Void>
    createNewArchiveForNewUser(String newUserName);

    /** 用户登录时，读取用户的存档信息。*/
    Mono<Void>
    readUserArchive(String userName);

    /** 用户登出时，保存用户的存档信息。*/
    Mono<Void>
    saveUserArchive(String userName);

    /** 删除用户时，对应的存档、数据库记录也应该一并删除。*/
    Mono<Void>
    deleteUserArchive(String userName);
}
