package com.sni.bokaticowork.features.ressource.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "resource_type")
public class ResourceType {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active")
    private Boolean active;

    /**
     * Maximum concurrent bookings allowed for resources of this type.
     * Null = use Resource.capacity (default behaviour for most types).
     * Set to 1 for exclusive-use resources (e.g. meeting rooms: RTY-001) where
     * capacity describes seating but only one group may book at a time.
     */
    @Column(name = "bookable_slots")
    private Integer bookableSlots;

    /**
     * La location occupe-t-elle la ressource entiere.
     *
     * <p>Vrai pour une salle, un bureau, une cabine · on loue la piece. La quantite d'une
     * reservation y designe alors le nombre de participants, elle ne consomme aucune place et ne
     * multiplie pas le prix.</p>
     *
     * <p>Faux pour un open space, un parking, un casier · on y prend des places, une a la fois, et
     * la quantite garde tout son sens.</p>
     */
    @Column(name = "whole_resource_booking", nullable = false)
    @Builder.Default
    private Boolean wholeResourceBooking = Boolean.FALSE;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

}
