package com.android.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * dataset: qa | liver | llama | drug | english | all
 */
@Data
public class RagVectorRebuildDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String dataset;
}
