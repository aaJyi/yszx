package com.android.service.impl;

import com.android.entity.HealthExposureHistory;
import com.android.mapper.HealthExposureHistoryMapper;
import com.android.service.IHealthExposureHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 暴露史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthExposureHistoryServiceImpl extends ServiceImpl<HealthExposureHistoryMapper, HealthExposureHistory> implements IHealthExposureHistoryService {

}
