package com.android.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户昵称 */
    private String nickname;
    /** 头像 URL */
    private String avatarUrl;
    /** 手机号 */
    private String phone;
    /** 原始密码（提交后会加密存储） */
    private String password;
    /** 性别：0-未知，1-男，2-女 */
    private Integer gender;
    /** 状态：1-正常，0-禁用 */
    private Integer status;
}

