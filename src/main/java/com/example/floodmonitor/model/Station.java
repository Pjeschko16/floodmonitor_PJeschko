package com.example.floodmonitor.model;

public class Station {
    private String id;
    private String stationName;
    private String timestamp;
    private double waterLevel;
    private double waterTemperature;
    private String unit;
    private double batteryLevel;
    private double flowRate;
    private String status;
    private String warnLevel;          // z. B. "NORMAL", "WARNING", "CRITICAL", "UNKNOWN"
    private double precipitationRate;  // Niederschlag in mm/h
    private double waterRiseRate;
    private String river;
    private boolean isActive;

    public Station() {}

    public Station(String id, String stationName, String timestamp, double waterLevel, double waterTemperature, String unit) {
        this.id = id;
        this.stationName = stationName;
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.waterTemperature = waterTemperature;
        this.unit = unit;
    }
    // Getter für den Status als String (falls benötigt)
    // HIER DIE FEHLENDEN METHODEN EINFÜGEN:
    public Boolean isActiv() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
    // --- Bestehende Getter und Setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    public double getWaterTemperature() { return waterTemperature; }
    public void setWaterTemperature(double waterTemperature) { this.waterTemperature = waterTemperature; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    // --- Neue Getter und Setter für die Simulation ---
    public double getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }

    public double getFlowRate() { return flowRate; }
    public void setFlowRate(double flowRate) { this.flowRate = flowRate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getWarnLevel() {
        return warnLevel;
    }

    public double getPrecipitationRate() {
        return precipitationRate;
    }

    public double getWaterRiseRate() {
        return waterRiseRate;
    }
    public void setWarnLevel(String warnLevel) {
        this.warnLevel = warnLevel;
    }
    public void setPrecipitationRate(double precipitationRate) {
        this.precipitationRate = precipitationRate;
    }
    public void setWaterRiseRate(double waterRiseRate) {
        this.waterRiseRate = waterRiseRate;
    }

    public String getRiver() {
        return river;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
    public void setRiver(String river) {
        this.river = river;
    }
}