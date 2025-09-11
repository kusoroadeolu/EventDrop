package com.victor.EventDrop.tests;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AzureSdkTest {

    @Value("azure-blob://eventdrop-uploads/VibeMatch_API_README.md")
    private Resource file;

    @GetMapping("/read-file")
    public CompletableFuture<ResponseEntity<Path>> testSdk() throws InterruptedException {
        ExecutorService virtualExecutorService = Executors.newVirtualThreadPerTaskExecutor();

        if(file.getFilename() == null)return null;

        return CompletableFuture.supplyAsync(() -> {
            String userDir = System.getProperty("user.home");
            Path downloadPath = Path.of(userDir, "Downloads", file.getFilename());
            try(InputStream stream = file.getInputStream()){
                long size = Files.copy(stream, downloadPath, StandardCopyOption.REPLACE_EXISTING);
                return new ResponseEntity(downloadPath, HttpStatus.OK);
            }catch (IOException e){
                throw new RuntimeException("", e);
            }

        }, virtualExecutorService);

    }
}
