package com.geopatitas.api.user.controller;

import com.geopatitas.api.security.JwtUtil;
import com.geopatitas.api.user.entity.User;
import com.geopatitas.api.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User requestUser) {
        java.util.Optional<User> existingUser = userRepository.findByEmail(requestUser.getEmail());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if ("GUEST".equals(user.getRol())) {
                // Reclamar cuenta fantasma
                user.setNombre(requestUser.getNombre());
                user.setPassword(passwordEncoder.encode(requestUser.getPassword()));
                user.setRol("USER");
                userRepository.save(user);
                
                String token = jwtUtil.generateToken(user.getEmail());
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "El email ya está registrado"));
        }

        User newUser = new User();
        newUser.setNombre(requestUser.getNombre());
        newUser.setEmail(requestUser.getEmail());
        newUser.setPassword(passwordEncoder.encode(requestUser.getPassword()));
        newUser.setRol("USER");

        userRepository.save(newUser);

        String token = jwtUtil.generateToken(newUser.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(credentials.get("email"), credentials.get("password"))
        );

        User user = userRepository.findByEmail(credentials.get("email")).orElseThrow();
        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user_id", user.getId());
        response.put("nombre", user.getNombre());
        return ResponseEntity.ok(response);
    }
}
