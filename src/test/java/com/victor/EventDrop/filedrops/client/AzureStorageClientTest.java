package com.victor.EventDrop.filedrops.client;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.batch.BlobBatch;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.victor.EventDrop.exceptions.AzureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureStorageClientTest {

    @Mock private BlobContainerClient blobContainerClient;
    @Mock private AsyncTaskExecutor asyncTaskExecutor;
    @Mock private BlobBatchClient blobBatchClient;
    @Mock private BlobClient blobClient;
    @Mock private BlobBatch blobBatch;

    @InjectMocks private AzureStorageClient azureStorageClient;

    @BeforeEach
    void setUp() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.initialize();
        azureStorageClient = new AzureStorageClient(blobContainerClient, executor, blobBatchClient);

    }

    @Test
    void uploadFile_shouldReturnBlobUrl_whenSuccessful() {
        InputStream dummyStream = new ByteArrayInputStream("data".getBytes());
        when(blobContainerClient.getBlobClient("file.txt")).thenReturn(blobClient);
        when(blobClient.getBlobUrl()).thenReturn("http://azure.blob/file.txt");

        CompletableFuture<String> future = azureStorageClient.uploadFile("file.txt", 4, dummyStream);
        String result = future.join();

        assertNotNull(result);
        assertEquals("http://azure.blob/file.txt", result);
        verify(blobClient, times(1)).upload(dummyStream);
    }

    @Test
    void uploadFile_shouldThrowCompletionException_whenUploadFails() {
        InputStream dummyStream = new ByteArrayInputStream("data".getBytes());

        when(blobContainerClient.getBlobClient("file.txt")).thenReturn(blobClient);
        doThrow(new RuntimeException("upload failed")).when(blobClient).upload(any(InputStream.class));

        CompletionException ex = assertThrows(CompletionException.class,
                () -> azureStorageClient.uploadFile("file.txt", 4, dummyStream).join()
        );

        assertTrue(ex.getMessage().contains("upload failed"));
    }

    // ======== Download SAS URL Tests ========
    @Test
    void downloadFile_shouldReturnPreSignedUrl() {
        when(blobContainerClient.getBlobClient("file.txt")).thenReturn(blobClient);
        when(blobClient.generateSas(any())).thenReturn("sas-token");

        String fileDropUrl = "https://my.blob.com/file.txt";
        String result = azureStorageClient.downloadFile("file.txt", fileDropUrl);

        assertTrue(result.contains(fileDropUrl));
        assertTrue(result.contains("sas-token"));
        verify(blobClient, times(1)).generateSas(any());
    }

    @Test
    void downloadFile_shouldThrowAzureException_onFailure() {
        when(blobContainerClient.getBlobClient("file.txt")).thenReturn(blobClient);
        doThrow(new RuntimeException("SAS failed")).when(blobClient).generateSas(any());

        AzureException ex = assertThrows(AzureException.class,
                () -> azureStorageClient.downloadFile("file.txt", "https://my.blob.com/file.txt"));

        assertTrue(ex.getMessage().contains("Failed to generate pre-signed url"));
    }

    // ======== Batch Delete Tests ========
    @Test
    void deleteFiles_shouldCallBatchDeleteSuccessfully() {
        List<String> blobs = List.of("file1.txt", "file2.txt");

        when(blobBatchClient.getBlobBatch()).thenReturn(blobBatch);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient, blobClient);
        when(blobClient.getBlobUrl()).thenReturn("url1", "url2");

        azureStorageClient.deleteFiles(blobs);

        verify(blobBatchClient, times(1)).submitBatch(blobBatch);
        verify(blobBatch, times(2)).deleteBlob(anyString());
    }

    @Test
    void deleteFiles_shouldThrowAzureException_onFailure() {
        List<String> blobs = List.of("file1.txt", "file2.txt");
        BlobClient client = Mockito.mock(BlobClient.class);

        when(blobBatchClient.getBlobBatch()).thenReturn(blobBatch);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(client, client);
        when(client.getBlobUrl()).thenReturn("url1", "url2");
        doThrow(new RuntimeException("delete failed")).when(blobBatchClient).submitBatch(blobBatch);

        AzureException ex = assertThrows(AzureException.class,
                () -> azureStorageClient.deleteFiles(blobs));

        assertTrue(ex.getMessage().contains("Batch delete failed"));
    }
}
