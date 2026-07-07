package com.sni.bokaticowork.features.event.repository;

import com.sni.bokaticowork.features.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByCodeIgnoreCase(String code);
}
