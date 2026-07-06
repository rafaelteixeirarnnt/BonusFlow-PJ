package com.bonusflowpj.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupUserSeeder implements ApplicationRunner {

    private final UserService userService;

    public StartupUserSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureSuperAdmin();
    }
}
