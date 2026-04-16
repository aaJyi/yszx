package com.android.service;

import com.android.dto.SocialDiseaseItemDTO;
import com.android.dto.SocialInferredDiseaseVO;
import com.android.dto.AdminInferredDiseaseRowVO;

import java.util.List;

public interface IUserInferredDiseaseService {

    void replaceForUser(Long userId, List<SocialDiseaseItemDTO> diseases, Long analysisId);

    List<SocialInferredDiseaseVO> listByUser(Long userId);

    List<AdminInferredDiseaseRowVO> listRecentForAdmin(Integer limit);
}
