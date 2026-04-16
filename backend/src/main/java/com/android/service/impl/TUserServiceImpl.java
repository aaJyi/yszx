package com.android.service.impl;

import com.android.dto.LoginRequest;
import com.android.dto.LoginResponse;
import com.android.dto.RegisterRequest;
import com.android.dto.RegisterResponse;
import com.android.dto.UserProfileUpdateRequest;
import com.android.dto.UserProfileVO;
import com.android.config.AliyunOssProperties;
import com.android.entity.TUser;
import com.android.mapper.TUserMapper;
import com.android.service.IAliyunOssService;
import com.android.service.ITUserService;
import com.android.util.PasswordUtil;
import com.android.util.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TUserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements ITUserService {

    private final IAliyunOssService aliyunOssService;
    private final AliyunOssProperties aliyunOssProperties;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 参数验证
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            throw new RuntimeException("账号或密码错误，请重新输入");
        }

        // 根据手机号查询用户
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getPhone, request.getPhone());
        TUser user = this.getOne(queryWrapper);

        // 用户不存在或密码错误
        if (user == null) {
            log.warn("登录失败：用户不存在，手机号：{}", request.getPhone());
            throw new RuntimeException("账号或密码错误，请重新输入");
        }

        // 验证密码（数据库中的密码应该是加密后的）
        String encryptedPassword = PasswordUtil.encrypt(request.getPassword());
        if (!encryptedPassword.equals(user.getPassword())) {
            log.warn("登录失败：密码错误，用户ID：{}", user.getId());
            throw new RuntimeException("账号或密码错误，请重新输入");
        }

        // 检查用户状态
        if (user.getStatus() != null && !user.getStatus()) {
            log.warn("登录失败：用户已被禁用，用户ID：{}", user.getId());
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 登录成功，保存用户ID到ThreadLocal
        UserContext.setUserId(user.getId());
        log.info("用户登录成功，用户ID：{}，手机号：{}", user.getId(), user.getPhone());

        // 构建响应
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(toPublicAvatarUrl(user.getAvatarUrl()));
        response.setPhone(user.getPhone());
        response.setToken("token_" + user.getId() + "_" + System.currentTimeMillis());

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest request) {
        // 参数验证
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            throw new RuntimeException("手机号和密码不能为空");
        }

        // 验证手机号格式
        String phone = request.getPhone();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }

        // 验证密码长度
        if (request.getPassword().length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        // 检查手机号是否已注册
        LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TUser::getPhone, phone);
        TUser existingUser = this.getOne(queryWrapper);
        if (existingUser != null) {
            throw new RuntimeException("该手机号已被注册");
        }

        // 创建新用户
        TUser user = new TUser();
        user.setPhone(phone);
        // 密码在应用层进行MD5加密后再存储
        user.setPassword(PasswordUtil.encrypt(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "用户" + phone.substring(7));
        user.setOpenid(StringUtils.hasText(request.getOpenid()) ? request.getOpenid() : "openid_" + UUID.randomUUID().toString().replace("-", ""));
        user.setGender(0);
        user.setStatus(true); // 默认启用
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户（密码已加密）
        boolean saved = this.save(user);
        if (!saved) {
            throw new RuntimeException("注册失败，请稍后重试");
        }

        log.info("用户注册成功，用户ID：{}，手机号：{}", user.getId(), user.getPhone());

        // 构建响应
        RegisterResponse response = new RegisterResponse();
        response.setUserId(user.getId());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setMessage("注册成功");

        return response;
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        TUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (request == null) {
            throw new RuntimeException("请求参数不能为空");
        }

        TUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }
        if (StringUtils.hasText(request.getAvatarUrl())) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (StringUtils.hasText(request.getPhone())) {
            String phone = request.getPhone().trim();
            if (!phone.matches("^1[3-9]\\d{9}$")) {
                throw new RuntimeException("手机号格式不正确");
            }
            LambdaQueryWrapper<TUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TUser::getPhone, phone)
                    .ne(TUser::getId, userId);
            if (this.count(queryWrapper) > 0) {
                throw new RuntimeException("手机号已被其他用户占用");
            }
            user.setPhone(phone);
        }
        if (StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().length() < 6) {
                throw new RuntimeException("密码长度不能少于6位");
            }
            user.setPassword(PasswordUtil.encrypt(request.getPassword()));
        }
        if (request.getGender() != null) {
            Integer gender = request.getGender();
            if (gender < 0 || gender > 2) {
                throw new RuntimeException("性别参数错误");
            }
            user.setGender(gender);
        }
        if (request.getStatus() != null) {
            int status = request.getStatus();
            if (status != 0 && status != 1) {
                throw new RuntimeException("状态参数错误");
            }
            user.setStatus(status == 1);
        }

        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        return toProfileVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        TUser user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String avatarUrl = aliyunOssService.uploadImage(file, "avatars");
        user.setAvatarUrl(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        this.updateById(user);
        return avatarUrl;
    }

    private UserProfileVO toProfileVO(TUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(toPublicAvatarUrl(user.getAvatarUrl()));
        vo.setPhone(user.getPhone());
        vo.setPassword("");
        int gender = user.getGender() == null ? 0 : user.getGender();
        vo.setGender(gender);
        vo.setGenderText(formatGender(gender));
        int status = Boolean.TRUE.equals(user.getStatus()) ? 1 : 0;
        vo.setStatus(status);
        vo.setStatusText(status == 1 ? "正常" : "禁用");
        return vo;
    }

    private String formatGender(Integer gender) {
        if (gender == null || gender == 0) {
            return "未知";
        }
        if (gender == 1) {
            return "男";
        }
        if (gender == 2) {
            return "女";
        }
        return "未知";
    }

    private String toPublicAvatarUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String v = raw.trim();
        if (v.startsWith("http://") || v.startsWith("https://")) {
            return v;
        }

        String key = v.startsWith("/") ? v.substring(1) : v;
        String prefix = aliyunOssProperties.getPublicUrlPrefix();
        if (StringUtils.hasText(prefix)) {
            return prefix.replaceAll("/+$", "") + "/" + key;
        }

        // 兜底：按 bucket + endpoint 拼接公开地址
        if (StringUtils.hasText(aliyunOssProperties.getEndpoint())
                && StringUtils.hasText(aliyunOssProperties.getBucketName())) {
            String endpoint = aliyunOssProperties.getEndpoint()
                    .replace("https://", "")
                    .replace("http://", "");
            return "https://" + aliyunOssProperties.getBucketName() + "." + endpoint + "/" + key;
        }
        return v;
    }
}
