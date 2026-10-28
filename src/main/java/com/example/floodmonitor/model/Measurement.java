package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "measurement")
public class Measurement {
    private String timestamp;
    private double waterLevel;
    private double flowRate;
    private double precipitationRate;
    private String warnLevel;

    public Measurement() {}

    public Measurement(String timestamp, double waterLevel, double flowRate, double precipitationRate, String warnLevel) {
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.flowRate = flowRate;
        this.precipitationRate = precipitationRate;
        this.warnLevel = warnLevel;
    }

    // Getter und Setter
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    public double getFlowRate() { return flowRate; }
    public void setFlowRate(double flowRate) { this.flowRate = flowRate; }

    @JsonProperty("rainfall")
    public double getPrecipitationRate() { return precipitationRate; }
    public void setPrecipitationRate(double precipitationRate) { this.precipitationRate = precipitationRate; }

    @JsonProperty("warningLevel")
    public String getWarnLevel() { return warnLevel; }
    public void setWarnLevel(String warnLevel) { this.warnLevel = warnLevel; }
}
