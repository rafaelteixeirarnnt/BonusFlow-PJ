package com.bonusflowpj.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bonusflowpj.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void listsUsersWithAndWithoutContactsWithoutLazyInitializationError() throws Exception {
        String token = loginAsSuperAdmin();
        String suffix = String.valueOf(System.nanoTime());

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Usuario Com Contato",
                      "cpf": "%s",
                      "birthDate": "1990-01-01",
                      "motherName": "Mae do Usuario",
                      "fatherName": "Pai do Usuario",
                      "email": "usuario-contato-%s@example.com",
                      "role": "VIEWER",
                      "contacts": [
                        {
                          "type": "MOBILE",
                          "ddi": "+55",
                          "ddd": "11",
                          "phone": "999999999"
                        }
                      ],
                      "address": {
                        "zipCode": "01001000",
                        "street": "Praca da Se",
                        "number": "100",
                        "complement": "Apto 1",
                        "neighborhood": "Se",
                        "city": "Sao Paulo",
                        "state": "SP"
                      }
                    }
                    """.formatted(validCpfFromSuffix(suffix), suffix)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].contacts").isArray())
            .andExpect(jsonPath("$[1].contacts").isArray());

        mockMvc.perform(get("/api/users/page?page=0&size=10")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].contacts").isArray())
            .andExpect(jsonPath("$.content[1].contacts").isArray());
    }

    private String loginAsSuperAdmin() throws Exception {
        userService.ensureSuperAdmin();
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"admin@bonusflow.com","password":"Admin@123"}
                    """))
            .andExpect(status().isOk())
            .andReturn();
        String body = result.getResponse().getContentAsString();
        return body.substring(body.indexOf("\"token\":\"") + 9, body.indexOf("\",\"user\""));
    }

    private String validCpfFromSuffix(String suffix) {
        String digits = suffix.replaceAll("\\D", "");
        String base = ("100000000" + digits).substring(digits.length(), digits.length() + 9);
        int firstDigit = cpfDigit(base, 9);
        int secondDigit = cpfDigit(base + firstDigit, 10);
        return base + firstDigit + secondDigit;
    }

    private int cpfDigit(String value, int length) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += Character.digit(value.charAt(index), 10) * (length + 1 - index);
        }
        int result = 11 - (sum % 11);
        return result >= 10 ? 0 : result;
    }
}
