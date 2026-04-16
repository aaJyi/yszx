package com.android.service;

import com.android.entity.FamilyMember;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 家人信息表 服务类
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
public interface IFamilyMemberService extends IService<FamilyMember> {

    /**
     * 根据创建者用户ID查询家人列表
     * 
     * @param ownerUserId 创建者用户ID
     * @return 家人列表
     */
    List<FamilyMember> getFamilyMembersByOwnerId(Integer ownerUserId);

    /**
     * 验证家人密码并添加家人
     * 
     * @param phone 家人手机号
     * @param password 家人密码
     * @param ownerUserId 创建者用户ID
     * @param relation 关系（可选）
     * @return 添加的家人信息
     */
    FamilyMember addFamilyMemberWithPassword(String phone, String password, Integer ownerUserId, String relation);
}
