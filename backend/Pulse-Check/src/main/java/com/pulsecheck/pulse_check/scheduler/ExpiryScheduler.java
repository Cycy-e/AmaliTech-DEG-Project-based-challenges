package com.pulsecheck.pulse_check.scheduler;

import com.pulsecheck.pulse_check.model.Monitor;
import com.pulsecheck.pulse_check.service.AlertService;
import com.pulsecheck.pulse_check.service.MonitorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class ExpiryScheduler {

    private static final long GRACE_PERIOD_SECONDS = 5;

    private final MonitorService monitorService;
    private final AlertService alertService;

    public ExpiryScheduler(MonitorService monitorService, AlertService alertService) {
        this.monitorService = monitorService;
        this.alertService = alertService;
    }

    @Scheduled(fixedRate = 1000)
    public void checkExpiredMonitors() {
        for (Monitor monitor : monitorService.getAllMonitors().values()) {
            if (monitor.getStatus() != Monitor.Status.ACTIVE) {
                continue;
            }
            long elapsed = Duration.between(monitor.getLastHeartbeat(), Instant.now()).getSeconds();
            if (elapsed >= monitor.getTimeoutSeconds() + GRACE_PERIOD_SECONDS) {
                monitor.setStatus(Monitor.Status.DOWN);
                alertService.fireDownAlert(monitor);
            }
        }
    }
}