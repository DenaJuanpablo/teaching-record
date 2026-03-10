package com.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XfyunConfig {
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String uploadUrl;
    private String getResultUrl;
}