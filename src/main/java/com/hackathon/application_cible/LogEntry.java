package com.hackathon.application_cible;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data // Génère getters, setters, toString, equals, hashCode
@NoArgsConstructor // Génère un constructeur sans arguments
@AllArgsConstructor // Génère un constructeur avec tous les arguments
public class LogEntry {
    private LocalDateTime timestamp;
    private String level; // INFO, WARN, ERROR
    private String message;
    private String user; // Peut être null
    private String ipAddress; // Adresse IP du client
    private String eventType; // LOGIN_SUCCESS, LOGIN_FAILED, ACCESS_PUBLIC, ACCESS_SECRET_UNAUTHORIZED
}
