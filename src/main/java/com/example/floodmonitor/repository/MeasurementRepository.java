package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class MeasurementRepository {
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Measurement>> measurements = new ConcurrentHashMap<>();

    public void save(String stationId, Measurement measurement) {
        measurements.computeIfAbsent(stationId, ignored -> new CopyOnWriteArrayList<>()).add(measurement);
    }

    public void deleteAll() { measurements.clear(); }

    public List<Measurement> findByStationId(String stationId) {
        List<Measurement> history = measurements.get(stationId);
        return history == null ? List.of() : List.copyOf(history);
    }

    public Optional<Measurement> findLatestByStationId(String stationId) {
        List<Measurement> history = measurements.get(stationId);
        return history == null || history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }
}
