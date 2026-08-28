package com.pulsecheck.pulse_check.model;

import java.time.Instant;

public class Monitor {

    public enum Status {
        ACTIVE, PAUSED, DOWN
    }

    private String id;
    private int timeoutSeconds;
    private String alertEmail;
    private Instant lastHeartbeat;
    private Status status;

    public Monitor(String id, int timeoutSeconds, String alertEmail) {
        this.id = id;
        this.timeoutSeconds = timeoutSeconds;
        this.alertEmail = alertEmail;
        this.lastHeartbeat = Instant.now();
        this.status = Status.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(Instant lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}