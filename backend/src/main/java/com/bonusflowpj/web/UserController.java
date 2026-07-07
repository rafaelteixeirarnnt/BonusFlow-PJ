package com.bonusflowpj.web;

import com.bonusflowpj.security.CurrentUserService;
import com.bonusflowpj.service.UserService;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
import com.bonusflowpj.web.dto.UserDtos.CreateUserRequest;
import com.bonusflowpj.web.dto.UserDtos.LinkProfessionalRequest;
import com.bonusflowpj.web.dto.UserDtos.PageResponse;
import com.bonusflowpj.web.dto.UserDtos.UserStatusRequest;
import com.bonusflowpj.web.dto.UserDtos.UpdateUserRequest;
import com.bonusflowpj.web.dto.UserDtos.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
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
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listResponses();
    }

    @GetMapping("/page")
    public PageResponse<UserResponse> listPage(Pageable pageable) {
        return userService.listResponses(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.findResponse(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.createResponse(new CreateUserCommand(
            request.fullName(),
            request.cpf(),
            request.birthDate(),
            request.motherName(),
            request.fatherName(),
            request.email(),
            null,
            request.role(),
            request.professionalId(),
            true,
            request.contacts(),
            request.address(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        ));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.updateResponse(id, new UpdateUserCommand(
            request.fullName(),
            request.cpf(),
            request.birthDate(),
            request.motherName(),
            request.fatherName(),
            request.email(),
            request.role(),
            request.professionalId(),
            request.active(),
            request.contacts(),
            request.address(),
            request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id, @RequestBody(required = false) UserStatusRequest request, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.deactivateResponse(
            id,
            request == null ? null : request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        );
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id, @RequestBody(required = false) UserStatusRequest request, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.activateResponse(
            id,
            request == null ? null : request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        );
    }

    @PatchMapping("/{id}/link-professional")
    public UserResponse linkProfessional(@PathVariable Long id, @Valid @RequestBody LinkProfessionalRequest request, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.linkProfessionalResponse(
            id,
            request.professionalId(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        );
    }

    @PatchMapping("/{id}/unlink-professional")
    public UserResponse unlinkProfessional(@PathVariable Long id, HttpServletRequest httpRequest) {
        var currentUser = currentUserService.currentUser();
        return userService.unlinkProfessionalResponse(
            id,
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        );
    }
}
