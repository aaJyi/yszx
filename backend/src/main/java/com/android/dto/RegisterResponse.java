package com.android.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 注册响应DTO
 * 
 * @author sjt
 * @since 2026-01-09
 */
@Data
public class RegisterResponse implements Serializable {
    
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
     * 手机号
     */
    private String phone;
    
    /**
     * 注册成功消息
     */
    private String message;
}
