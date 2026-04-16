package com.android.service;

import com.android.dto.ClubDivisionAdminOverviewVO;
import com.android.dto.ClubDivisionImgManifestVO;
import com.android.dto.ClubDivisionMemberAdminVO;

import java.math.BigDecimal;
import java.util.List;

public interface IClubDivisionService {

    /**
     * 从 user_inferred_disease 聚合用户疾病集，调用 club-division/run_ncss_json.py 并落库。
     */
    void recompute(BigDecimal similarityThreshold) throws Exception;

    ClubDivisionAdminOverviewVO getLatestOverview();

    List<ClubDivisionMemberAdminVO> listMembers(Long snapshotId);

    /** club-division/img 下 PNG 清单，供管理端轮播 */
    ClubDivisionImgManifestVO getImgManifest();
}
