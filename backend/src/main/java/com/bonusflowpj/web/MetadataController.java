package com.bonusflowpj.web;

import com.bonusflowpj.domain.AbsenceStatus;
import com.bonusflowpj.domain.AbsenceType;
import com.bonusflowpj.domain.UserRole;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    @GetMapping
    public Map<String, Object> metadata() {
        return Map.of(
            "absenceTypes", AbsenceType.values(),
            "absenceStatuses", AbsenceStatus.values(),
            "userRoles", UserRole.values()
        );
    }
}
