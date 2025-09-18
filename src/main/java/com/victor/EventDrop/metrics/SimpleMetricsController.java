package com.victor.EventDrop.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/metrics")
public class SimpleMetricsController {

    private final SimpleMetricsService simpleMetricsService;

    @GetMapping
    public ResponseEntity<SimpleMetricsDto> simpleMetricsDto(){
        return new ResponseEntity<>(simpleMetricsService.metricsDto(), HttpStatus.OK);
    }

}
