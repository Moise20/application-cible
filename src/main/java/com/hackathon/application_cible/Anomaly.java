package com.hackathon.application_cible;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity // Indique que c'est une entité JPA (sera mappée à une table de BDD)
@Data // Génère getters, setters, toString, equals, hashCode
@NoArgsConstructor // Génère un constructeur sans arguments
@AllArgsConstructor // Génère un constructeur avec tous les arguments
public class Anomaly {
    @Id // Indique que c'est la clé primaire
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Génération automatique de l'ID
    private Long id;
    private LocalDateTime timestamp;
    private String type; // Ex: "Force Brute", "Scan de Vulnérabilité"
    private String sourceIp;
    private String details; // Informations supplémentaires sur l'anomalie
    private boolean resolved; // Pourrait être utilisé pour marquer une anomalie comme traitée
}