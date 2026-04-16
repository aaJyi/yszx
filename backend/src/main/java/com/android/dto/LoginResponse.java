package com.android.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 登录响应DTO
 * 
 * @author sjt
 * @since 2026-01-09
 */
@Data
public class LoginResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * Token（后续可用于JWT等）
     */
    private String token;
}
