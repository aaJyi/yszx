package com.android.service.impl;

import com.android.entity.HealthProfileTags;
import com.android.mapper.HealthProfileTagsMapper;
import com.android.service.IHealthProfileTagsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 个人健康标识表 服务实现类
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Service
public class HealthProfileTagsServiceImpl extends ServiceImpl<HealthProfileTagsMapper, HealthProfileTags> implements IHealthProfileTagsService {

}
