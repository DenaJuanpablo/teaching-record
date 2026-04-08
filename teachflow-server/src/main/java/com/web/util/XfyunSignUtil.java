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


@Component
public class XfyunSignUtil {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final String TIME_ZONE_OFFSET = "+0800";
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RANDOM_LENGTH = 16;


    public static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String formatted = now.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
        return formatted + TIME_ZONE_OFFSET;
    }


    public static String generateRandom16() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }


    public static String generateSignature(Map<String, String> params, String accessKeySecret) {

        TreeMap<String, String> sortedMap = new TreeMap<>(params);
        sortedMap.remove("signature");


        StringBuilder baseString = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }

            String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
            baseString.append(key).append("=").append(encodedValue).append("&");
        }
        if (baseString.length() > 0) {
            baseString.deleteCharAt(baseString.length() - 1);
        }


        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(
                    accessKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA1"
            );
            mac.init(keySpec);
            byte[] signBytes = mac.doFinal(baseString.toString().getBytes(StandardCharsets.UTF_8));


            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
}