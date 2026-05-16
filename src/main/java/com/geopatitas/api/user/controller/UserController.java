package com.geopatitas.api.user.controller;

import com.geopatitas.api.user.entity.User;
import com.geopatitas.api.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("error", "No autorizado"));
        }

        String email = authentication.getName(); // En JwtAuthenticationFilter pusimos el email como username
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("nombre", user.getNombre());
            response.put("email", user.getEmail());
            response.put("rol", user.getRol());
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(404).body(Map.of("error", "Usuario no encontrado"));
    }
}
