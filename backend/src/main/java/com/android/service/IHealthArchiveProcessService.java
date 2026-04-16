package com.android.service;

import com.android.dto.HealthArchiveRequest;
import com.android.entity.HealthArchive;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 健康档案处理服务接口
 * </p>
 *
 * @author sjt
 * @since 2026-01-15
 */
public interface IHealthArchiveProcessService {

    /**
     * 处理并保存健康档案数据
     * 在同一事务中保存健康档案主表和个人健康标识表
     *
     * @param request 健康档案请求对象
     * @param userId 用户ID（可选，如果不提供则从ThreadLocal获取）
     * @param familyMemberId 家人ID（可选，如果提供则为家人创建档案，使用家人的userId）
     * @return 保存后的健康档案对象
     */
    HealthArchive processAndSaveHealthArchive(HealthArchiveRequest request, Integer userId, Integer familyMemberId);

    /**
     * 根据用户ID查询该用户的所有健康档案列表
     * 包括自己创建的以及别人为自己创建的所有健康档案
     *
     * @param userId 用户ID（档案拥有者ID）
     * @return 健康档案列表
     */
    List<HealthArchive> getHealthArchiveListByUserId(Integer userId);

    /**
     * 根据创建者用户ID查询该用户创建的所有健康档案列表（包括自己和家人的）
     *
     * @param creatorUserId 创建者用户ID
     * @return 健康档案列表
     */
    List<HealthArchive> getHealthArchiveListByCreatorId(Integer creatorUserId);

    /**
     * 根据档案ID查询完整的健康档案信息（包括所有关联表）
     *
     * @param archiveId 档案ID
     * @return 完整的健康档案信息
     */
    Map<String, Object> getHealthArchiveDetailById(Integer archiveId);

    /**
     * 生成健康档案PDF
     *
     * @param archiveData 健康档案数据
     * @return PDF文件的字节数组
     */
    byte[] generateHealthArchivePdf(Map<String, Object> archiveData) throws Exception;

    /**
     * 查询登录用户最新健康档案中的慢病列表
     *
     * @param userId 用户ID（可选，如果不提供则从ThreadLocal获取）
     * @return 慢病列表
     */
    List<String> getChronicDiseaseList(Integer userId);

    /**
     * 更新健康档案数据
     * 更新健康档案主表及所有关联表的数据
     *
     * @param archiveId 档案ID
     * @param request 健康档案请求对象
     * @param userId 用户ID（可选，如果不提供则从ThreadLocal获取）
     * @return 更新后的健康档案对象
     */
    HealthArchive updateHealthArchive(Integer archiveId, HealthArchiveRequest request, Integer userId) throws JsonProcessingException;

    /**
     * 根据健康档案ID更新健康档案主表信息
     *
     * @param healthArchive 健康档案对象（必须包含archiveId）
     * @return 是否更新成功
     */
    boolean updateArchiveById(HealthArchive healthArchive);

    /**
     * 获取健康档案统计数据
     * 包括：总记录数（我的+我管理的家人的）、体检报告数、监测数据数
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    Map<String, Integer> getHealthArchiveStats(Integer userId);

    /**
     * 生成最新健康档案
     * 1. 查询用户最新的健康档案
     * 2. 复制最新档案的基本信息（用户基本信息）
     * 3. 创建新的健康档案（使用当前年份）
     * 4. 调用智能体接口生成其他信息
     *
     * @param userId 用户ID
     * @return 新创建的健康档案
     */
    HealthArchive generateLatestHealthArchive(Integer userId);
}
