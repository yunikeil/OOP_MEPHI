package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AppData data;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        data = new AppData();
        authService = new AuthService(data);
    }

    @Test
    void registerAndLoginSuccess() {
        UserAccount user = authService.register("user1", "pass1");
        assertNotNull(user);
        assertEquals("user1", user.getUsername());

        UserAccount logged = authService.login("user1", "pass1");
        assertNotNull(logged);
        assertEquals(user, logged);
    }

    @Test
    void registerDuplicateUserThrows() {
        authService.register("user1", "pass1");
        assertThrows(IllegalArgumentException.class, () -> {
            authService.register("user1", "pass2");
        });
    }

    @Test
    void loginWrongPasswordThrows() {
        authService.register("user1", "pass1");
        assertThrows(IllegalArgumentException.class, () -> {
            authService.login("user1", "wrong");
        });
    }
}
