package com.bonusflowpj.web;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.service.BusinessRuleException;
import com.bonusflowpj.web.dto.ProfessionalDtos.CreateProfessionalRequest;
import com.bonusflowpj.web.dto.ProfessionalDtos.ProfessionalResponse;
import com.bonusflowpj.web.dto.ProfessionalDtos.UpdateProfessionalRequest;
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

    private final ProfessionalRepository professionalRepository;

    public ProfessionalController(ProfessionalRepository professionalRepository) {
        this.professionalRepository = professionalRepository;
    }

    @GetMapping
    public List<ProfessionalResponse> list() {
        return professionalRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProfessionalResponse get(@PathVariable Long id) {
        return toResponse(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalResponse create(@Valid @RequestBody CreateProfessionalRequest request) {
        return toResponse(professionalRepository.save(new Professional(
            request.name(), request.email(), request.document(), request.team(), request.active()
        )));
    }

    @PutMapping("/{id}")
    public ProfessionalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateProfessionalRequest request) {
        Professional professional = find(id);
        professional.update(request.name(), request.email(), request.document(), request.team(), request.active());
        return toResponse(professionalRepository.save(professional));
    }

    @PatchMapping("/{id}/deactivate")
    public ProfessionalResponse deactivate(@PathVariable Long id) {
        Professional professional = find(id);
        professional.deactivate();
        return toResponse(professionalRepository.save(professional));
    }

    private Professional find(Long id) {
        return professionalRepository.findById(id)
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
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
