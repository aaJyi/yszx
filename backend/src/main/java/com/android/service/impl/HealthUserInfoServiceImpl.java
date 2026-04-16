package com.android.service.impl;

import com.android.entity.HealthUserInfo;
import com.android.mapper.HealthUserInfoMapper;
import com.android.service.IHealthUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 健康档案用户基本信息表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-16
 */
@Service
public class HealthUserInfoServiceImpl extends ServiceImpl<HealthUserInfoMapper, HealthUserInfo> implements IHealthUserInfoService {

}
