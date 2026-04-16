package com.android.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserProfileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String phone;
    /**
     * 注意：历史密码是不可逆摘要，无法解密回显；这里只用于前端输入新密码
     */
    private String password;
    private Integer gender;
    private String genderText;
    private Integer status;
    private String statusText;
}

