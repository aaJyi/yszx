package com.android.service.impl;

import com.android.dto.AdminUserRowVO;
import com.android.dto.PageResultVO;
import com.android.entity.TUser;
import com.android.service.IAdminUserQueryService;
import com.android.service.ITUserService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserQueryServiceImpl implements IAdminUserQueryService {

    private final ITUserService tUserService;

    @Override
    public PageResultVO<AdminUserRowVO> pageUsers(long pageNum, long pageSize) {
        Page<TUser> p = new Page<>(Math.max(1, pageNum), Math.min(100, Math.max(1, pageSize)));
        Page<TUser> page = tUserService.page(p, Wrappers.<TUser>lambdaQuery().orderByDesc(TUser::getId));
        List<AdminUserRowVO> rows = page.getRecords().stream().map(this::toRow).collect(Collectors.toList());
        return new PageResultVO<>(rows, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private AdminUserRowVO toRow(TUser u) {
        Integer gender = u.getGender();
        int st = Boolean.TRUE.equals(u.getStatus()) ? 1 : 0;
        return AdminUserRowVO.builder()
                .userId(u.getId())
                .openid(u.getOpenid())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .phone(u.getPhone())
                .passwordHint(maskPassword(u.getPassword()))
                .gender(gender)
                .genderText(formatGender(gender))
                .status(st)
                .statusText(formatStatus(u.getStatus()))
                .build();
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "—";
        }
        return "已加密存储（" + password.length() + " 位摘要）";
    }

    private String formatGender(Integer gender) {
        if (gender == null || gender == 0) {
            return "未知";
        }
        return gender == 1 ? "男" : "女";
    }

    private String formatStatus(Boolean status) {
        if (status == null) {
            return "未知";
        }
        return Boolean.TRUE.equals(status) ? "正常" : "禁用";
    }
}
