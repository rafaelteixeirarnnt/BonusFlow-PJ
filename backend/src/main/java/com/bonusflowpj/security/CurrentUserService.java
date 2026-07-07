package com.bonusflowpj.security;

import com.bonusflowpj.domain.User;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.BusinessRuleException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessRuleException("Usuario autenticado nao encontrado.");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new BusinessRuleException("Usuario autenticado nao encontrado."));
    }
}
