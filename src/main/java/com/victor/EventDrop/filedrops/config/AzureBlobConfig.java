package com.victor.EventDrop.filedrops.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.storage.blob.*;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.batch.BlobBatchClientBuilder;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.policy.RequestRetryOptions;
import com.azure.storage.common.policy.RetryPolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Configuration
@RequiredArgsConstructor
public class AzureBlobConfig {

    private final AzureStorageBlobConfigProperties configProperties;

    @Value("${spring.cloud.azure.vault.url}")
    private String vaultUrl;

    @Bean
    public SecretClient secretClient(DefaultAzureCredential credential){
        return new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .credential(credential)
                .buildClient();
    }

    @Bean
    //@Profile("dev")
    public DefaultAzureCredential credential(){
        return new DefaultAzureCredentialBuilder()
                .build();
    }


    @Bean
    public BlobContainerClient blobContainerClient(){
        return new BlobContainerClientBuilder()
                .connectionString(configProperties.getConnectionString())
                .containerName(configProperties.getContainerName())
                .buildClient();
    }

    @Bean
    public BlobSasPermission blobSasPermission(){
        return new BlobSasPermission().setReadPermission(true);
    }

    @Bean
    public OffsetDateTime expiryTime(){
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5);
    }

    @Bean
    public BlobBatchClient blobBatchClient(BlobContainerClient blobContainerClient){
        return new BlobBatchClientBuilder(blobContainerClient.getServiceClient())
                .buildClient();
    }



}
