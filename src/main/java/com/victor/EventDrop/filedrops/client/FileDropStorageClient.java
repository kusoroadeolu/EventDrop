package com.victor.EventDrop.filedrops.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface FileDropStorageClient {

    CompletableFuture<String> uploadFile(String fileName, long sizeInBytes, InputStream inputStream) throws IOException;

    String downloadFile(String fileDropUrl);

    void deleteFiles(List<String> fileNames);
}
