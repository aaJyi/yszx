package com.android.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RagKbEnDocMutateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String input;
    private String output;
}
