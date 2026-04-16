package com.android.controller;

import com.android.common.Result;
import com.android.dto.HealthCrawlKeywordSaveDTO;
import com.android.dto.HealthCrawlKeywordVO;
import com.android.service.IHealthCrawlKeywordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端：爬取种子关键词（智能导航）
 */
@RestController
@RequestMapping("/admin/crawl-keywords")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminHealthCrawlKeywordController {

    private final IHealthCrawlKeywordService healthCrawlKeywordService;

    @GetMapping
    public Result<List<HealthCrawlKeywordVO>> list() {
        try {
            return Result.success(healthCrawlKeywordService.listAll());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Long> create(@RequestBody HealthCrawlKeywordSaveDTO body) {
        try {
            return Result.success(healthCrawlKeywordService.create(body));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody HealthCrawlKeywordSaveDTO body) {
        try {
            healthCrawlKeywordService.update(id, body);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            healthCrawlKeywordService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
