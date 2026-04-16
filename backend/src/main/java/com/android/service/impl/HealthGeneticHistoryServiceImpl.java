package com.android.service.impl;

import com.android.entity.HealthGeneticHistory;
import com.android.mapper.HealthGeneticHistoryMapper;
import com.android.service.IHealthGeneticHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 遗传病史表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthGeneticHistoryServiceImpl extends ServiceImpl<HealthGeneticHistoryMapper, HealthGeneticHistory> implements IHealthGeneticHistoryService {

}
