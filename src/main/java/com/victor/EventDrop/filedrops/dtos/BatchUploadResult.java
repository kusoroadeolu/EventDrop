package com.victor.EventDrop.filedrops.dtos;

import java.util.List;

public record BatchUploadResult(
        List<FileDropResponseDto> successfulUploads,
        List<FileDropResponseDto> failedUploads
) {

}
