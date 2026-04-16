package com.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** RAG 问答类知识行（qa / liver_cancer / llama） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRagInstructionRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String instruction;
    private String output;
    private LocalDateTime createdAt;
}
