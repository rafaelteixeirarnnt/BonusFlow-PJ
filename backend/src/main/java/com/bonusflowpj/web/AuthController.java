package com.bonusflowpj.web;

import com.bonusflowpj.domain.User;
import com.bonusflowpj.security.JwtService;
import com.bonusflowpj.service.UserService;
import com.bonusflowpj.web.dto.AuthDtos.AuthUserResponse;
import com.bonusflowpj.web.dto.AuthDtos.LoginRequest;
import com.bonusflowpj.web.dto.AuthDtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        return new LoginResponse(jwtService.generate(user), toResponse(user));
    }

    private AuthUserResponse toResponse(User user) {
        Long professionalId = user.getProfessional() == null ? null : user.getProfessional().getId();
        return new AuthUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            professionalId,
            user.isSystemUser(),
            user.getLastLoginAt()
        );
    }
}
