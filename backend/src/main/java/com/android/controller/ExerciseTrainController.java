package com.android.controller;

import com.android.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

/**
 * 运动识别控制器 - 调用 ai-exercise-train 摄像头识别
 * 小程序运动页上传摄像头帧，后端转发到 ai-exercise-train 服务进行实时识别。
 */
@Slf4j
@RestController
@RequestMapping("/exercise-train")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "运动识别", description = "AI 运动识别（ai-exercise-train）")
public class ExerciseTrainController {

    private final RestTemplate restTemplate;

    @Value("${exercise.train.service.url:http://127.0.0.1:5000}")
    private String exerciseTrainServiceUrl;

    /**
     * 上传一帧图片，返回运动类型与置信度（由前端定时拍照调用，实现“实时”识别）
     */
    @Operation(summary = "运动识别", description = "上传摄像头图片，返回运动类型、置信度、次数及会话运动记录（可选 sessionId 保持同一会话）")
    @PostMapping("/recognize")
    public Result<Map<String, Object>> recognize(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请上传图片");
        }
        String url = exerciseTrainServiceUrl.replaceAll("/$", "") + "/recognize";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "frame.jpg";
                }
            });
            if (sessionId != null && !sessionId.isEmpty()) {
                body.add("sessionId", sessionId);
            }
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> respBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && respBody != null) {
                Object codeObj = respBody.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 0;
                if (code == 200) {
                    return Result.success("识别成功", respBody);
                }
                return Result.success(respBody.get("message") != null ? respBody.get("message").toString() : "识别完成", respBody);
            }
            return Result.error("识别服务返回异常");
        } catch (Exception e) {
            log.warn("调用 ai-exercise-train 失败: {}, 请确认已启动 python run_server.py", e.getMessage());
            return Result.error("识别服务不可用，请确认已启动 ai-exercise-train 的 run_server.py（端口 5000）");
        }
    }

    /**
     * 上传视频文件，返回视频内运动记录（各项次数与时长）。
     */
    @Operation(summary = "视频识别", description = "上传视频文件，逐帧识别并返回运动记录（各项次数与时长）")
    @PostMapping("/recognize-video")
    public Result<Map<String, Object>> recognizeVideo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.badRequest("请上传视频文件");
        }
        String url = exerciseTrainServiceUrl.replaceAll("/$", "") + "/recognize-video";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.mp4";
                }
            });
            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, request,
                    new ParameterizedTypeReference<Map<String, Object>>() {});
            Map<String, Object> respBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK && respBody != null) {
                Object codeObj = respBody.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 0;
                if (code == 200) {
                    return Result.success("分析完成", respBody);
                }
                return Result.success(respBody.get("message") != null ? respBody.get("message").toString() : "分析完成", respBody);
            }
            return Result.error("识别服务返回异常");
        } catch (Exception e) {
            log.warn("调用 ai-exercise-train 视频识别失败: {}", e.getMessage());
            return Result.error("视频识别服务不可用，请确认已启动 run_server.py");
        }
    }

    /**
     * 获取 ai-exercise-train 生成的带标注结果视频。
     */
    @Operation(summary = "获取标注视频", description = "按 token 拉取 ai-exercise-train 生成的标注结果视频")
    @GetMapping("/annotated-video/{token}")
    public ResponseEntity<byte[]> getAnnotatedVideo(@PathVariable("token") String token) {
        String url = exerciseTrainServiceUrl.replaceAll("/$", "") + "/annotated-video/" + token;
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("video/mp4"));
                headers.setCacheControl(CacheControl.noStore().getHeaderValue());
                return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.warn("拉取标注视频失败 token={}: {}", token, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
