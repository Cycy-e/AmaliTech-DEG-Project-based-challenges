package com.pulsecheck.pulse_check.service;

import com.pulsecheck.pulse_check.model.Monitor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitorService {

    private final Map<String, Monitor> monitors = new ConcurrentHashMap<>();

    public Monitor register(String id, int timeoutSeconds, String alertEmail) {
        Monitor monitor = new Monitor(id, timeoutSeconds, alertEmail);
        monitors.put(id, monitor);
        return monitor;
    }

    public Optional<Monitor> findById(String id) {
        return Optional.ofNullable(monitors.get(id));
    }

    public boolean heartbeat(String id) {
        Monitor monitor = monitors.get(id);
        if (monitor == null) {
            return false;
        }
        monitor.setLastHeartbeat(Instant.now());
        monitor.setStatus(Monitor.Status.ACTIVE);
        return true;
    }

    public boolean pause(String id) {
        Monitor monitor = monitors.get(id);
        if (monitor == null) {
            return false;
        }
        monitor.setStatus(Monitor.Status.PAUSED);
        return true;
    }

    public Map<String, Monitor> getAllMonitors() {
        return monitors;
    }
}