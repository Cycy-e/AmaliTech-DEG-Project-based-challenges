package com.pulsecheck.pulse_check.service;

import com.pulsecheck.pulse_check.model.Monitor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AlertService {

    public void fireDownAlert(Monitor monitor) {
        System.out.println("{\"ALERT\": \"Device " + monitor.getId() + " is down!\", \"time\": \"" + Instant.now() + "\"}");
    }
}