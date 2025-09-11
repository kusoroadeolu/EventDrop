package com.victor.EventDrop.tests;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public SecretClient secretClient(DefaultAzureCredential credential){
        return new SecretClientBuilder()
                .vaultUrl("https://eventdropkv.vault.azure.net/")
                .credential(credential)
                .buildClient();
    }

    @Bean
    public DefaultAzureCredential credential(){
        return new DefaultAzureCredentialBuilder()
                .build();
    }
}
