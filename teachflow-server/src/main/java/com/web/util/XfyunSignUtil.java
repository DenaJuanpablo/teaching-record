package com.web.util;

import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * 讯飞 API 签名工具类
 */
@Component
public class XfyunSignUtil {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String TIME_ZONE_OFFSET = "+0800"; // 东八区固定偏移
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_LENGTH = 16;

    /**
     * 生成当前时间字符串，格式：yyyy-MM-dd'T'HH:mm:ss+0800
     */
    public static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
        return formatted + TIME_ZONE_OFFSET;
    }

    /**
     * 生成16位随机字符串（大小写字母+数字）
     */
    public static String generateRandom16() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 生成签名
     * @param params 请求参数（不含 signature），键值对
     * @param accessKeySecret 您的 APISecret
     * @return Base64 编码后的签名
     */
    public static String generateSignature(Map<String, String> params, String accessKeySecret) {
        // 1. 按参数名排序（TreeMap 自动排序）
        TreeMap<String, String> sortedMap = new TreeMap<>(params);
        sortedMap.remove("signature"); // 移除可能存在的 signature 字段

        // 2. 拼接 baseString（参数值需要 URL 编码）
        StringBuilder baseString = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue; // 空值不参与签名（根据文档）
            }
            // 对值进行 URL 编码
            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
            baseString.append(key).append("=").append(encodedValue).append("&");
        }
        if (baseString.length() > 0) {
            baseString.deleteCharAt(baseString.length() - 1); // 删除末尾的 &
        }

        // 3. HMAC-SHA1 加密
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(
                    accessKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA1"
            );
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(baseString.toString().getBytes(StandardCharsets.UTF_8));

            // 4. Base64 编码
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}