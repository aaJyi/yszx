package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("rag_kb_en_doc")
public class RagKbEnDoc implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 英文问句（来自 docs.csv 的 input 列） */
    private String input;

    /** 英文回答（来自 docs.csv 的 output 列） */
    private String output;

    private LocalDateTime createdAt;
}
