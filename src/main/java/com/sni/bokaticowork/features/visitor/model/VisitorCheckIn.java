package com.sni.bokaticowork.features.visitor.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "visitor_check_in")
public class VisitorCheckIn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pass_id", nullable = false)
    private VisitorPass pass;
    private Instant checkedInAt;
    private Instant checkedOutAt;
    private String checkInAgent;
    private String checkOutAgent;
    private String notes;
}
