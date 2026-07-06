package com.bonusflowpj.web;

import com.bonusflowpj.domain.ContractRule;
import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.repository.ContractRuleRepository;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.service.BusinessRuleException;
import com.bonusflowpj.web.dto.ContractRuleDtos.ContractRuleResponse;
import com.bonusflowpj.web.dto.ContractRuleDtos.CreateContractRuleRequest;
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
@RequestMapping("/api/contract-rules")
public class ContractRuleController {

    private final ContractRuleRepository contractRuleRepository;
    private final ProfessionalRepository professionalRepository;

    public ContractRuleController(ContractRuleRepository contractRuleRepository, ProfessionalRepository professionalRepository) {
        this.contractRuleRepository = contractRuleRepository;
        this.professionalRepository = professionalRepository;
    }

    @GetMapping
    public List<ContractRuleResponse> list() {
        return contractRuleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractRuleResponse create(@Valid @RequestBody CreateContractRuleRequest request) {
        if (request.validTo() != null && request.validTo().isBefore(request.validFrom())) {
            throw new BusinessRuleException("Vigencia final nao pode ser menor que vigencia inicial.");
        }
        Professional professional = professionalRepository.findById(request.professionalId())
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
        return toResponse(contractRuleRepository.save(new ContractRule(
            professional,
            request.absenceType(),
            request.daysAllowed(),
            request.validFrom(),
            request.validTo()
        )));
    }

    private ContractRuleResponse toResponse(ContractRule rule) {
        return new ContractRuleResponse(
            rule.getId(),
            rule.getProfessional().getId(),
            rule.getProfessional().getName(),
            rule.getAbsenceType(),
            rule.getDaysAllowed(),
            rule.getValidFrom(),
            rule.getValidTo()
        );
    }
}
