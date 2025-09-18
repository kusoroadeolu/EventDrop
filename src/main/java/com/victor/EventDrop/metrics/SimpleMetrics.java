package com.victor.EventDrop.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SimpleMetrics
{

    private Integer roomsCreated;
    private Integer filesUploaded;
    private Integer filesDownloaded;



}
