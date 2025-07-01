package com.hackathon.application_cible;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest; // Pour récupérer l'IP du client

@RestController // Indique que c'est un contrôleur REST
@RequestMapping("/api") // Tous les endpoints de ce contrôleur commenceront par /api
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    // Endpoint pour la connexion
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        String clientIp = request.getRemoteAddr(); // Récupère l'adresse IP du client

        // Logique de connexion simplifiée pour la démo
        if ("user".equals(username) && "password".equals(password)) {
            logger.info("LOGIN_SUCCESS - User: {} - IP: {}", username, clientIp);
            return ResponseEntity.ok("Connexion réussie pour " + username);
        } else {
            logger.warn("LOGIN_FAILED - User: {} - Password: {} - IP: {}", username, password, clientIp);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiants incorrects pour " + username);
        }
    }

    // Endpoint public
    @GetMapping("/public")
    public ResponseEntity<String> publicContent(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        logger.info("ACCESS_PUBLIC - IP: {}", clientIp);
        return ResponseEntity.ok("Ceci est un contenu public.");
    }

    // Endpoint sensible (simule une ressource protégée)
    @GetMapping("/secret")
    public ResponseEntity<String> secretContent(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        // Pour la démo, nous allons simuler un accès non autorisé si l'utilisateur n'est pas "admin"
        // En vrai, il y aurait une authentification/autorisation plus robuste ici.
        logger.warn("ACCESS_SECRET_UNAUTHORIZED - IP: {}", clientIp);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé. Contenu secret.");
    }
}
