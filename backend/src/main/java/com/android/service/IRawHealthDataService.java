package com.android.service;

import com.android.entity.RawHealthData;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 原始健康数据服务类
 * </p>
 *
 * @author sjt
 * @since 2026-01-12
 */
public interface IRawHealthDataService extends IService<RawHealthData> {
    
    /**
     * 上传报告（拍报告接口）
     * 用户ID通过ThreadLocal获取，data_type固定为REPORT，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的报告信息
     */
    RawHealthData uploadReport(MultipartFile file);
    
    /**
     * 上传就诊记录（拍照上传）
     * 用户ID通过ThreadLocal获取，data_type固定为MEDICAL_RECORD，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的就诊记录信息
     */
    RawHealthData uploadMedicalRecordImage(MultipartFile file);
    
    /**
     * 上传就诊记录（文件上传）
     * 用户ID通过ThreadLocal获取，data_type固定为MEDICAL_RECORD，format_type为PDF
     * 
     * @param file PDF文件
     * @return 保存的就诊记录信息
     */
    RawHealthData uploadMedicalRecordFile(MultipartFile file);
    
    /**
     * 上传皮肤照片（拍皮肤接口）
     * 用户ID通过ThreadLocal获取，data_type固定为SKIN，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的皮肤照片信息
     */
    RawHealthData uploadSkin(MultipartFile file);
    
    /**
     * 上传检查检验（拍报告接口）
     * 用户ID通过ThreadLocal获取，data_type固定为LAB，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的检查检验信息
     */
    RawHealthData uploadLab(MultipartFile file);
    
    /**
     * 上传情绪检测（拍报告接口）
     * 用户ID通过ThreadLocal获取，data_type固定为EMOTION，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的情绪检测信息
     */
    RawHealthData uploadEmotion(MultipartFile file);
    
    /**
     * 上传基因检测（拍报告接口）
     * 用户ID通过ThreadLocal获取，data_type固定为GENETIC，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的基因检测信息
     */
    RawHealthData uploadGenetic(MultipartFile file);
    
    /**
     * 上传拍三餐（拍三餐接口）
     * 用户ID通过ThreadLocal获取，data_type固定为MEAL，format_type为IMAGE
     * 
     * @param file 图片文件
     * @return 保存的拍三餐信息
     */
    RawHealthData uploadMeal(MultipartFile file);
    
    /**
     * 根据用户ID批量查询该用户的所有最新健康数据（按上传时间排序）
     * 用于传递给智能体模块，生成健康档案
     * 
     * @param userId 用户ID
     * @return 用户的所有最新健康数据列表（包含raw-data的Base64编码）
     */
    java.util.List<java.util.Map<String, Object>> getAllLatestHealthDataByUserId(Long userId);
    
    /**
     * 根据数据ID获取健康数据（用于下载）
     * 
     * @param id 数据ID
     * @return 健康数据实体（包含raw-data二进制数据）
     */
    RawHealthData getHealthDataById(Long id);
    
    /**
     * 获取用户及其管理的家人的所有原始数据列表（包括所属人名字）
     * 
     * @param userId 用户ID
     * @return 原始数据列表，每个数据包含所属人名字
     */
    java.util.List<java.util.Map<String, Object>> getAllRawHealthDataWithOwnerName(Integer userId);
}
