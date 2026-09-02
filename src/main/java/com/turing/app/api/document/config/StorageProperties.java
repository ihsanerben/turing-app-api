package com.turing.app.api.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(boolean enabled,String endpoint,String accessKey,String secretKey,String bucket) {}
