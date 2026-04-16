package com.android.controller;

import com.android.common.Result;
import com.android.dto.SocialAgentDiseasesRequest;
import com.android.dto.SocialUserIdRequest;
import com.android.service.ISocialArticleFeedService;
import com.android.service.IUserInferredDiseaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 智能体服务端调用：写入推断疾病、触发资讯抓取
 */
@RestController
@RequestMapping("/social/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "社交资讯-智能体", description = "智能体写入疾病并抓取头条相关文章")
public class SocialAgentController {

    private final IUserInferredDiseaseService userInferredDiseaseService;
    private final ISocialArticleFeedService socialArticleFeedService;

    @Operation(summary = "覆盖写入用户推断疾病")
    @PostMapping("/inferred-diseases")
    public Result<Map<String, Object>> saveInferredDiseases(@RequestBody SocialAgentDiseasesRequest body) {
        try {
            if (body == null || body.getUserId() == null) {
                return Result.badRequest("userId 不能为空");
            }
            userInferredDiseaseService.replaceForUser(
                    body.getUserId(),
                    body.getDiseases(),
                    body.getAnalysisId());
            Map<String, Object> data = new HashMap<>();
            data.put("userId", body.getUserId());
            return Result.success("已更新推断疾病", data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "按推断疾病抓取并入库资讯")
    @PostMapping("/crawl-articles")
    public Result<Map<String, Object>> crawlArticles(@RequestBody SocialUserIdRequest body) {
        try {
            if (body == null || body.getUserId() == null) {
                return Result.badRequest("userId 不能为空");
            }
            int n = socialArticleFeedService.refreshArticles(body.getUserId());
            Map<String, Object> data = new HashMap<>();
            data.put("userId", body.getUserId());
            data.put("inserted", n);
            String msg = n > 0 ? "已抓取并写入 " + n + " 条" : "未写入（无推断疾病或检索无结果）";
            return Result.success(msg, data);
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error("抓取失败: " + e.getMessage());
        }
    }
}
