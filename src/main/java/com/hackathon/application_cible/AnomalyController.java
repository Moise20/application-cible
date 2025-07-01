package com.hackathon.application_cible;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    private final AnomalyDetectionService anomalyDetectionService;
    private final AnomalyRepository anomalyRepository;

    public AnomalyController(AnomalyDetectionService anomalyDetectionService, AnomalyRepository anomalyRepository) {
        this.anomalyDetectionService = anomalyDetectionService;
        this.anomalyRepository = anomalyRepository;
    }

    // Endpoint pour déclencher la détection d'anomalies manuellement
    @PostMapping("/detect")
    public ResponseEntity<String> triggerDetection() {
        anomalyDetectionService.detectAnomalies();
        return ResponseEntity.ok("Détection d'anomalies déclenchée. Vérifiez les logs et les anomalies stockées.");
    }

    // Endpoint pour récupérer toutes les anomalies détectées
    @GetMapping
    public ResponseEntity<List<Anomaly>> getAllAnomalies() {
        List<Anomaly> anomalies = anomalyRepository.findAll();
        return ResponseEntity.ok(anomalies);
    }
}
