package com.bonusflowpj.service;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoggingAccessEmailService implements AccessEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAccessEmailService.class);

    private final String systemUrl;

    public LoggingAccessEmailService(@Value("${app.frontend-url}") String systemUrl) {
        this.systemUrl = systemUrl;
    }

    @Override
    public void sendInitialAccess(Professional professional, User user, String rawPassword) {
        // Integrate a real SMTP/provider here. Never log rawPassword.
        String body = """
            Ola, %s.

            Seu acesso ao sistema BonusFlow PJ foi criado.

            Login: %s
            Senha inicial: %s

            Acesse o sistema pelo link:
            %s

            Por seguranca, recomendamos alterar sua senha no primeiro acesso.

            Atenciosamente,
            Equipe BonusFlow PJ
            """.formatted(professional.getName(), user.getEmail(), rawPassword, systemUrl);
        LOGGER.info("Initial access email prepared for professionalId={} userId={} body={}",
            professional.getId(), user.getId(), body.replace(rawPassword, "[senha inicial mascarada]"));
    }
}
