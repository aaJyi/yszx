package com.android.service;

import com.android.dto.AdminRagDrugRowVO;
import com.android.dto.AdminRagInstructionRowVO;
import com.android.dto.PageResultVO;

public interface IRagKbAdminService {

    PageResultVO<AdminRagInstructionRowVO> pageQa(long pageNum, long pageSize, String keyword);

    PageResultVO<AdminRagInstructionRowVO> pageLiverCancer(long pageNum, long pageSize, String keyword);

    PageResultVO<AdminRagInstructionRowVO> pageLlama(long pageNum, long pageSize, String keyword);

    PageResultVO<AdminRagDrugRowVO> pageDrug(long pageNum, long pageSize, String keyword);

    PageResultVO<AdminRagInstructionRowVO> pageEnglish(long pageNum, long pageSize, String keyword);
}
