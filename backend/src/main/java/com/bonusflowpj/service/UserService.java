package com.bonusflowpj.service;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    public static final String SUPER_ADMIN_EMAIL = "admin@bonusflow.com";
    private static final String SUPER_ADMIN_INITIAL_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final ProfessionalRepository professionalRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessEmailService accessEmailService;

    public UserService(
        UserRepository userRepository,
        ProfessionalRepository professionalRepository,
        PasswordEncoder passwordEncoder,
        AccessEmailService accessEmailService
    ) {
        this.userRepository = userRepository;
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessEmailService = accessEmailService;
    }

    @Transactional
    public User ensureSuperAdmin() {
        return userRepository.findByEmailIgnoreCase(SUPER_ADMIN_EMAIL)
            .orElseGet(() -> userRepository.save(new User(
                "Super Admin",
                SUPER_ADMIN_EMAIL,
                passwordEncoder.encode(SUPER_ADMIN_INITIAL_PASSWORD),
                UserRole.SUPER_ADMIN,
                true,
                null,
                true
            )));
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User find(Long id) {
        return userRepository.findWithProfessionalById(id)
            .orElseThrow(() -> new BusinessRuleException("Usuario nao encontrado."));
    }

    @Transactional
    public User create(CreateUserCommand command) {
        validateEmailAvailable(command.email(), null);
        Professional professional = resolveProfessional(command.role(), command.professionalId());
        if (professional != null && userRepository.existsByProfessionalId(professional.getId())) {
            throw new BusinessRuleException("Este profissional ja possui usuario vinculado.");
        }

        String rawPassword = command.rawPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateInitialPassword();
        }

        User user = userRepository.save(new User(
            command.name(),
            command.email(),
            passwordEncoder.encode(rawPassword),
            command.role(),
            command.active(),
            professional,
            false
        ));
        if (professional != null) {
            accessEmailService.sendInitialAccess(professional, user, rawPassword);
        }
        return user;
    }

    @Transactional
    public User update(Long id, UpdateUserCommand command) {
        User user = find(id);
        validateMutableSystemUser(user, command.active());
        validateEmailAvailable(command.email(), id);
        Professional professional = resolveProfessional(command.role(), command.professionalId());
        if (professional != null && (user.getProfessional() == null || !professional.getId().equals(user.getProfessional().getId()))
            && userRepository.existsByProfessionalId(professional.getId())) {
            throw new BusinessRuleException("Este profissional ja possui usuario vinculado.");
        }
        user.update(command.name(), command.email(), command.role(), professional, command.active());
        return user;
    }

    @Transactional
    public User deactivate(Long id) {
        User user = find(id);
        validateMutableSystemUser(user, false);
        user.deactivate();
        return user;
    }

    @Transactional
    public User activate(Long id) {
        User user = find(id);
        user.activate();
        return user;
    }

    @Transactional
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findWithProfessionalByEmailIgnoreCase(email)
            .orElseThrow(() -> new BusinessRuleException("Credenciais invalidas."));
        if (!user.isActive()) {
            throw new BusinessRuleException("Usuario inativo nao pode acessar o sistema.");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessRuleException("Credenciais invalidas.");
        }
        user.registerLogin();
        return user;
    }

    private void validateEmailAvailable(String email, Long currentId) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new BusinessRuleException("Ja existe usuario com este e-mail.");
            }
        });
    }

    private Professional resolveProfessional(UserRole role, Long professionalId) {
        if (role == UserRole.SUPER_ADMIN) {
            return null;
        }
        if (professionalId == null) {
            throw new BusinessRuleException("Usuario deve estar vinculado a um profissional.");
        }
        return professionalRepository.findById(professionalId)
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
    }

    private void validateMutableSystemUser(User user, boolean targetActive) {
        if (user.isSystemUser() && !targetActive) {
            throw new BusinessRuleException("Usuarios de sistema nao podem ser inativados.");
        }
    }

    private String generateInitialPassword() {
        return "Temp@" + Long.toUnsignedString(System.nanoTime(), 36);
    }
}
