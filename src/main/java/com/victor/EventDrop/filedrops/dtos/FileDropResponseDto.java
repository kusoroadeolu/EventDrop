package com.victor.EventDrop.filedrops.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FileDropResponseDto(
        String fileId,
        String fileName,
        BigDecimal fileSizeInMB,
        LocalDateTime uploadedAt
) {
}
