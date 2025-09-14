package com.victor.EventDrop.filedrops;

import java.time.LocalDateTime;

public record FileDropResponseDto(
        String fileId,
        String fileName,
        LocalDateTime uploadedAt
) {
}
