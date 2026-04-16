package com.android.dto;

import lombok.Data;
import java.io.Serializable;

/**
 * 注册请求DTO
 * 
 * @author sjt
 * @since 2026-01-09
 */
@Data
public class RegisterRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 密码（原始密码，应用层会进行MD5加密）
     */
    private String password;
    
    /**
     * 用户昵称
     */
    private String nickname;
    
    /**
     * 小程序用户openid（可选）
     */
    private String openid;
}
