package com.bonusflowpj.web;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.web.dto.ProfessionalDtos.CreateProfessionalRequest;
import com.bonusflowpj.web.dto.ProfessionalDtos.ProfessionalResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessionalResponse create(@Valid @RequestBody CreateProfessionalRequest request) {
        return toResponse(professionalRepository.save(new Professional(
            request.name(), request.email(), request.document(), request.team(), request.active()
        )));
    }

    private ProfessionalResponse toResponse(Professional professional) {
        return new ProfessionalResponse(
            professional.getId(),
            professional.getName(),
            professional.getEmail(),
            professional.getDocument(),
            professional.getTeam(),
            professional.isActive()
        );
    }
}
