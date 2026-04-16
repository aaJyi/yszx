package com.android.service;

import com.android.dto.HealthArticleDraft;

import java.util.List;

/**
 * 按关键词拉取健康/医疗相关资讯（由本机 Python RSS 服务提供数据，Java 仅 HTTP 调用）。
 */
public interface IHealthArticleFetchService {

    List<HealthArticleDraft> fetchByKeyword(String keyword, int limit);
}
