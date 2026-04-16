package com.android.service;

import com.android.dto.HealthCrawlKeywordSaveDTO;
import com.android.dto.HealthCrawlKeywordVO;

import java.util.List;

public interface IHealthCrawlKeywordService {

    /** 管理端：全部列表（按排序、id） */
    List<HealthCrawlKeywordVO> listAll();

    Long create(HealthCrawlKeywordSaveDTO dto);

    void update(Long id, HealthCrawlKeywordSaveDTO dto);

    void delete(Long id);

    /** 定时爬取使用：仅返回库中 enabled=1 的关键词（按 sort_order）；无启用项则返回空列表，不使用任何配置文件默认种子。 */
    List<String> resolveSeedKeywordsForCrawl();
}
