package com.android.controller;

import com.android.common.Result;
import com.android.service.DashboardDevAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 管理端工作台：本地训练结果图、提示词文件读写（路径见 dashboard.dev-assets.*）。
 */
@RestController
@RequestMapping("/admin/dashboard/dev")
@RequiredArgsConstructor
public class AdminDashboardDevController {

    private final DashboardDevAssetService dashboardDevAssetService;

    @GetMapping("/images/exercise-results")
    public ResponseEntity<byte[]> exerciseResultsImage() {
        return imageResponse(() -> dashboardDevAssetService.readExerciseResultsImage());
    }

    @GetMapping("/images/food-results")
    public ResponseEntity<byte[]> foodResultsImage() {
        return imageResponse(() -> dashboardDevAssetService.readFoodResultsImage());
    }

    private ResponseEntity<byte[]> imageResponse(ImageSupplier supplier) {
        try {
            byte[] bytes = supplier.get();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-store");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @FunctionalInterface
    private interface ImageSupplier {
        byte[] get() throws IOException;
    }

    @GetMapping("/prompts/{slot}")
    public Result<String> getPrompt(@PathVariable String slot) {
        try {
            return Result.success(dashboardDevAssetService.readPrompt(normalizeSlot(slot)));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("读取失败: " + e.getMessage());
        }
    }

    @PutMapping(value = "/prompts/{slot}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Result<Void> putPrompt(@PathVariable String slot, @RequestBody(required = false) String body) {
        try {
            dashboardDevAssetService.writePrompt(normalizeSlot(slot), body);
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (IOException e) {
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    private static String normalizeSlot(String slot) {
        if (slot == null) {
            throw new IllegalArgumentException("slot 不能为空");
        }
        return slot.trim().toLowerCase();
    }
}
