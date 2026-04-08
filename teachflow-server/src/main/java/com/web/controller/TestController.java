package com.web.controller;

import com.web.common.ApiResponse;
import com.web.config.XfyunConfig;
import com.web.util.SiliconFlowUtil;
import com.web.util.XfyunSignUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private XfyunConfig xfyunConfig;


    @GetMapping("/signature")
    public String testSignature(
            @RequestParam String appId,
            @RequestParam String accessKeyId,
            @RequestParam String fileSize,
            @RequestParam String fileName,
            @RequestParam String duration,
            @RequestParam(required = false, defaultValue = "autodialect") String language
    ) {

        String dateTime = XfyunSignUtil.getCurrentDateTime();
        String signatureRandom = XfyunSignUtil.generateRandom16();


        Map<String, String> params = new HashMap<>();
        params.put("appId", appId);
        params.put("accessKeyId", accessKeyId);
        params.put("dateTime", dateTime);
        params.put("signatureRandom", signatureRandom);
        params.put("fileSize", fileSize);
        params.put("fileName", fileName);
        params.put("duration", duration);
        params.put("language", language);


        String signature = XfyunSignUtil.generateSignature(params, xfyunConfig.getApiSecret());


        return String.format(
                "dateTime: %s\nsignatureRandom: %s\nsignature: %s",
                dateTime, signatureRandom, signature
        );
    }

    @Autowired
    private SiliconFlowUtil siliconFlowUtil;

    @GetMapping("/test-ai")
    public ApiResponse<SiliconFlowUtil.AnalysisResult> testAi(
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "GENERAL") String sceneType) {
        try {
            SiliconFlowUtil.AnalysisResult result = siliconFlowUtil.analyze(text, sceneType, "测试用的附加背景信息");
            return ApiResponse.ok(result);
        } catch (Exception e) {
            return ApiResponse.fail(5000, e.getMessage());
        }
    }


}