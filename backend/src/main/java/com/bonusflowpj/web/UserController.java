package com.bonusflowpj.web;

import com.bonusflowpj.domain.User;
import com.bonusflowpj.service.UserService;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
import com.bonusflowpj.web.dto.UserDtos.CreateUserRequest;
import com.bonusflowpj.web.dto.UserDtos.UpdateUserRequest;
import com.bonusflowpj.web.dto.UserDtos.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return toResponse(userService.find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return toResponse(userService.create(new CreateUserCommand(
            request.name(),
            request.email(),
            request.password(),
            request.role(),
            request.professionalId(),
            request.active()
        )));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return toResponse(userService.update(id, new UpdateUserCommand(
            request.name(),
            request.email(),
            request.role(),
            request.professionalId(),
            request.active()
        )));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        return toResponse(userService.deactivate(id));
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return toResponse(userService.activate(id));
    }

    private UserResponse toResponse(User user) {
        Long professionalId = user.getProfessional() == null ? null : user.getProfessional().getId();
        String professionalName = user.getProfessional() == null ? null : user.getProfessional().getName();
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            professionalId,
            professionalName,
            user.isSystemUser(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getLastLoginAt()
        );
    }
}
