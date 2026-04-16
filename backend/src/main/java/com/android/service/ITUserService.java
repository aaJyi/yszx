package com.android.service;

import com.android.dto.LoginRequest;
import com.android.dto.LoginResponse;
import com.android.dto.RegisterRequest;
import com.android.dto.RegisterResponse;
import com.android.dto.UserProfileUpdateRequest;
import com.android.dto.UserProfileVO;
import com.android.entity.TUser;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author sjt
 * @since 2026-01-09
 */
public interface ITUserService extends IService<TUser> {

    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户注册
     * 密码在应用层进行MD5加密后再存储
     * 
     * @param request 注册请求
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest request);

    UserProfileVO getUserProfile(Long userId);

    UserProfileVO updateUserProfile(Long userId, UserProfileUpdateRequest request);

    String uploadAvatar(Long userId, MultipartFile file);
}
