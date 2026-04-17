package com.melomarit.melopost.controller;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoginControllerTest {

    private final LoginController controller = new LoginController();

    @Test
    void login_shouldReturnView() {
        Model model = new ConcurrentModel();
        String view = controller.login(null, null, model);
        assertEquals("login", view);
    }

    @Test
    void login_withError_shouldAddMessage() {
        Model model = new ConcurrentModel();
        controller.login("true", null, model);
        assertTrue(model.containsAttribute("errorMessage"));
    }

    @Test
    void login_withLogout_shouldAddMessage() {
        Model model = new ConcurrentModel();
        controller.login(null, "true", model);
        assertTrue(model.containsAttribute("logoutMessage"));
    }
}
