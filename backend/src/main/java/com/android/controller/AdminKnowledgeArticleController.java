package com.android.controller;

import com.android.common.Result;
import com.android.dto.AdminKnowledgeArticleRowVO;
import com.android.dto.PageResultVO;
import com.android.service.IHealthKnowledgeArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：健康资讯知识库分页
 */
@RestController
@RequestMapping("/admin/knowledge-articles")
@RequiredArgsConstructor
public class AdminKnowledgeArticleController {

    private final IHealthKnowledgeArticleService healthKnowledgeArticleService;

    @GetMapping("/page")
    public Result<PageResultVO<AdminKnowledgeArticleRowVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(healthKnowledgeArticleService.pageAdmin(pageNum, pageSize, keyword));
    }
}
