package com.android.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

/**
 * 健康档案Excel导入导出服务接口
 *
 * @author sjt
 * @since 2026-01-16
 */
public interface IHealthArchiveExcelService {

    /**
     * 导出健康档案Excel模板
     *
     * @param response HTTP响应
     * @throws Exception 导出异常
     */
    void exportTemplate(HttpServletResponse response) throws Exception;

    /**
     * 导入健康档案Excel数据
     *
     * @param file Excel文件
     * @param userId 用户ID
     * @return 导入结果消息
     * @throws Exception 导入异常
     */
    String importHealthArchive(MultipartFile file, Integer userId) throws Exception;

    /**
     * 导出健康档案数据为Excel
     *
     * @param response HTTP响应
     * @param userId 用户ID
     * @throws Exception 导出异常
     */
    void exportHealthArchive(HttpServletResponse response, Integer userId) throws Exception;
}
