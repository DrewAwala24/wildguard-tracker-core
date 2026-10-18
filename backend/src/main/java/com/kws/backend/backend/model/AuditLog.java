package com.kws.backend.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "action_type", nullable = false)
    private String actionType; // USER_LOGIN, ANIMAL_REGISTERED, ANIMAL_UPDATED, PATROL_DISPATCHED, INCIDENT_RESOLVED, etc.

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "ip_address")
    private String ipAddress = "127.0.0.1";

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
