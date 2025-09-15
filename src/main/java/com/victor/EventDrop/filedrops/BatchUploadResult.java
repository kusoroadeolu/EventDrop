package com.victor.EventDrop.filedrops;

import java.util.List;

public record BatchUploadResult(
        List<FileDropResponseDto> successfulUploads,
        List<FileDropResponseDto> failedUploads
) {

}
