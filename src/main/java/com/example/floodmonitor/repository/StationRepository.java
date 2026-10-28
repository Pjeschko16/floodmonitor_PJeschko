package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Station;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class StationRepository {
    private final ConcurrentHashMap<String, Station> stations = new ConcurrentHashMap<>();

    public void save(Station station) { stations.put(station.getId(), station); }
    public void saveAll(Iterable<Station> values) { values.forEach(this::save); }
    public List<Station> findAll() { return new ArrayList<>(stations.values()); }
    public Optional<Station> findById(String id) { return Optional.ofNullable(stations.get(id)); }
}
