package com.pulsecheck.pulse_check.controller;

import com.pulsecheck.pulse_check.model.ApiMessage;
import com.pulsecheck.pulse_check.model.Monitor;
import com.pulsecheck.pulse_check.model.MonitorRequest;
import com.pulsecheck.pulse_check.service.MonitorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @PostMapping
    public ResponseEntity<ApiMessage> register(@RequestBody MonitorRequest request) {
        monitorService.register(request.getId(), request.getTimeout(), request.getAlertEmail());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiMessage("Monitor " + request.getId() + " registered"));
    }

    @PostMapping("/{id}/heartbeat")
    public ResponseEntity<ApiMessage> heartbeat(@PathVariable String id) {
        boolean found = monitorService.heartbeat(id);
        if (!found) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiMessage("Monitor " + id + " not found"));
        }
        return ResponseEntity.ok(new ApiMessage("Heartbeat received for " + id));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiMessage> pause(@PathVariable String id) {
        boolean found = monitorService.pause(id);
        if (!found) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiMessage("Monitor " + id + " not found"));
        }
        return ResponseEntity.ok(new ApiMessage("Monitor " + id + " paused"));
    }
}