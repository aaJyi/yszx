package com.android.controller;

import com.android.common.Result;
import com.android.dto.LoginRequest;
import com.android.dto.LoginResponse;
import com.android.dto.MiniProfileStatsVO;
import com.android.dto.RegisterRequest;
import com.android.dto.RegisterResponse;
import com.android.dto.UserAvatarUploadVO;
import com.android.dto.UserProfileUpdateRequest;
import com.android.dto.UserProfileVO;
import com.android.service.IMiniProfileStatsService;
import com.android.service.ITUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-09
 */
@Slf4j
@RestController
@RequestMapping("/t-user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TUserController {

    private final ITUserService userService;
    private final IMiniProfileStatsService miniProfileStatsService;

    /**
     * 用户登录接口
     * 
     * @param request 登录请求
     * @return 登录响应
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        log.info("用户登录请求，手机号：{}", request.getPhone());
        return userService.login(request);
    }

    /**
     * 用户注册接口
     * 密码在应用层进行MD5加密后再存储到数据库
     * 
     * @param request 注册请求
     * @return 注册响应
     */
    @PostMapping("/register")
    public RegisterResponse register(@RequestBody RegisterRequest request) {
        log.info("用户注册请求，手机号：{}", request.getPhone());
        return userService.register(request);
    }

    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile(
            @RequestHeader(value = "userId", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            return Result.badRequest("用户ID不能为空");
        }
        try {
            return Result.success(userService.getUserProfile(Long.parseLong(userId.trim())));
        } catch (NumberFormatException e) {
            return Result.badRequest("用户ID格式错误");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public Result<UserProfileVO> updateProfile(
            @RequestHeader(value = "userId", required = false) String userId,
            @RequestBody UserProfileUpdateRequest request
    ) {
        if (userId == null || userId.isBlank()) {
            return Result.badRequest("用户ID不能为空");
        }
        try {
            return Result.success(userService.updateUserProfile(Long.parseLong(userId.trim()), request));
        } catch (NumberFormatException e) {
            return Result.badRequest("用户ID格式错误");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/avatar")
    public Result<UserAvatarUploadVO> uploadAvatar(
            @RequestHeader(value = "userId", required = false) String userId,
            @RequestParam("file") MultipartFile file
    ) {
        if (userId == null || userId.isBlank()) {
            return Result.badRequest("用户ID不能为空");
        }
        try {
            String avatarUrl = userService.uploadAvatar(Long.parseLong(userId.trim()), file);
            return Result.success(new UserAvatarUploadVO(avatarUrl));
        } catch (NumberFormatException e) {
            return Result.badRequest("用户ID格式错误");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 小程序「我的」页统计：健康记录数、家人管理数、健康报告数（均基于当前登录用户）
     *
     * @param userId 用户ID（请求头 userId，与小程序其它接口一致）
     */
    @GetMapping("/profile-stats")
    public Result<MiniProfileStatsVO> profileStats(
            @RequestHeader(value = "userId", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return Result.badRequest("用户ID不能为空");
        }
        try {
            long id = Long.parseLong(userId.trim());
            return Result.success(miniProfileStatsService.getProfileStats(id));
        } catch (NumberFormatException e) {
            return Result.badRequest("用户ID格式错误");
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        }
    }
}
