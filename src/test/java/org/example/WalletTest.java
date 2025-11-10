package org.example;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WalletTest {

    @Test
    void getSpentForCategoryInMonthIsCorrect() {
        Wallet wallet = new Wallet();
        YearMonth ym = YearMonth.now();

        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 100, "Еда", "обед", ym.atDay(1)));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 200, "Еда", "ужин", ym.atDay(2)));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 50, "Транспорт", "проезд", ym.atDay(3)));
        // другая категория
        double spentFood = wallet.getSpentForCategoryInMonth("Еда", ym);
        assertEquals(300.0, spentFood, 0.0001);
    }

    @Test
    void getExpensesByCategoryForMonthAggregatesCorrectly() {
        Wallet wallet = new Wallet();
        YearMonth ym = YearMonth.now();
        YearMonth prev = ym.minusMonths(1);

        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 100, "Еда", "1", ym.atDay(1)));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 50, "Еда", "2", ym.atDay(2)));
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 30, "Транспорт", "3", ym.atDay(3)));
        // в другом месяце не должно учитываться
        wallet.addTransaction(new Transaction(TransactionType.EXPENSE, 999, "Еда", "старое", prev.atDay(1)));

        Map<String, Double> map = wallet.getExpensesByCategoryForMonth(ym);
        assertEquals(2, map.size());
        assertEquals(150.0, map.get(Wallet.normalizeCategory("Еда")), 0.0001);
        assertEquals(30.0, map.get(Wallet.normalizeCategory("Транспорт")), 0.0001);
    }
}
