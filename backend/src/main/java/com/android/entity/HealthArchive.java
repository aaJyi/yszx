package com.android.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 健康档案主表
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("health_archive")
public class HealthArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 档案ID
     */
    @TableId(value = "archive_id", type = IdType.AUTO)
    private Integer archiveId;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 档案姓名
     */
    private String userName;

    /**
     * 档案编号
     */
    private String archiveNo;

    /**
     * 档案名称
     */
    private String archiveName;

    /**
     * 档案日期
     */
    private LocalDate archiveDate;

    /**
     * 档案年份
     */
    private Integer archiveYear;

    /**
     * 档案管理机构名称
     */
    private String archiveManagerName;

    /**
     * 档案管理机构电话
     */
    private String archiveManagerPhone;
}
