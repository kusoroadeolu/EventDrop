package com.victor.EventDrop.filedrops.dtos;

import java.time.LocalDateTime;

public record FileDropResponseDto(
        String fileId,
        String fileName,
        java.math.BigDecimal fileSizeInMB,
        LocalDateTime uploadedAt
) {
}
