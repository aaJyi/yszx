package com.android.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 门诊时间项
 */
@Data
@ApiModel(value = "门诊时间项", description = "医生门诊时间安排单项")
public class OutpatientScheduleItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("日期，如：周一至周五、周一")
    private String day;

    @ApiModelProperty("时段，如：上午、下午、全天")
    private String period;

    @ApiModelProperty("地点，如：门诊部2楼儿童诊区")
    private String location;

    @ApiModelProperty("费用，如：30元")
    private String fee;
}
