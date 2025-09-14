package com.victor.EventDrop.filedrops.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("spring.cloud.azure.storage.blob")
@Component
@Getter
@Setter
public class AzureStorageBlobConfigProperties {
    private String accountName;
    private String accountKey;
    private String connectionString;
    private String endpoint;
    private String containerName;
}
