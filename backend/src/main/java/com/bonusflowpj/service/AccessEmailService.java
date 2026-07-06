package com.bonusflowpj.service;

import com.bonusflowpj.domain.Professional;
import com.bonusflowpj.domain.User;

public interface AccessEmailService {

    void sendInitialAccess(Professional professional, User user, String rawPassword);
}
