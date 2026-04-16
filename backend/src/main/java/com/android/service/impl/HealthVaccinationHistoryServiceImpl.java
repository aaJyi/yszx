package com.android.service.impl;

import com.android.entity.HealthVaccinationHistory;
import com.android.mapper.HealthVaccinationHistoryMapper;
import com.android.service.IHealthVaccinationHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 预防接种史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthVaccinationHistoryServiceImpl extends ServiceImpl<HealthVaccinationHistoryMapper, HealthVaccinationHistory> implements IHealthVaccinationHistoryService {

}
