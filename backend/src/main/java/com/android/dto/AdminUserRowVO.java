package com.android.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理端：用户列表行
 */
@Data
@Builder
public class AdminUserRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务主键（查询健康数据等接口仍用数值 userId）
     */
    private Long userId;
    /** 展示用用户标识：小程序 openid */
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String phone;
    /** 密码为加密存储，仅作提示 */
    private String passwordHint;
    /** 0-未知 1-男 2-女 */
    private Integer gender;
    private String genderText;
    /** 1-正常 0-禁用 */
    private Integer status;
    private String statusText;
}
