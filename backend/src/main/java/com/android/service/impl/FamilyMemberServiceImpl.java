package com.android.service.impl;

import com.android.entity.FamilyMember;
import com.android.entity.TUser;
import com.android.mapper.FamilyMemberMapper;
import com.android.mapper.TUserMapper;
import com.android.service.IFamilyMemberService;
import com.android.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 家人信息表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Slf4j
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService {

    @Autowired
    private TUserMapper tUserMapper;

    @Override
    public List<FamilyMember> getFamilyMembersByOwnerId(Integer ownerUserId) {
        if (ownerUserId == null) {
            throw new RuntimeException("创建者用户ID不能为空");
        }
        LambdaQueryWrapper<FamilyMember> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FamilyMember::getOwnerUserId, ownerUserId);
        queryWrapper.orderByDesc(FamilyMember::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FamilyMember addFamilyMemberWithPassword(String phone, String password, Integer ownerUserId, String relation) {
        // 参数验证
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(password)) {
            throw new RuntimeException("手机号和密码不能为空");
        }

        if (ownerUserId == null) {
            throw new RuntimeException("创建者用户ID不能为空");
        }

        // 验证手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new RuntimeException("手机号格式不正确");
        }

        // 查询家人是否已注册
        LambdaQueryWrapper<TUser> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.eq(TUser::getPhone, phone);
        TUser familyUser = tUserMapper.selectOne(userQueryWrapper);

        if (familyUser == null) {
            throw new RuntimeException("该手机号未注册，请先注册后再添加为家人");
        }

        // 验证密码
        String encryptedPassword = PasswordUtil.encrypt(password);
        if (!encryptedPassword.equals(familyUser.getPassword())) {
            throw new RuntimeException("密码错误，请重新输入");
        }

        // 检查是否已经是家人
        LambdaQueryWrapper<FamilyMember> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(FamilyMember::getOwnerUserId, ownerUserId);
        checkWrapper.eq(FamilyMember::getPhone, phone);
        FamilyMember existingMember = this.getOne(checkWrapper);

        if (existingMember != null) {
            throw new RuntimeException("该家人已经添加过了");
        }

        // 创建家人记录
        FamilyMember familyMember = new FamilyMember();
        familyMember.setOwnerUserId(ownerUserId);
        familyMember.setPhone(phone);
        familyMember.setFullName(familyUser.getNickname() != null ? familyUser.getNickname() : phone);
        familyMember.setIsRegistered(true);
        familyMember.setRegisteredUserId(familyUser.getId().intValue());
        familyMember.setRelation(StringUtils.hasText(relation) ? relation : "家人");
        familyMember.setAvatarUrl(familyUser.getAvatarUrl());
        Integer userGender = familyUser.getGender();
        familyMember.setGender(userGender != null && userGender == 1);
        familyMember.setCreateTime(LocalDateTime.now());
        familyMember.setUpdateTime(LocalDateTime.now());

        this.save(familyMember);
        log.info("添加家人成功，创建者ID：{}，家人手机号：{}，家人用户ID：{}", ownerUserId, phone, familyUser.getId());

        return familyMember;
    }
}
