package com.pulsecheck.pulse_check.service;

import com.pulsecheck.pulse_check.model.Monitor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AlertService {

    private final List<String> alertHistory = new CopyOnWriteArrayList<>();

    public void fireDownAlert(Monitor monitor) {
        String alert = "{\"ALERT\": \"Device " + monitor.getId() + " is down!\", \"time\": \"" + Instant.now() + "\"}";
        System.out.println(alert);
        alertHistory.add(alert);
    }

    public List<String> getAlertHistory() {
        return Collections.unmodifiableList(alertHistory);
    }
}