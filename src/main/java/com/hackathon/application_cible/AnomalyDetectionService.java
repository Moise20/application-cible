package com.hackathon.application_cible;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service // Indique que c'est un composant de service Spring
public class AnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);
    private static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Value("${logging.file.name}") // Récupère le nom du fichier de log depuis application.properties
    private String logFilePath;

    private final AnomalyRepository anomalyRepository;

    // Map pour stocker les tentatives de connexion échouées par IP et l'heure de la première tentative
    private final Map<String, LoginAttemptTracker> failedLoginAttempts = new HashMap<>();

    public AnomalyDetectionService(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    // Méthode pour déclencher la détection d'anomalies
    public void detectAnomalies() {
        logger.info("Début de la détection d'anomalies à partir du fichier de log : {}", logFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LogEntry logEntry = parseLogLine(line);
                if (logEntry != null) {
                    applyDetectionRules(logEntry);
                }
            }
            logger.info("Fin de la détection d'anomalies.");
        } catch (IOException e) {
            logger.error("Erreur lors de la lecture du fichier de log : {}", e.getMessage());
        }
    }

    // Parse une ligne de log en objet LogEntry
    private LogEntry parseLogLine(String line) {
        // Exemple de ligne de log : 2023-10-26 10:30:00.123 INFO [main] com.hackathon.applicationcible.AuthController - LOGIN_FAILED - User: wronguser - Password: wrongpass - IP: 192.168.1.100
        // Pattern pour capturer la date, le niveau, le message, et les détails spécifiques (User, IP, EventType)
        Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\S+)\\s+.*\\[com\\.hackathon\\.applicationcible\\.AuthController\\]\\s+-\\s+(\\S+)\\s+-\\s+(.*)");
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            try {
                LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), LOG_DATE_FORMATTER);
                String level = matcher.group(2);
                String eventType = matcher.group(3); // LOGIN_SUCCESS, LOGIN_FAILED, ACCESS_PUBLIC, ACCESS_SECRET_UNAUTHORIZED
                String details = matcher.group(4);

                String user = null;
                String ip = null;

                // Extraire l'utilisateur et l'IP des détails
                Pattern userIpPattern = Pattern.compile("User: (\\S+) - IP: (\\S+)");
                Matcher userIpMatcher = userIpPattern.matcher(details);
                if (userIpMatcher.find()) {
                    user = userIpMatcher.group(1);
                    ip = userIpMatcher.group(2);
                } else {
                    // Si pas de user/password, juste IP (pour public/secret)
                    Pattern ipPattern = Pattern.compile("IP: (\\S+)");
                    Matcher ipMatcher = ipPattern.matcher(details);
                    if (ipMatcher.find()) {
                        ip = ipMatcher.group(1);
                    }
                }
                
                return new LogEntry(timestamp, level, line, user, ip, eventType); // Utilise la ligne complète comme message
            } catch (Exception e) {
                logger.warn("Impossible de parser la ligne de log : {} - Erreur : {}", line, e.getMessage());
                return null;
            }
        }
        return null;
    }

    // Applique les règles de détection aux entrées de log
    private void applyDetectionRules(LogEntry logEntry) {
        if (logEntry.getIpAddress() == null) {
            return; // Pas d'IP, pas de détection basée sur l'IP
        }

        // Règle 1: Détection de force brute (tentatives de connexion échouées)
        if ("LOGIN_FAILED".equals(logEntry.getEventType())) {
            String ip = logEntry.getIpAddress();
            failedLoginAttempts.putIfAbsent(ip, new LoginAttemptTracker());
            LoginAttemptTracker tracker = failedLoginAttempts.get(ip);
            tracker.addAttempt(logEntry.getTimestamp());

            // Si plus de 5 tentatives en 60 secondes
            if (tracker.getFailedAttemptsInLastMinute() >= 5) {
                // Détecter l'anomalie une seule fois par IP pour cette fenêtre
                if (!tracker.isAnomalyDetected()) {
                    Anomaly anomaly = new Anomaly(null, logEntry.getTimestamp(), "Force Brute", ip,
                            "Plus de 5 tentatives de connexion échouées en 60 secondes pour l'IP: " + ip, false);
                    anomalyRepository.save(anomaly);
                    logger.warn("ANOMALIE DÉTECTÉE: Force Brute - IP: {}", ip);
                    tracker.setAnomalyDetected(true); // Marquer l'anomalie comme détectée pour cette fenêtre
                }
            }
        }

        // Règle 2: Détection de scan de chemins invalides (tentatives d'accès à des URLs non existantes/sensibles)
        // Nous allons nous baser sur le log ACCESS_SECRET_UNAUTHORIZED pour simplifier
        if ("ACCESS_SECRET_UNAUTHORIZED".equals(logEntry.getEventType())) {
            String ip = logEntry.getIpAddress();
            // Pour un hackathon, on peut détecter chaque tentative comme une anomalie de scan
            // Une version plus avancée compterait les tentatives multiples depuis la même IP
            Anomaly anomaly = new Anomaly(null, logEntry.getTimestamp(), "Scan de Vulnérabilité", ip,
                    "Tentative d'accès non autorisé à une ressource secrète depuis l'IP: " + ip + ". Log: " + logEntry.getMessage(), false);
            anomalyRepository.save(anomaly);
            logger.warn("ANOMALIE DÉTECTÉE: Scan de Vulnérabilité - IP: {}", ip);
        }

        // Nettoyer les tentatives de connexion échouées obsolètes
        failedLoginAttempts.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    // Classe interne pour suivre les tentatives de connexion par IP
    private static class LoginAttemptTracker {
        private final Map<LocalDateTime, Integer> attempts = new HashMap<>(); // Timestamp -> count
        private LocalDateTime firstAttemptTime;
        private boolean anomalyDetected; // Pour éviter de spammer les anomalies pour la même fenêtre

        public LoginAttemptTracker() {
            this.anomalyDetected = false;
        }

        public void addAttempt(LocalDateTime timestamp) {
            if (firstAttemptTime == null || timestamp.isAfter(firstAttemptTime.plusMinutes(1))) {
                // Réinitialiser si la fenêtre de 60 secondes est passée
                attempts.clear();
                firstAttemptTime = timestamp;
                anomalyDetected = false; // Réinitialiser le drapeau de détection
            }
            attempts.put(timestamp, attempts.getOrDefault(timestamp, 0) + 1);
        }

        public int getFailedAttemptsInLastMinute() {
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            return attempts.entrySet().stream()
                    .filter(entry -> entry.getKey().isAfter(oneMinuteAgo))
                    .mapToInt(Map.Entry::getValue)
                    .sum();
        }

        public boolean isExpired() {
            return firstAttemptTime != null && LocalDateTime.now().isAfter(firstAttemptTime.plusMinutes(1));
        }

        public boolean isAnomalyDetected() {
            return anomalyDetected;
        }

        public void setAnomalyDetected(boolean anomalyDetected) {
            this.anomalyDetected = anomalyDetected;
        }
    }
}