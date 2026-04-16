package com.android.service;

import com.android.dto.RagKbDrugMutateDTO;
import com.android.dto.RagKbEnDocMutateDTO;
import com.android.dto.RagKbInstructionMutateDTO;

public interface IRagKbDatasetAdminService {

    Long createQa(RagKbInstructionMutateDTO dto);

    void updateQa(Long id, RagKbInstructionMutateDTO dto);

    void deleteQa(Long id);

    Long createLiver(RagKbInstructionMutateDTO dto);

    void updateLiver(Long id, RagKbInstructionMutateDTO dto);

    void deleteLiver(Long id);

    Long createLlama(RagKbInstructionMutateDTO dto);

    void updateLlama(Long id, RagKbInstructionMutateDTO dto);

    void deleteLlama(Long id);

    Long createDrug(RagKbDrugMutateDTO dto);

    void updateDrug(Long id, RagKbDrugMutateDTO dto);

    void deleteDrug(Long id);

    Long createEnglish(RagKbEnDocMutateDTO dto);

    void updateEnglish(Long id, RagKbEnDocMutateDTO dto);

    void deleteEnglish(Long id);
}
