package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты: проверяем совместную работу AuthService, WalletService и модели.
 */
public class FinanceIntegrationTest {

    private AppData data;
    private AuthService authService;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        data = new AppData();
        authService = new AuthService(data);
        walletService = new WalletService();
    }

    @Test
    void fullScenarioUserFlow() {
        // Регистрация и логин
        UserAccount user = authService.register("ivan", "1234");
        UserAccount logged = authService.login("ivan", "1234");
        assertEquals(user, logged);

        // Доход
        walletService.addIncome(logged, 5000.0, "ЗП", "зарплата");
        // Бюджет по Еда
        walletService.setBudget(logged, "Еда", 2000.0);
        // Расход
        walletService.addExpense(logged, 1000.0, "Еда", "продукты");

        Wallet wallet = logged.getWallet();
        assertEquals(2, wallet.getTransactions().size());
        assertEquals(4000.0, wallet.getBalance(), 0.0001);

        // Сводка
        List<String> summary = walletService.buildSummary(logged);
        assertTrue(summary.stream().anyMatch(s -> s.contains("Еда")));
        assertTrue(summary.stream().anyMatch(s -> s.contains("Текущий баланс")));
    }

    @Test
    void dataStoreSaveAndLoadKeepsUsers() {
        // создаём пользователя
        UserAccount user = authService.register("masha", "abcd");
        walletService.addIncome(user, 1000.0, "ЗП", "зарплата");

        // сохраняем
        DataStore.save(data);

        // загружаем заново
        AppData loaded = DataStore.load();
        assertNotNull(loaded);

        AuthService authService2 = new AuthService(loaded);
        UserAccount logged = authService2.login("masha", "abcd");
        assertNotNull(logged);
        assertEquals(1000.0, logged.getWallet().getBalance(), 0.0001);
    }
}
