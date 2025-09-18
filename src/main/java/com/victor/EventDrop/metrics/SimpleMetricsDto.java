package com.victor.EventDrop.metrics;

public record SimpleMetricsDto(
        int totalRoomsCreated,
        int totalFilesUploaded,
        int totalFilesDownloaded
) {
}
