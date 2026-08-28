package com.pulsecheck.pulse_check.model;

public class MonitorRequest {

    private String id;
    private int timeout;
    private String alertEmail;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }
}