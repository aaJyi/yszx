package com.android.service.impl;

import com.android.entity.HealthDisabilityStatus;
import com.android.mapper.HealthDisabilityStatusMapper;
import com.android.service.IHealthDisabilityStatusService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 残疾情况表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthDisabilityStatusServiceImpl extends ServiceImpl<HealthDisabilityStatusMapper, HealthDisabilityStatus> implements IHealthDisabilityStatusService {

}
