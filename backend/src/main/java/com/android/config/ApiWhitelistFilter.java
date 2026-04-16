package com.android.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 接口白名单过滤器：
 * 仅放行小程序当前使用到的后端接口，其余请求直接返回 404。
 *
 * 注意：这里是“运行期禁用”，不物理删除代码文件，便于回滚与排障。
 */
public class ApiWhitelistFilter implements Filter {

    private static final Pattern[] WHITELIST_PATTERNS = new Pattern[] {
            // 登录/注册/用户资料
            Pattern.compile("^/t-user/(login|register|profile-stats|profile|avatar)$"),

            // 管理端控制台统计（PC）
            Pattern.compile("^/admin/dashboard/overview$"),
            Pattern.compile("^/admin/dashboard/inferred-diseases$"),
            Pattern.compile("^/admin/dashboard/dev/.*$"),
            Pattern.compile("^/admin/users/page$"),
            Pattern.compile("^/admin/users/\\d+$"),
            Pattern.compile("^/admin/health-data/overview$"),
            Pattern.compile("^/admin/health-data/records$"),
            Pattern.compile("^/admin/family-graph$"),
            // 病机图谱：器官疾病知识库
            Pattern.compile("^/admin/organ-diseases$"),
            Pattern.compile("^/admin/knowledge-articles/page$"),
            Pattern.compile("^/admin/rag-kb/.*$"),
            Pattern.compile("^/admin/crawl-keywords.*$"),
            Pattern.compile("^/admin/club-division/.*$"),
            // NCSS 导出的 matplotlib 图片（静态资源）
            Pattern.compile("^/club-division-img/.*$"),

            // 健康档案列表/详情/统计/生成/导出
            Pattern.compile("^/health-archive-process/list$"),
            Pattern.compile("^/health-archive-process/stats$"),
            Pattern.compile("^/health-archive-process/detail/\\d+$"),
            Pattern.compile("^/health-archive-process/update/\\d+$"),
            Pattern.compile("^/health-archive-process/generate-latest$"),
            Pattern.compile("^/health-archive-process/basic-info$"),
            Pattern.compile("^/health-archive-process/export/pdf/\\d+$"),
            Pattern.compile("^/health-archive-process/chronic-disease/list$"),
            // 小程序代码里实际使用了：/health-archive-process/process*familyMemberId=xxx（包含 '*' 字符）
            Pattern.compile("^/health-archive-process/process.*$"),

            // 健康档案所有子表编辑（小程序 detail.vue 通过 updateTable/updateTableList 触发）
            Pattern.compile("^/health-[a-z-]+/archive/\\d+$"),

            // AI 健康分析建议（首页展示）
            Pattern.compile("^/health-ai-analysis/user/\\d+/recommendations/latest$"),
            // ai-agent 拉取完整数据、提交健康总结/建议
            Pattern.compile("^/health-ai-analysis/data/user/\\d+$"),
            Pattern.compile("^/health-ai-analysis/submit/user/\\d+$"),
            // 可选查询：最新分析、档案分析列表
            Pattern.compile("^/health-ai-analysis/user/\\d+/latest$"),
            Pattern.compile("^/health-ai-analysis/archive/\\d+$"),

            // 社交资讯：推断疾病 + 头条相关文章
            Pattern.compile("^/social/articles/user/\\d+$"),
            Pattern.compile("^/social/inferred-diseases/user/\\d+$"),
            Pattern.compile("^/social/articles/refresh$"),
            Pattern.compile("^/social/agent/.*$"),
            // 病友社交：推荐、关注、私信
            Pattern.compile("^/social/peer/.*$"),

            // 原始健康数据上传/下载
            Pattern.compile("^/raw-health-data/upload$"),
            Pattern.compile("^/raw-health-data/.+/upload$"),
            Pattern.compile("^/raw-health-data/.+/upload-image$"),
            Pattern.compile("^/raw-health-data/.+/upload-file$"),
            Pattern.compile("^/raw-health-data/list/user/\\d+$"),
            Pattern.compile("^/raw-health-data/user/\\d+$"),
            Pattern.compile("^/raw-health-data/download/\\d+"),

            // 运动识别
            Pattern.compile("^/exercise-train/recognize$"),
            Pattern.compile("^/exercise-train/recognize-video$"),

            // 情绪监测
            Pattern.compile("^/emotion-monitoring/data$"),
            Pattern.compile("^/emotion-monitoring/record$"),

            // 家人管理
            Pattern.compile("^/family-member/list$"),
            Pattern.compile("^/family-member/add$"),
            Pattern.compile("^/family-member/\\d+$"),

            // AI 对话（在线问诊/首页对话/心理咨询）
            Pattern.compile("^/consultation/chat(/.*)?$"),
            Pattern.compile("^/home-chat/ask$"),
            Pattern.compile("^/psychological-chat/chat$"),
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        final String uri = req.getRequestURI();
        if (isWhitelisted(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 非白名单统一返回 404
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":404,\"message\":\"接口未启用\",\"data\":null}");
    }

    private boolean isWhitelisted(String uri) {
        if (uri == null) return false;
        for (Pattern p : WHITELIST_PATTERNS) {
            if (p.matcher(uri).matches()) return true;
        }
        return false;
    }
}

