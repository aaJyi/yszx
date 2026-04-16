package com.android.controller;

import com.android.common.Result;
import com.android.dto.AdminUserRowVO;
import com.android.dto.PageResultVO;
import com.android.dto.UserProfileUpdateRequest;
import com.android.dto.UserProfileVO;
import com.android.service.IAdminUserQueryService;
import com.android.service.ITUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：用户列表
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IAdminUserQueryService adminUserQueryService;
    private final ITUserService userService;

    @GetMapping("/page")
    public Result<PageResultVO<AdminUserRowVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(adminUserQueryService.pageUsers(pageNum, pageSize));
    }

    /**
     * 管理端修改用户资料（昵称、手机、性别、状态等），与小程序 profile 更新共用同一套校验逻辑。
     */
    @PutMapping("/{userId}")
    public Result<UserProfileVO> updateUser(
            @PathVariable("userId") Long userId,
            @RequestBody UserProfileUpdateRequest request) {
        if (userId == null) {
            return Result.badRequest("用户ID不能为空");
        }
        try {
            return Result.success(userService.updateUserProfile(userId, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
