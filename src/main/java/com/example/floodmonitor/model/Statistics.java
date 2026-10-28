package com.example.floodmonitor.model;

public class Statistics {
        private double minWaterLevel;
        private double maxWaterLevel;
        private double averageWaterLevel;
        private double averageFlowRate;
        private double totalPrecipitation;
        private long measurementCount;
        private long alertCount;
        private long criticalAlertCount;

    public Statistics(double minWaterLevel, double maxWaterLevel, double averageWaterLevel, double averageFlowRate, double totalPrecipitation, long measurementCount, long alertCount, long criticalAlertCount) {
        this.minWaterLevel = minWaterLevel;
        this.maxWaterLevel = maxWaterLevel;
        this.averageWaterLevel = averageWaterLevel;
        this.averageFlowRate = averageFlowRate;
        this.totalPrecipitation = totalPrecipitation;
        this.measurementCount = measurementCount;
        this.alertCount = alertCount;
        this.criticalAlertCount = criticalAlertCount;
    }

    public Statistics() {
    }

    public double getMinWaterLevel() {
        return minWaterLevel;
    }

    public double getMaxWaterLevel() {
        return maxWaterLevel;
    }

    public double getAverageWaterLevel() {
        return averageWaterLevel;
    }

    public double getAverageFlowRate() {
        return averageFlowRate;
    }

    public double getTotalPrecipitation() {
        return totalPrecipitation;
    }

    public long getMeasurementCount() {
        return measurementCount;
    }

    public long getAlertCount() {
        return alertCount;
    }

    public long getCriticalAlertCount() {
        return criticalAlertCount;
    }

    public void setMinWaterLevel(double minWaterLevel) {
        this.minWaterLevel = minWaterLevel;
    }

    public void setMaxWaterLevel(double maxWaterLevel) {
        this.maxWaterLevel = maxWaterLevel;
    }

    public void setAverageWaterLevel(double averageWaterLevel) {
        this.averageWaterLevel = averageWaterLevel;
    }

    public void setAverageFlowRate(double averageFlowRate) {
        this.averageFlowRate = averageFlowRate;
    }

    public void setTotalPrecipitation(double totalPrecipitation) {
        this.totalPrecipitation = totalPrecipitation;
    }

    public void setMeasurementCount(long measurementCount) {
        this.measurementCount = measurementCount;
    }

    public void setAlertCount(long alertCount) {
        this.alertCount = alertCount;
    }

    public void setCriticalAlertCount(long criticalAlertCount) {
        this.criticalAlertCount = criticalAlertCount;
    }
}
