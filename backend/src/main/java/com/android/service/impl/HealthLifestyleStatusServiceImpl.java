package com.android.service.impl;

import com.android.entity.HealthLifestyleStatus;
import com.android.mapper.HealthLifestyleStatusMapper;
import com.android.service.IHealthLifestyleStatusService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 健康档案-日常生活方式与行为状况表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-20
 */
@Service
public class HealthLifestyleStatusServiceImpl extends ServiceImpl<HealthLifestyleStatusMapper, HealthLifestyleStatus> implements IHealthLifestyleStatusService {

}
