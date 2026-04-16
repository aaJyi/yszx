package com.android.controller;

import com.android.common.Result;
import com.android.entity.FamilyMember;
import com.android.service.IFamilyMemberService;
import com.android.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 家人信息表 前端控制器
 * </p>
 *
 * @author sjt
 * @since 2026-01-21
 */
@Slf4j
@RestController
@RequestMapping("/family-member")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "家人管理", description = "家人管理相关接口")
public class FamilyMemberController {

    private final IFamilyMemberService familyMemberService;

    /**
     * 获取当前用户的家人列表
     * 
     * @param userId 用户ID（可选，如果未提供则从ThreadLocal获取）
     * @return 家人列表
     */
    @Operation(summary = "获取家人列表", description = "根据创建者用户ID查询家人列表")
    @GetMapping("/list")
    public Result<List<FamilyMember>> getFamilyMemberList(@RequestParam(required = false) Integer userId) {
        try {
            Integer currentUserId = userId;
            if (currentUserId == null) {
                Long threadLocalUserId = UserContext.getUserId();
                if (threadLocalUserId != null) {
                    currentUserId = threadLocalUserId.intValue();
                }
            }
            if (currentUserId == null) {
                return Result.badRequest("用户ID不能为空，请先登录");
            }

            List<FamilyMember> familyMembers = familyMemberService.getFamilyMembersByOwnerId(currentUserId);
            log.info("查询家人列表成功，创建者ID：{}，家人数量：{}", currentUserId, familyMembers.size());
            return Result.success("查询成功", familyMembers);
        } catch (RuntimeException e) {
            log.error("查询家人列表失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("查询家人列表时发生异常", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 添加家人（需要验证家人密码）
     * 
     * @param request 添加家人请求（包含phone、password、relation）
     * @param userId 创建者用户ID（可选，如果未提供则从ThreadLocal获取）
     * @return 添加的家人信息
     */
    @Operation(summary = "添加家人", description = "验证家人手机号和密码后添加为家人")
    @PostMapping("/add")
    public Result<FamilyMember> addFamilyMember(
            @RequestBody Map<String, String> request,
            @RequestParam(required = false) Integer userId) {
        try {
            Integer currentUserId = userId;
            if (currentUserId == null) {
                Long threadLocalUserId = UserContext.getUserId();
                if (threadLocalUserId != null) {
                    currentUserId = threadLocalUserId.intValue();
                }
            }
            if (currentUserId == null) {
                return Result.badRequest("用户ID不能为空，请先登录");
            }

            String phone = request.get("phone");
            String password = request.get("password");
            String relation = request.get("relation");

            FamilyMember familyMember = familyMemberService.addFamilyMemberWithPassword(
                    phone, password, currentUserId, relation);

            log.info("添加家人成功，创建者ID：{}，家人手机号：{}", currentUserId, phone);
            return Result.success("添加家人成功", familyMember);
        } catch (RuntimeException e) {
            log.error("添加家人失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("添加家人时发生异常", e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 删除家人
     * 
     * @param memberId 家人ID
     * @param userId 创建者用户ID（可选，如果未提供则从ThreadLocal获取）
     * @return 删除结果
     */
    @Operation(summary = "删除家人", description = "根据家人ID删除家人")
    @DeleteMapping("/{memberId}")
    public Result<Void> deleteFamilyMember(
            @PathVariable Integer memberId,
            @RequestParam(required = false) Integer userId) {
        try {
            Integer currentUserId = userId;
            if (currentUserId == null) {
                Long threadLocalUserId = UserContext.getUserId();
                if (threadLocalUserId != null) {
                    currentUserId = threadLocalUserId.intValue();
                }
            }
            if (currentUserId == null) {
                return Result.badRequest("用户ID不能为空，请先登录");
            }

            // 验证家人是否属于当前用户
            FamilyMember familyMember = familyMemberService.getById(memberId);
            if (familyMember == null) {
                return Result.badRequest("家人不存在");
            }
            if (!familyMember.getOwnerUserId().equals(currentUserId)) {
                return Result.badRequest("无权删除该家人");
            }

            familyMemberService.removeById(memberId);
            log.info("删除家人成功，创建者ID：{}，家人ID：{}", currentUserId, memberId);
            return Result.success();
        } catch (RuntimeException e) {
            log.error("删除家人失败", e);
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("删除家人时发生异常", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}
