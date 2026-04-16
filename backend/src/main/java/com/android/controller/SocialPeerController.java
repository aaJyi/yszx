package com.android.controller;

import com.android.common.Result;
import com.android.dto.SocialFollowBody;
import com.android.dto.SocialPeerMessageBody;
import com.android.dto.SocialPeerMessageVO;
import com.android.dto.SocialPeerUserVO;
import com.android.dto.SocialPreferenceBody;
import com.android.entity.UserSocialPreference;
import com.android.service.ISocialPeerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/social/peer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "病友社交", description = "推荐、关注、私信")
public class SocialPeerController {

    private final ISocialPeerService socialPeerService;

    @Operation(summary = "推荐病友（NCSS 同社团优先，否则按疾病 Jaccard）")
    @GetMapping("/recommendations/user/{userId}")
    public Result<Map<String, Object>> recommendations(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        try {
            return Result.success(socialPeerService.listRecommendations(userId, page, pageSize));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "读取社交授权与隐私偏好")
    @GetMapping("/preference/user/{userId}")
    public Result<UserSocialPreference> preference(@PathVariable Long userId) {
        try {
            return Result.success(socialPeerService.getOrCreatePreference(userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "保存社交授权与隐私偏好")
    @PostMapping("/preference")
    public Result<Void> savePreference(@RequestBody SocialPreferenceBody body) {
        try {
            if (body == null || body.getUserId() == null) {
                return Result.badRequest("userId不能为空");
            }
            UserSocialPreference p = new UserSocialPreference();
            p.setUserId(body.getUserId());
            p.setAllowRecommend(body.getAllowRecommend());
            p.setAllowDiseaseBasedMatch(body.getAllowDiseaseBasedMatch());
            p.setActiveTimeWindow(body.getActiveTimeWindow());
            p.setReplyStyle(body.getReplyStyle());
            p.setBlockedUserIds(body.getBlockedUserIds());
            socialPeerService.savePreference(p);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "关注")
    @PostMapping("/follow")
    public Result<Void> follow(@RequestBody SocialFollowBody body) {
        try {
            if (body == null || body.getFollowerUserId() == null || body.getFolloweeUserId() == null) {
                return Result.badRequest("参数不完整");
            }
            socialPeerService.follow(body.getFollowerUserId(), body.getFolloweeUserId());
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "取消关注")
    @PostMapping("/follow/cancel")
    public Result<Void> unfollow(@RequestBody SocialFollowBody body) {
        try {
            if (body == null || body.getFollowerUserId() == null || body.getFolloweeUserId() == null) {
                return Result.badRequest("参数不完整");
            }
            socialPeerService.unfollow(body.getFollowerUserId(), body.getFolloweeUserId());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "互相关注的病友列表（可发起聊天）")
    @GetMapping("/partners/user/{userId}")
    public Result<List<SocialPeerUserVO>> partners(@PathVariable Long userId) {
        try {
            return Result.success(socialPeerService.listMutualPartners(userId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "会话消息")
    @GetMapping("/messages/conversation")
    public Result<List<SocialPeerMessageVO>> conversation(
            @RequestParam Long userId,
            @RequestParam Long peerId,
            @RequestParam(required = false) Long sinceId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        try {
            return Result.success(socialPeerService.listConversation(userId, peerId, sinceId, limit));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "发送消息（需互关）")
    @PostMapping("/messages")
    public Result<Void> send(@RequestBody SocialPeerMessageBody body) {
        try {
            if (body == null || body.getFromUserId() == null || body.getToUserId() == null) {
                return Result.badRequest("参数不完整");
            }
            socialPeerService.sendMessage(body.getFromUserId(), body.getToUserId(), body.getContent());
            return Result.success();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
