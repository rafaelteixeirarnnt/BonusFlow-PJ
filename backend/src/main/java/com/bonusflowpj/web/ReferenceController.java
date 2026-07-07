package com.bonusflowpj.web;

import com.bonusflowpj.service.ReferenceService;
import com.bonusflowpj.service.ReferenceService.CepReference;
import com.bonusflowpj.service.ReferenceService.DddDetail;
import com.bonusflowpj.service.ReferenceService.DddReference;
import com.bonusflowpj.service.ReferenceService.DdiReference;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @GetMapping("/ddis")
    public List<DdiReference> ddis() {
        return referenceService.ddis();
    }

    @GetMapping("/ddds")
    public List<DddReference> ddds() {
        return referenceService.ddds();
    }

    @GetMapping("/ddd/{ddd}")
    public DddDetail ddd(@PathVariable String ddd) {
        return referenceService.ddd(ddd);
    }

    @GetMapping("/cep/{cep}")
    public CepReference cep(@PathVariable String cep) {
        return referenceService.cep(cep);
    }
}
