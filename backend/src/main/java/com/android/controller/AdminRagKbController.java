package com.android.controller;

import com.android.common.Result;
import com.android.dto.AdminRagDrugRowVO;
import com.android.dto.AdminRagInstructionRowVO;
import com.android.dto.PageResultVO;
import com.android.service.IRagKbAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端：ai-doctor-rag 向量库对应的结构化数据集（分表）
 */
@RestController
@RequestMapping("/admin/rag-kb")
@RequiredArgsConstructor
public class AdminRagKbController {

    private final IRagKbAdminService ragKbAdminService;

    @GetMapping("/qa/page")
    public Result<PageResultVO<AdminRagInstructionRowVO>> pageQa(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(ragKbAdminService.pageQa(pageNum, pageSize, keyword));
    }

    @GetMapping("/liver-cancer/page")
    public Result<PageResultVO<AdminRagInstructionRowVO>> pageLiver(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(ragKbAdminService.pageLiverCancer(pageNum, pageSize, keyword));
    }

    @GetMapping("/llama/page")
    public Result<PageResultVO<AdminRagInstructionRowVO>> pageLlama(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(ragKbAdminService.pageLlama(pageNum, pageSize, keyword));
    }

    @GetMapping("/drug/page")
    public Result<PageResultVO<AdminRagDrugRowVO>> pageDrug(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(ragKbAdminService.pageDrug(pageNum, pageSize, keyword));
    }

    @GetMapping("/english/page")
    public Result<PageResultVO<AdminRagInstructionRowVO>> pageEnglish(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(ragKbAdminService.pageEnglish(pageNum, pageSize, keyword));
    }
}
