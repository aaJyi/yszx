package com.android.service;

import com.android.dto.HealthArchiveRequest;
import com.android.entity.HealthArchive;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 健康档案主表 服务类
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
public interface IHealthArchiveService extends IService<HealthArchive> {

    /**
     * 新增健康档案（包含个人健康标识）
     * 两个表在同一个事务中保存
     *
     * @param request 健康档案请求对象
     * @param userId 用户ID（可选，如果不提供则从ThreadLocal获取）
     * @return 保存后的健康档案对象
     */
    HealthArchive addHealthArchive(HealthArchiveRequest request, Integer userId);
}
