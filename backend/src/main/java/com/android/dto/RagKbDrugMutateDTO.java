package com.android.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RagKbDrugMutateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String drugName;
    private String indicationText;
    private String diseasesLabeled;
}
