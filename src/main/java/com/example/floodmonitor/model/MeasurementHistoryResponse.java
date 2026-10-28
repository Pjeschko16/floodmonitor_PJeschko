package com.example.floodmonitor.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "measurementHistory")
public class MeasurementHistoryResponse {
    private List<Measurement> measurements;
    private Statistics statistics;

    public MeasurementHistoryResponse() { }

    public MeasurementHistoryResponse(List<Measurement> measurements, Statistics statistics) {
        this.measurements = measurements;
        this.statistics = statistics;
    }

    public List<Measurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<Measurement> measurements) { this.measurements = measurements; }
    public Statistics getStatistics() { return statistics; }
    public void setStatistics(Statistics statistics) { this.statistics = statistics; }
}
