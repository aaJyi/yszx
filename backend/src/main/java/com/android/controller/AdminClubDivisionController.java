package com.android.controller;

import com.android.common.Result;
import com.android.dto.ClubDivisionAdminOverviewVO;
import com.android.dto.ClubDivisionImgManifestVO;
import com.android.dto.ClubDivisionMemberAdminVO;
import com.android.dto.ClubDivisionRecomputeBody;
import com.android.service.IClubDivisionService;
import com.android.service.ISocialPeerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/club-division")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminClubDivisionController {

    @Value("${club.division.similarity.threshold.default:0.45}")
    private double defaultSimilarityThreshold;

    private final IClubDivisionService clubDivisionService;
    private final ISocialPeerService socialPeerService;

    @GetMapping("/latest")
    public Result<ClubDivisionAdminOverviewVO> latest() {
        try {
            ClubDivisionAdminOverviewVO vo = clubDivisionService.getLatestOverview();
            return Result.success(vo);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/img-manifest")
    public Result<ClubDivisionImgManifestVO> imgManifest() {
        try {
            return Result.success(clubDivisionService.getImgManifest());
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/members")
    public Result<List<ClubDivisionMemberAdminVO>> members(@RequestParam(required = false) Long snapshotId) {
        try {
            return Result.success(clubDivisionService.listMembers(snapshotId));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @PostMapping("/recompute")
    public Result<Map<String, Object>> recompute(@RequestBody(required = false) ClubDivisionRecomputeBody body) {
        try {
            BigDecimal th = body != null && body.getSimilarityThreshold() != null
                    ? BigDecimal.valueOf(body.getSimilarityThreshold())
                    : BigDecimal.valueOf(defaultSimilarityThreshold);
            clubDivisionService.recompute(th);
            return Result.success("已根据当前推断疾病重新计算社团并落库", Collections.singletonMap("ok", true));
        } catch (Exception e) {
            return Result.error("计算失败: " + e.getMessage());
        }
    }

    @GetMapping("/recommendations")
    public Result<Map<String, Object>> recommendations(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        try {
            return Result.success(socialPeerService.listRecommendations(userId, page, pageSize));
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
