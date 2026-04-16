package com.android.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RagKbInstructionMutateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String instruction;
    private String output;
}
