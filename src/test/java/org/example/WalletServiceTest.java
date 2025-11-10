package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class WalletServiceTest {

    private WalletService walletService;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        walletService = new WalletService();
        user = new UserAccount("test", "1234");
    }

    @Test
    void addIncomeIncreasesBalanceAndCreatesTransaction() {
        walletService.addIncome(user, 1000.0, "ЗП", "зарплата");
        Wallet wallet = user.getWallet();

        assertEquals(1, wallet.getTransactions().size());
        assertEquals(1000.0, wallet.getBalance(), 0.0001);
        Transaction tx = wallet.getTransactions().get(0);
        assertEquals(TransactionType.INCOME, tx.getType());
        assertEquals("ЗП", tx.getCategory());
    }

    @Test
    void addExpenseDecreasesBalanceAndCreatesTransaction() {
        walletService.addIncome(user, 1000.0, "ЗП", "зарплата");
        List<String> notes = walletService.addExpense(user, 200.0, "Еда", "ужин");

        Wallet wallet = user.getWallet();
        assertEquals(2, wallet.getTransactions().size());
        assertEquals(800.0, wallet.getBalance(), 0.0001);
        Transaction tx = wallet.getTransactions().get(1);
        assertEquals(TransactionType.EXPENSE, tx.getType());
        assertEquals("Еда", tx.getCategory());
        // По умолчанию бюджета по "Еда" нет → должно быть предупреждение
        assertTrue(notes.stream().anyMatch(s -> s.contains("ещё не установлен бюджет")));
    }

    @Test
    void addExpenseTriggers80PercentWarning() {
        walletService.setBudget(user, "Еда", 1000.0);
        walletService.addIncome(user, 2000.0, "ЗП", "зарплата");

        // Потратим ровно 800 -> 80% лимита
        List<String> notes = walletService.addExpense(user, 800.0, "Еда", "продукты");
        assertTrue(
                notes.stream().anyMatch(s -> s.contains("80%")),
                "Должно быть предупреждение про 80% бюджета"
        );
    }

    @Test
    void addExpenseTriggers90PercentWarning() {
        walletService.setBudget(user, "Еда", 1000.0);
        walletService.addIncome(user, 2000.0, "ЗП", "зарплата");

        List<String> notes = walletService.addExpense(user, 900.0, "Еда", "продукты");
        assertTrue(
                notes.stream().anyMatch(s -> s.contains("90%")),
                "Должно быть предупреждение про 90% бюджета"
        );
    }

    @Test
    void addExpenseTriggersOverLimitWarning() {
        walletService.setBudget(user, "Еда", 1000.0);
        walletService.addIncome(user, 2000.0, "ЗП", "зарплата");

        List<String> notes = walletService.addExpense(user, 1200.0, "Еда", "продукты");
        assertTrue(
                notes.stream().anyMatch(s -> s.contains("превышен")),
                "Должно быть сообщение о перерасходе бюджета"
        );
    }

    @Test
    void addExpenseWarnsOnZeroOrNegativeBalance() {
        walletService.addIncome(user, 100.0, "ЗП", "зарплата");
        List<String> notes = walletService.addExpense(user, 100.0, "Еда", "продукты");

        assertEquals(0.0, user.getWallet().getBalance(), 0.0001);
        assertTrue(
                notes.stream().anyMatch(s -> s.contains("баланс нулевой или отрицательный")),
                "Должно быть предупреждение о нулевом/отрицательном балансе"
        );
    }

    @Test
    void setBudgetStoresBudget() {
        walletService.setBudget(user, "Еда", 15000.0);
        CategoryBudget b = user.getWallet().getBudget("Еда");
        assertNotNull(b);
        assertEquals(15000.0, b.getLimit(), 0.0001);
    }

    @Test
    void renameCategoryChangesTransactionsAndBudget() {
        walletService.setBudget(user, "Еда", 1000.0);
        walletService.addIncome(user, 2000.0, "ЗП", "зарплата");
        walletService.addExpense(user, 100.0, "Еда", "обед");

        walletService.renameCategory(user, "Еда", "Продукты");

        Wallet wallet = user.getWallet();
        // Бюджет переименован
        assertNull(wallet.getBudget("Еда"));
        assertNotNull(wallet.getBudget("Продукты"));

        // Транзакция тоже переименована
        boolean any = wallet.getTransactions().stream()
                .anyMatch(t -> t.getType() == TransactionType.EXPENSE && "Продукты".equals(t.getCategory()));
        assertTrue(any, "Должна быть транзакция с новой категорией 'Продукты'");
    }

    @Test
    void buildSummaryReturnsNonEmptyLines() {
        walletService.addIncome(user, 1000.0, "ЗП", "зарплата");
        walletService.setBudget(user, "Еда", 500.0);
        walletService.addExpense(user, 100.0, "Еда", "обед");

        List<String> summary = walletService.buildSummary(user);
        assertFalse(summary.isEmpty());
        assertTrue(summary.stream().anyMatch(l -> l.contains("Текущий баланс")));
        assertTrue(summary.stream().anyMatch(l -> l.contains("Еда")));
    }

    @Test
    void buildFilteredReportFiltersByDateAndCategories() {
        // Сегодня, вчера, завтра
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        // Доходы/расходы в разные дни
        user.getWallet().addTransaction(new Transaction(TransactionType.INCOME, 1000, "ЗП", "old", yesterday));
        user.getWallet().addTransaction(new Transaction(TransactionType.EXPENSE, 200, "Еда", "today", today));
        user.getWallet().addTransaction(new Transaction(TransactionType.EXPENSE, 300, "Транспорт", "tomorrow", tomorrow));

        LocalDate from = today;
        LocalDate to = today;
        Set<String> cats = new HashSet<>();
        cats.add(Wallet.normalizeCategory("Еда"));

        List<String> report = walletService.buildFilteredReport(user, from, to, cats);
        // В отчёте должна быть только сегодняшняя "Еда"
        boolean hasFood = report.stream().anyMatch(l -> l.contains("Еда") && l.contains("today"));
        boolean hasTransport = report.stream().anyMatch(l -> l.contains("Транспорт"));
        assertTrue(hasFood);
        assertFalse(hasTransport);
    }

    @Test
    void exportAndImportCsvWorks() throws IOException {
        // заполним исходного пользователя
        walletService.addIncome(user, 1000.0, "ЗП", "зарплата");
        walletService.addExpense(user, 200.0, "Еда", "обед");

        File tmp = File.createTempFile("tx_test", ".csv");
        tmp.deleteOnExit();

        walletService.exportTransactionsToCsv(user, tmp.getAbsolutePath());

        // Новый пользователь, пустой кошелёк
        UserAccount other = new UserAccount("other", "1234");
        int imported = walletService.importTransactionsFromCsv(other, tmp.getAbsolutePath());

        assertEquals(2, imported);
        Wallet otherWallet = other.getWallet();
        assertEquals(2, otherWallet.getTransactions().size());
        // Баланс должен совпасть с исходным: 1000 - 200 = 800
        assertEquals(800.0, otherWallet.getBalance(), 0.0001);
    }
}
