package com.victor.EventDrop.filedrops.client;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.batch.BlobBatch;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.victor.EventDrop.exceptions.AzureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class AzureStorageClient implements FileDropStorageClient {
    private final BlobContainerClient blobContainerClient;
    private final AsyncTaskExecutor  asyncTaskExecutor;
    private final BlobBatchClient blobBatchClient;

    /**
     * Asynchronously uploads a file to Azure Blob Storage.
     *
     * @param fileName      the name of the file.
     * @param sizeInBytes   the file's size.
     * @param inputStream   the file's content.
     * @return a CompletableFuture for the uploaded file's URL.
     */
    @Override
    public CompletableFuture<String> uploadFile(String fileName, long sizeInBytes, InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BlobClient client = blobContainerClient.getBlobClient(fileName);
                log.info("Attempting to upload file: {} into azure blob storage", fileName);
                 client.upload(inputStream, sizeInBytes, true);
                log.info("Successfully uploaded file: {} into azure blob storage", fileName);
                return client.getBlobUrl();
            }catch (Exception e){
                log.info("An error occurred while trying to upload file to azure: {}. Error message: {}", fileName, e.getMessage());
                throw new AzureException(String.format("An error occurred while trying to upload file to azure: %s. Error message: %s", fileName, e.getMessage()), e);
            }

        }, asyncTaskExecutor)
                .exceptionally(throwable -> {
                    log.error("An error occurred while trying to upload file to azure: {}. Error message: {}", fileName, throwable.getMessage());
                    throw new AzureException(String.format("An error occurred while trying to upload file to azure: %s. Error message: %s", fileName, throwable.getMessage()));
                });


    }

    @Override
    public String downloadFile(String fileDropUrl) {
        return "";
    }


    /**
     * Generates a secure, pre-signed download URL for a file.
     *
     * @param fileDropUrl the file drop url
     * @return a secure download URL.
     */
    @Override
    public String downloadFile(String blobName, String fileDropUrl){
        try{
            BlobClient client = blobContainerClient.getBlobClient(blobName);

            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                    OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5), // expiry time
                    new BlobSasPermission().setReadPermission(true)
            ).setStartTime(OffsetDateTime.now(ZoneOffset.UTC));

            String sasToken = client.generateSas(sasValues);
            log.info("Successfully generated sas token: {}", sasToken);
            String preSignedUrl = fileDropUrl + "?" + sasToken;
            log.info("Successfully generated pre-signed url for download: {}", preSignedUrl);
            return preSignedUrl;
        }catch (Exception e){
            log.info("Failed to generate pre-signed url for download", e);
            throw new AzureException("Failed to generate pre-signed url for download", e);
        }
    }

    @Override
    public void deleteFiles(List<String> blobNames){
        try{
            log.info("Attempting batch delete for {} blobs", blobNames.size());
            BlobBatch batch = blobBatchClient.getBlobBatch();
            blobNames.forEach(blobName -> {
                BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
                if(blobClient.exists()){
                    batch.deleteBlob(blobClient.getBlobUrl());
                }
            });
            blobBatchClient.submitBatch(batch);

            log.info("Successfully submitted batch delete for {} blobs", blobNames.size());

        }catch (Exception e){
            log.error("Failed to perform batch delete for {} blobs", blobNames.size(), e);
            throw new AzureException("Batch delete failed for blobs", e);
        }
    }


}
