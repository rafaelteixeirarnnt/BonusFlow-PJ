package com.bonusflowpj.web;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.security.CurrentUserService;
import com.bonusflowpj.service.ProfessionalService;
import com.bonusflowpj.web.dto.ProfessionalDtos.CreateProfessionalRequest;
import com.bonusflowpj.web.dto.ProfessionalDtos.ProfessionalStatusRequest;
import com.bonusflowpj.web.dto.ProfessionalDtos.ProfessionalResponse;
import com.bonusflowpj.web.dto.ProfessionalDtos.UpdateProfessionalRequest;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalService professionalService;
    private final CurrentUserService currentUserService;

    public ProfessionalController(ProfessionalService professionalService, CurrentUserService currentUserService) {
        this.professionalService = professionalService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<ProfessionalResponse> list() {
        return professionalService.list().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProfessionalResponse get(@PathVariable Long id) {
        return toResponse(professionalService.find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalResponse create(@Valid @RequestBody CreateProfessionalRequest request) {
        return toResponse(professionalService.create(
            request.name(), request.email(), request.document(), request.team(), request.active()
        ));
    }

    @PutMapping("/{id}")
    public ProfessionalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProfessionalRequest request, HttpServletRequest httpRequest) {
        User currentUser = currentUserService.currentUser();
        return toResponse(professionalService.update(
            id,
            request.name(),
            request.email(),
            request.document(),
            request.team(),
            request.active(),
            request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    public ProfessionalResponse deactivate(@PathVariable Long id, @RequestBody(required = false) ProfessionalStatusRequest request, HttpServletRequest httpRequest) {
        User currentUser = currentUserService.currentUser();
        return toResponse(professionalService.deactivate(
            id,
            request == null ? null : request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        ));
    }

    @PatchMapping("/{id}/activate")
    public ProfessionalResponse activate(@PathVariable Long id, @RequestBody(required = false) ProfessionalStatusRequest request, HttpServletRequest httpRequest) {
        User currentUser = currentUserService.currentUser();
        return toResponse(professionalService.activate(
            id,
            request == null ? null : request.justification(),
            currentUser.getId(),
            RequestIp.resolve(httpRequest)
        ));
    }

    private ProfessionalResponse toResponse(Professional professional) {
        return new ProfessionalResponse(
            professional.getId(),
            professional.getName(),
            professional.getEmail(),
            professional.getDocument(),
            professional.getTeam(),
            professional.isActive(),
            professional.getCreatedAt(),
            professional.getUpdatedAt()
        );
    }
}
