package com.android.controller;

import com.android.common.Result;
import com.android.dto.RagKbDrugMutateDTO;
import com.android.dto.RagKbEnDocMutateDTO;
import com.android.dto.RagKbInstructionMutateDTO;
import com.android.dto.RagVectorRebuildDTO;
import com.android.service.IRagKbDatasetAdminService;
import com.android.service.RagVectorRebuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 管理端：RAG 知识库数据集 CRUD + 触发向量重建
 */
@RestController
@RequestMapping("/admin/rag-kb")
@RequiredArgsConstructor
public class AdminRagKbManageController {

    private static final Set<String> DATASETS = Set.of("qa", "liver", "llama", "drug", "english", "all");

    private final IRagKbDatasetAdminService ragKbDatasetAdminService;
    private final RagVectorRebuildService ragVectorRebuildService;

    @PostMapping("/rebuild-vector")
    public Result<String> rebuildVector(@RequestBody RagVectorRebuildDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDataset())) {
            return Result.badRequest("dataset 不能为空");
        }
        String ds = dto.getDataset().trim();
        if (!DATASETS.contains(ds)) {
            return Result.badRequest("dataset 必须是 qa|liver|llama|drug|english|all");
        }
        ragVectorRebuildService.rebuildAsync(ds);
        return Result.success(
                "向量重建任务已在后台启动（耗时与数据量有关）。完成后将尝试通知 RAG 服务热加载；若失败请重启 ai-doctor-rag。",
                null);
    }

    // ---------- QA ----------
    @PostMapping("/qa")
    public Result<Long> createQa(@RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        return Result.success(ragKbDatasetAdminService.createQa(dto));
    }

    @PutMapping("/qa/{id}")
    public Result<Void> updateQa(@PathVariable Long id, @RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        ragKbDatasetAdminService.updateQa(id, dto);
        return Result.success();
    }

    @DeleteMapping("/qa/{id}")
    public Result<Void> deleteQa(@PathVariable Long id) {
        ragKbDatasetAdminService.deleteQa(id);
        return Result.success();
    }

    // ---------- 肝癌 ----------
    @PostMapping("/liver-cancer")
    public Result<Long> createLiver(@RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        return Result.success(ragKbDatasetAdminService.createLiver(dto));
    }

    @PutMapping("/liver-cancer/{id}")
    public Result<Void> updateLiver(@PathVariable Long id, @RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        ragKbDatasetAdminService.updateLiver(id, dto);
        return Result.success();
    }

    @DeleteMapping("/liver-cancer/{id}")
    public Result<Void> deleteLiver(@PathVariable Long id) {
        ragKbDatasetAdminService.deleteLiver(id);
        return Result.success();
    }

    // ---------- Llama ----------
    @PostMapping("/llama")
    public Result<Long> createLlama(@RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        return Result.success(ragKbDatasetAdminService.createLlama(dto));
    }

    @PutMapping("/llama/{id}")
    public Result<Void> updateLlama(@PathVariable Long id, @RequestBody RagKbInstructionMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInstruction())) {
            return Result.badRequest("instruction 不能为空");
        }
        ragKbDatasetAdminService.updateLlama(id, dto);
        return Result.success();
    }

    @DeleteMapping("/llama/{id}")
    public Result<Void> deleteLlama(@PathVariable Long id) {
        ragKbDatasetAdminService.deleteLlama(id);
        return Result.success();
    }

    // ---------- 药品 ----------
    @PostMapping("/drug")
    public Result<Long> createDrug(@RequestBody RagKbDrugMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDrugName())) {
            return Result.badRequest("drugName 不能为空");
        }
        return Result.success(ragKbDatasetAdminService.createDrug(dto));
    }

    @PutMapping("/drug/{id}")
    public Result<Void> updateDrug(@PathVariable Long id, @RequestBody RagKbDrugMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDrugName())) {
            return Result.badRequest("drugName 不能为空");
        }
        ragKbDatasetAdminService.updateDrug(id, dto);
        return Result.success();
    }

    @DeleteMapping("/drug/{id}")
    public Result<Void> deleteDrug(@PathVariable Long id) {
        ragKbDatasetAdminService.deleteDrug(id);
        return Result.success();
    }

    // ---------- 英文 ----------
    @PostMapping("/english")
    public Result<Long> createEnglish(@RequestBody RagKbEnDocMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInput())) {
            return Result.badRequest("input 不能为空");
        }
        return Result.success(ragKbDatasetAdminService.createEnglish(dto));
    }

    @PutMapping("/english/{id}")
    public Result<Void> updateEnglish(@PathVariable Long id, @RequestBody RagKbEnDocMutateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getInput())) {
            return Result.badRequest("input 不能为空");
        }
        ragKbDatasetAdminService.updateEnglish(id, dto);
        return Result.success();
    }

    @DeleteMapping("/english/{id}")
    public Result<Void> deleteEnglish(@PathVariable Long id) {
        ragKbDatasetAdminService.deleteEnglish(id);
        return Result.success();
    }
}
