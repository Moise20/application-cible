package com.hackathon.application_cible;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Indique que c'est un composant de persistance
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
    // Spring Data JPA génère automatiquement les méthodes CRUD (Create, Read, Update, Delete)
    // Vous pouvez ajouter des méthodes personnalisées ici si besoin, ex: findBySourceIp
}