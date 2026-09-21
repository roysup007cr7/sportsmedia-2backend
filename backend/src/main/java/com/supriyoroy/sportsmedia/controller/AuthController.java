package com.supriyoroy.sportsmedia.controller;

import com.supriyoroy.sportsmedia.dto.Dtos.*;
import com.supriyoroy.sportsmedia.repo.AdminUserRepository;
import com.supriyoroy.sportsmedia.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminUserRepository admins;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var user = admins.findByUsername(req.username()).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !encoder.matches(req.password(), user.getPasswordHash())) {
            return ResponseEntity.status(401)
                    .body(new ApiMessage(false, "Username or password is wrong."));
        }
        user.setLastLogin(Instant.now());
        admins.save(user);
        return ResponseEntity.ok(new LoginResponse(
                jwt.issue(user.getUsername()), user.getDisplayName(), jwt.expirySeconds()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String username,
                                            @RequestParam String currentPassword,
                                            @RequestParam String newPassword) {
        var user = admins.findByUsername(username).orElse(null);
        if (user == null || !encoder.matches(currentPassword, user.getPasswordHash())) {
            return ResponseEntity.status(401).body(new ApiMessage(false, "Current password is wrong."));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.badRequest().body(new ApiMessage(false, "Use at least 8 characters."));
        }
        user.setPasswordHash(encoder.encode(newPassword));
        admins.save(user);
        return ResponseEntity.ok(new ApiMessage(true, "Password changed."));
    }
}
