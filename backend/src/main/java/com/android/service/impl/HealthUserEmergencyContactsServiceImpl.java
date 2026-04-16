package com.android.service.impl;

import com.android.entity.HealthUserEmergencyContacts;
import com.android.mapper.HealthUserEmergencyContactsMapper;
import com.android.service.IHealthUserEmergencyContactsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 健康档案用户紧急联系人表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthUserEmergencyContactsServiceImpl extends ServiceImpl<HealthUserEmergencyContactsMapper, HealthUserEmergencyContacts> implements IHealthUserEmergencyContactsService {

}
