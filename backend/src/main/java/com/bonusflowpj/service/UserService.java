package com.bonusflowpj.service;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.ContactType;
import com.bonusflowpj.domain.User;
import com.bonusflowpj.domain.UserAddress;
import com.bonusflowpj.domain.UserContact;
import com.bonusflowpj.domain.UserRole;
import com.bonusflowpj.domain.AuditAction;
import com.bonusflowpj.repository.ProfessionalRepository;
import com.bonusflowpj.repository.UserRepository;
import com.bonusflowpj.service.dto.CreateUserCommand;
import com.bonusflowpj.service.dto.UpdateUserCommand;
import com.bonusflowpj.web.dto.UserDtos.AddressRequest;
import com.bonusflowpj.web.dto.UserDtos.AddressResponse;
import com.bonusflowpj.web.dto.UserDtos.ContactRequest;
import com.bonusflowpj.web.dto.UserDtos.ContactResponse;
import com.bonusflowpj.web.dto.UserDtos.PageResponse;
import com.bonusflowpj.web.dto.UserDtos.UserResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final AuditLogService auditLogService;

    public UserService(
        UserRepository userRepository,
        ProfessionalRepository professionalRepository,
        PasswordEncoder passwordEncoder,
        AccessEmailService accessEmailService,
        AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.professionalRepository = professionalRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessEmailService = accessEmailService;
        this.auditLogService = auditLogService;
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
    public List<UserResponse> listResponses() {
        return userRepository.findAllForListing().stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listResponses(Pageable pageable) {
        Page<Long> idPage = userRepository.findIdsForListing(pageable);
        if (idPage.isEmpty()) {
            return new PageResponse<>(List.of(), idPage.getTotalElements(), idPage.getTotalPages(), idPage.getNumber(), idPage.getSize());
        }
        List<Long> ids = idPage.getContent();
        Map<Long, User> usersById = new LinkedHashMap<>();
        userRepository.findAllForListingByIdIn(ids).forEach(user -> usersById.put(user.getId(), user));
        List<UserResponse> content = ids.stream()
            .map(usersById::get)
            .map(this::toResponse)
            .toList();
        return new PageResponse<>(content, idPage.getTotalElements(), idPage.getTotalPages(), idPage.getNumber(), idPage.getSize());
    }

    @Transactional(readOnly = true)
    public User find(Long id) {
        return userRepository.findWithProfessionalById(id)
            .orElseThrow(() -> new BusinessRuleException("Usuario nao encontrado."));
    }

    @Transactional(readOnly = true)
    public UserResponse findResponse(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public UserResponse createResponse(CreateUserCommand command) {
        return toResponse(create(command));
    }

    @Transactional
    public UserResponse updateResponse(Long id, UpdateUserCommand command) {
        return toResponse(update(id, command));
    }

    @Transactional
    public UserResponse deactivateResponse(Long id, String justification, Long performedByUserId, String ipAddress) {
        return toResponse(deactivate(id, justification, performedByUserId, ipAddress));
    }

    @Transactional
    public UserResponse activateResponse(Long id, String justification, Long performedByUserId, String ipAddress) {
        return toResponse(activate(id, justification, performedByUserId, ipAddress));
    }

    @Transactional
    public UserResponse linkProfessionalResponse(Long id, Long professionalId, Long performedByUserId, String ipAddress) {
        return toResponse(linkProfessional(id, professionalId, performedByUserId, ipAddress));
    }

    @Transactional
    public UserResponse unlinkProfessionalResponse(Long id, Long performedByUserId, String ipAddress) {
        return toResponse(unlinkProfessional(id, performedByUserId, ipAddress));
    }

    @Transactional
    public User create(CreateUserCommand command) {
        User performedBy = resolvePerformedBy(command.performedByUserId());
        validateCreateFields(command);
        validateRoleGrant(performedBy, null, command.role(), command.performedByUserId(), command.ipAddress());
        validateEmailAvailable(command.email(), null);
        validateCpfAvailable(CpfValidator.digitsOnly(command.cpf()), null);
        Professional professional = resolveOptionalProfessional(command.role(), command.professionalId());
        validateProfessionalLinkAvailable(professional);

        String rawPassword = command.rawPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            rawPassword = generateInitialPassword();
        }

        User user = userRepository.save(new User(
            command.name(),
            CpfValidator.digitsOnly(command.cpf()),
            command.birthDate(),
            command.motherName(),
            command.fatherName(),
            command.email(),
            passwordEncoder.encode(rawPassword),
            command.role(),
            true,
            professional,
            false,
            toContacts(command.contacts()),
            toAddress(command.address())
        ));
        if (professional != null) {
            accessEmailService.sendInitialAccess(professional, user, rawPassword);
        }
        if (performedBy != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_CREATED,
                null,
                user.getRole().name(),
                "Criacao de usuario",
                performedBy.getId(),
                command.ipAddress()
            );
        }
        return user;
    }

    @Transactional
    public User update(Long id, UpdateUserCommand command) {
        User user = find(id);
        User performedBy = resolvePerformedBy(command.performedByUserId());
        String nextCpf = command.cpf() == null ? user.getCpf() : CpfValidator.digitsOnly(command.cpf());
        LocalDate nextBirthDate = command.birthDate() == null ? user.getBirthDate() : command.birthDate();
        String nextMotherName = command.motherName() == null ? user.getMotherName() : command.motherName();
        String nextFatherName = command.fatherName() == null ? user.getFatherName() : command.fatherName();
        List<UserContact> nextContacts = command.contacts() == null ? user.getContacts() : toContacts(command.contacts());
        UserAddress nextAddress = command.address() == null ? user.getAddress() : toAddress(command.address());
        validateUpdateFields(command, nextCpf, nextBirthDate, nextMotherName);
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(command.email());
        boolean cpfChanged = nextCpf != null && (user.getCpf() == null || !user.getCpf().equals(nextCpf));
        boolean roleChanged = user.getRole() != command.role();
        if (emailChanged) {
            validateEmailChangeAllowed(performedBy);
            validateEmailAvailable(command.email(), id);
        }
        if (cpfChanged) {
            validateCpfChangeAllowed(performedBy);
            validateCpfAvailable(nextCpf, id);
        }
        if (user.getRole() == UserRole.SUPER_ADMIN && performedBy != null && performedBy.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessRuleException("Apenas SUPER_ADMIN pode alterar dados de outro SUPER_ADMIN.");
        }
        if (roleChanged) {
            validateRoleGrant(performedBy, user, command.role(), command.performedByUserId(), command.ipAddress());
        }
        if (emailChanged || roleChanged || cpfChanged) {
            auditLogService.requireJustification(command.justification());
        }
        String previousEmail = user.getEmail();
        String previousCpf = user.getCpf();
        UserRole previousRole = user.getRole();
        user.updateProfile(
            command.name(),
            nextCpf,
            nextBirthDate,
            nextMotherName,
            nextFatherName,
            emailChanged ? command.email() : user.getEmail(),
            command.role(),
            user.getProfessional(),
            command.active(),
            nextContacts,
            nextAddress
        );
        if (emailChanged) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.CHANGE_USER_EMAIL,
                previousEmail,
                user.getEmail(),
                command.justification(),
                command.performedByUserId(),
                command.ipAddress()
            );
        }
        if (roleChanged) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_ROLE_CHANGED,
                previousRole.name(),
                user.getRole().name(),
                command.justification(),
                command.performedByUserId(),
                command.ipAddress()
            );
        }
        if (cpfChanged) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.PROFESSIONAL_DOCUMENT_CHANGED,
                previousCpf,
                user.getCpf(),
                command.justification(),
                command.performedByUserId(),
                command.ipAddress()
            );
        }
        return user;
    }

    @Transactional
    public User linkProfessional(Long id, Long professionalId, Long performedByUserId, String ipAddress) {
        User user = find(id);
        if (user.getProfessional() != null) {
            throw new BusinessRuleException("Usuário já possui profissional vinculado.");
        }
        Professional professional = professionalRepository.findById(professionalId)
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
        if (!professional.isActive()) {
            throw new BusinessRuleException("Não é permitido vincular profissional inativo.");
        }
        validateProfessionalLinkAvailable(professional);
        user.linkProfessional(professional);
        if (performedByUserId != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_PROFESSIONAL_LINKED,
                null,
                String.valueOf(professional.getId()),
                "Vinculo de profissional ao usuario",
                performedByUserId,
                ipAddress
            );
        }
        return user;
    }

    @Transactional
    public User unlinkProfessional(Long id, Long performedByUserId, String ipAddress) {
        User user = find(id);
        Long previousProfessionalId = user.getProfessional() == null ? null : user.getProfessional().getId();
        user.unlinkProfessional();
        if (performedByUserId != null && previousProfessionalId != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_PROFESSIONAL_UNLINKED,
                String.valueOf(previousProfessionalId),
                null,
                "Remocao de vinculo de profissional do usuario",
                performedByUserId,
                ipAddress
            );
        }
        return user;
    }

    @Transactional
    public User deactivate(Long id) {
        return deactivate(id, "Inativacao administrativa", null, null);
    }

    @Transactional
    public User deactivate(Long id, String justification, Long performedByUserId, String ipAddress) {
        User user = find(id);
        if (user.isSystemUser() || user.getRole() == UserRole.SUPER_ADMIN) {
            recordBlockedSystemUserDeactivation(user, false, justification, performedByUserId, ipAddress);
            validateMutableSystemUser(user, false);
        }
        auditLogService.requireJustification(justification);
        user.deactivate();
        if (performedByUserId != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_DEACTIVATED,
                "true",
                "false",
                justification,
                performedByUserId,
                ipAddress
            );
        }
        return user;
    }

    @Transactional
    public User activate(Long id) {
        return activate(id, "Reativacao administrativa", null, null);
    }

    @Transactional
    public User activate(Long id, String justification, Long performedByUserId, String ipAddress) {
        User user = find(id);
        auditLogService.requireJustification(justification);
        user.activate();
        if (performedByUserId != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_REACTIVATED,
                "false",
                "true",
                justification,
                performedByUserId,
                ipAddress
            );
        }
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
        if (user.getRole() != UserRole.SUPER_ADMIN && user.getProfessional() == null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.USER_LOGIN_WITHOUT_PROFESSIONAL_BLOCKED,
                null,
                null,
                "Tentativa de login sem profissional vinculado",
                user.getId(),
                null
            );
            throw new BusinessRuleException("Usuário sem profissional vinculado. Entre em contato com o administrador do sistema.");
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

    private void validateCpfAvailable(String cpf, Long currentId) {
        userRepository.findByCpf(cpf).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                Map<String, List<String>> errors = new LinkedHashMap<>();
                addError(errors, "cpf", "Já existe um cadastro com este CPF.");
                throw new FieldValidationException(errors);
            }
        });
    }

    private Professional resolveOptionalProfessional(UserRole role, Long professionalId) {
        if (role == UserRole.SUPER_ADMIN) {
            return null;
        }
        if (professionalId == null) {
            return null;
        }
        return professionalRepository.findById(professionalId)
            .orElseThrow(() -> new BusinessRuleException("Profissional nao encontrado."));
    }

    private void validateProfessionalLinkAvailable(Professional professional) {
        if (professional == null) {
            return;
        }
        if (!professional.isActive()) {
            throw new BusinessRuleException("Não é permitido vincular profissional inativo.");
        }
        if (userRepository.existsByProfessionalIdAndActiveTrue(professional.getId())) {
            throw new BusinessRuleException("Profissional já vinculado a outro usuário ativo.");
        }
    }

    private User resolvePerformedBy(Long performedByUserId) {
        if (performedByUserId == null) {
            return null;
        }
        return userRepository.findById(performedByUserId)
            .orElseThrow(() -> new BusinessRuleException("Usuario responsavel nao encontrado."));
    }

    private void validateEmailChangeAllowed(User performedBy) {
        if (performedBy == null || performedBy.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessRuleException("Apenas SUPER_ADMIN pode alterar e-mail de usuario.");
        }
    }

    private void validateCpfChangeAllowed(User performedBy) {
        if (performedBy == null || performedBy.getRole() != UserRole.SUPER_ADMIN) {
            throw new BusinessRuleException("Apenas SUPER_ADMIN pode alterar CPF de usuario.");
        }
    }

    private void validateCreateFields(CreateUserCommand command) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        validateRequiredProfile(errors, command.name(), command.cpf(), command.birthDate(), command.motherName(), command.role());
        validateCpf(errors, command.cpf());
        validateContacts(errors, command.contacts());
        validateAddress(errors, command.address());
        throwIfInvalid(errors);
    }

    private void validateUpdateFields(UpdateUserCommand command, String cpf, LocalDate birthDate, String motherName) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        validateRequiredProfile(errors, command.name(), cpf, birthDate, motherName, command.role());
        validateCpf(errors, cpf);
        if (command.contacts() != null) {
            validateContacts(errors, command.contacts());
        }
        if (command.address() != null) {
            validateAddress(errors, command.address());
        }
        throwIfInvalid(errors);
    }

    private void validateRequiredProfile(Map<String, List<String>> errors, String name, String cpf, LocalDate birthDate, String motherName, UserRole role) {
        if (isBlank(name)) {
            addError(errors, "fullName", "Não deve estar em branco.");
        }
        if (isBlank(cpf)) {
            addError(errors, "cpf", "Não deve estar em branco.");
        }
        if (birthDate == null) {
            addError(errors, "birthDate", "Deve ser informada.");
        }
        if (isBlank(motherName)) {
            addError(errors, "motherName", "Não deve estar em branco.");
        }
        if (role == null) {
            addError(errors, "role", "Deve ser informado.");
        }
    }

    private void validateCpf(Map<String, List<String>> errors, String cpf) {
        if (!isBlank(cpf) && !CpfValidator.isValid(cpf)) {
            addError(errors, "cpf", "CPF inválido.");
        }
    }

    private void validateContacts(Map<String, List<String>> errors, List<ContactRequest> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            addError(errors, "contacts", "Informe ao menos um contato.");
            return;
        }
        for (int index = 0; index < contacts.size(); index++) {
            ContactRequest contact = contacts.get(index);
            String prefix = "contacts[" + index + "]";
            if (contact.type() == null) {
                addError(errors, prefix + ".type", "Deve ser informado.");
            }
            if (isBlank(contact.ddi())) {
                addError(errors, prefix + ".ddi", "Não deve estar em branco.");
            }
            String phone = digitsOnly(contact.phone());
            if ("+55".equals(contact.ddi()) && isBlank(contact.ddd())) {
                addError(errors, prefix + ".ddd", "Obrigatório para contatos do Brasil.");
            } else if ("+55".equals(contact.ddi()) && fallbackBrazilianDdds().stream().noneMatch(ddd -> ddd.equals(digitsOnly(contact.ddd())))) {
                addError(errors, prefix + ".ddd", "DDD inválido.");
            }
            if (isBlank(phone)) {
                addError(errors, prefix + ".phone", "Não deve estar em branco.");
            } else if (phone.length() > 9) {
                addError(errors, prefix + ".phone", "Deve ter no máximo 9 caracteres.");
            } else if ("+55".equals(contact.ddi()) && contact.type() == ContactType.MOBILE && phone.length() != 9) {
                addError(errors, prefix + ".phone", "Celular deve ter 9 dígitos.");
            } else if ("+55".equals(contact.ddi()) && contact.type() == ContactType.RESIDENTIAL && phone.length() != 8) {
                addError(errors, prefix + ".phone", "Residencial deve ter 8 dígitos.");
            }
        }
    }

    private void validateAddress(Map<String, List<String>> errors, AddressRequest address) {
        if (address == null) {
            addError(errors, "address", "Deve ser informado.");
            return;
        }
        if (isBlank(address.zipCode())) {
            addError(errors, "address.zipCode", "Não deve estar em branco.");
        }
        if (isBlank(address.number())) {
            addError(errors, "address.number", "Não deve estar em branco.");
        }
    }

    private List<UserContact> toContacts(List<ContactRequest> contacts) {
        if (contacts == null) {
            return List.of();
        }
        return contacts.stream()
            .map(contact -> new UserContact(
                contact.type(),
                contact.ddi(),
                "+55".equals(contact.ddi()) ? digitsOnly(contact.ddd()) : null,
                digitsOnly(contact.phone())
            ))
            .toList();
    }

    private UserAddress toAddress(AddressRequest address) {
        if (address == null) {
            return null;
        }
        return new UserAddress(
            digitsOnly(address.zipCode()),
            address.street(),
            address.number(),
            address.complement(),
            address.neighborhood(),
            address.city(),
            address.state()
        );
    }

    private String digitsOnly(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> fallbackBrazilianDdds() {
        return List.of(
            "11", "12", "13", "14", "15", "16", "17", "18", "19", "21", "22", "24", "27", "28",
            "31", "32", "33", "34", "35", "37", "38", "41", "42", "43", "44", "45", "46", "47",
            "48", "49", "51", "53", "54", "55", "61", "62", "63", "64", "65", "66", "67", "68",
            "69", "71", "73", "74", "75", "77", "79", "81", "82", "83", "84", "85", "86", "87",
            "88", "89", "91", "92", "93", "94", "95", "96", "97", "98", "99"
        );
    }

    private void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, ignored -> new ArrayList<>()).add(message);
    }

    private void throwIfInvalid(Map<String, List<String>> errors) {
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    private void validateRoleGrant(User performedBy, User targetUser, UserRole grantedRole, Long performedByUserId, String ipAddress) {
        if (performedBy == null) {
            return;
        }
        boolean blocked = performedBy.getRole() == UserRole.PROFESSIONAL
            || performedBy.getRole() == UserRole.VIEWER
            || roleRank(grantedRole) > roleRank(performedBy.getRole())
            || (grantedRole == UserRole.SUPER_ADMIN && performedBy.getRole() != UserRole.SUPER_ADMIN);
        if (!blocked) {
            return;
        }
        auditLogService.record(
            "User",
            targetUser == null ? performedBy.getId() : targetUser.getId(),
            AuditAction.USER_ROLE_GRANT_BLOCKED,
            targetUser == null ? null : targetUser.getRole().name(),
            grantedRole.name(),
            "Tentativa bloqueada de conceder perfil superior",
            performedByUserId,
            ipAddress
        );
        throw new BusinessRuleException("Você não possui permissão para conceder este perfil.");
    }

    private int roleRank(UserRole role) {
        return switch (role) {
            case SUPER_ADMIN -> 4;
            case ADMIN -> 3;
            case MANAGER -> 2;
            case PROFESSIONAL -> 1;
            case VIEWER -> 0;
        };
    }

    private void validateMutableSystemUser(User user, boolean targetActive) {
        if ((user.isSystemUser() || user.getRole() == UserRole.SUPER_ADMIN) && !targetActive) {
            throw new BusinessRuleException("Usuarios de sistema nao podem ser inativados.");
        }
    }

    private void recordBlockedSystemUserDeactivation(
        User user,
        boolean targetActive,
        String justification,
        Long performedByUserId,
        String ipAddress
    ) {
        if (targetActive || (!user.isSystemUser() && user.getRole() != UserRole.SUPER_ADMIN)) {
            return;
        }
        if (performedByUserId != null) {
            auditLogService.record(
                "User",
                user.getId(),
                AuditAction.SUPER_ADMIN_DEACTIVATION_BLOCKED,
                "true",
                "false",
                justification,
                performedByUserId,
                ipAddress
            );
        } else {
            auditLogService.requireJustification(justification);
        }
    }

    private String generateInitialPassword() {
        return "Temp@" + Long.toUnsignedString(System.nanoTime(), 36);
    }

    private UserResponse toResponse(User user) {
        Long professionalId = user.getProfessional() == null ? null : user.getProfessional().getId();
        String professionalName = user.getProfessional() == null ? null : user.getProfessional().getName();
        AddressResponse address = user.getAddress() == null ? null : new AddressResponse(
            user.getAddress().getZipCode(),
            user.getAddress().getStreet(),
            user.getAddress().getNumber(),
            user.getAddress().getComplement(),
            user.getAddress().getNeighborhood(),
            user.getAddress().getCity(),
            user.getAddress().getState()
        );
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getName(),
            user.getCpf(),
            user.getBirthDate(),
            user.getMotherName(),
            user.getFatherName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            professionalId,
            professionalName,
            user.getContacts().stream()
                .map(contact -> new ContactResponse(contact.getType(), contact.getDdi(), contact.getDdd(), contact.getPhone()))
                .toList(),
            address,
            user.isSystemUser(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getLastLoginAt()
        );
    }
}
