package com.android.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工作台开发资源：训练结果图、提示词文本（本地路径由配置指定）。
 */
@Service
public class DashboardDevAssetService {

    @Value("${dashboard.dev-assets.exercise-results-image:}")
    private String exerciseResultsImage;

    @Value("${dashboard.dev-assets.food-results-image:}")
    private String foodResultsImage;

    @Value("${dashboard.dev-assets.chat-prompt-file:}")
    private String chatPromptFile;

    @Value("${dashboard.dev-assets.emotion-prompt-file:}")
    private String emotionPromptFile;

    public byte[] readExerciseResultsImage() throws IOException {
        return readImageBytes(exerciseResultsImage);
    }

    public byte[] readFoodResultsImage() throws IOException {
        return readImageBytes(foodResultsImage);
    }

    private byte[] readImageBytes(String configuredPath) throws IOException {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IOException("未配置图片路径");
        }
        Path p = Paths.get(configuredPath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(p)) {
            throw new IOException("文件不存在: " + p);
        }
        return Files.readAllBytes(p);
    }

    public String readPrompt(String slot) throws IOException {
        String pathStr = switch (slot) {
            case "chat" -> chatPromptFile;
            case "emotion" -> emotionPromptFile;
            default -> throw new IllegalArgumentException("unknown prompt slot: " + slot);
        };
        if (pathStr == null || pathStr.isBlank()) {
            return "";
        }
        Path p = Paths.get(pathStr).toAbsolutePath().normalize();
        if (!Files.isRegularFile(p)) {
            return "";
        }
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    public void writePrompt(String slot, String text) throws IOException {
        String pathStr = switch (slot) {
            case "chat" -> chatPromptFile;
            case "emotion" -> emotionPromptFile;
            default -> throw new IllegalArgumentException("unknown prompt slot: " + slot);
        };
        if (pathStr == null || pathStr.isBlank()) {
            throw new IOException("未配置提示词文件路径");
        }
        Path p = Paths.get(pathStr).toAbsolutePath().normalize();
        Path parent = p.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(p, text == null ? "" : text, StandardCharsets.UTF_8);
    }
}
