package com.magnossao.controller;

import java.time.Instant;
import com.magnossao.service.HealthService;
import com.magnossao.dto.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService service;

    public HealthController(HealthService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        boolean dbOk = service.databaseOk();
        HealthResponse body = new HealthResponse(
                dbOk ? "ok" : "degraded",
                dbOk ? "ok" : "erro",
                Instant.now().toString());
        return dbOk ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }
}
