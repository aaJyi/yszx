package com.android.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 登录请求DTO
 * 
 * @author sjt
 * @since 2026-01-09
 */
@Data
public class LoginRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 密码
     */
    private String password;
}
