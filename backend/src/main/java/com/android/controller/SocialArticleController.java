package com.android.controller;

import com.android.common.Result;
import com.android.dto.SocialArticleItemVO;
import com.android.dto.SocialInferredDiseaseVO;
import com.android.dto.SocialUserIdRequest;
import com.android.service.ISocialArticleFeedService;
import com.android.service.IUserInferredDiseaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序：资讯流、刷新抓取、查看推断疾病
 */
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "社交资讯", description = "推断疾病与头条相关资讯推荐")
public class SocialArticleController {

    private final ISocialArticleFeedService socialArticleFeedService;
    private final IUserInferredDiseaseService userInferredDiseaseService;

    @Operation(summary = "用户资讯列表")
    @GetMapping("/articles/user/{userId}")
    public Result<List<SocialArticleItemVO>> listArticles(@PathVariable Long userId) {
        try {
            return Result.success(socialArticleFeedService.listArticles(userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "推断疾病列表")
    @GetMapping("/inferred-diseases/user/{userId}")
    public Result<List<SocialInferredDiseaseVO>> listInferred(@PathVariable Long userId) {
        try {
            return Result.success(userInferredDiseaseService.listByUser(userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "重新抓取资讯（按当前推断疾病）")
    @PostMapping("/articles/refresh")
    public Result<Map<String, Object>> refresh(@RequestBody SocialUserIdRequest body) {
        try {
            if (body == null || body.getUserId() == null) {
                return Result.badRequest("userId 不能为空");
            }
            int n = socialArticleFeedService.refreshArticles(body.getUserId());
            Map<String, Object> data = new HashMap<>();
            data.put("inserted", n);
            String msg = n > 0
                    ? "已写入知识库 " + n + " 条新资讯（按 URL 去重）"
                    : "无新写入：若无推断疾病请先做分析；若有疾病则可能本轮结果均已存在或来源暂不可用";
            return Result.success(msg, data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error("刷新失败: " + e.getMessage());
        }
    }
}
