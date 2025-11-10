package org.example;

import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Главный класс: CLI-интерфейс и цикл команд.
 */
public class FinanceApp {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        AppData data = DataStore.load();
        AuthService authService = new AuthService(data);
        WalletService walletService = new WalletService();

        UserAccount currentUser = null;
        System.out.println("Финансовый трекер. Введите 'help' для списка команд.");

        mainLoop:
        while (true) {
            System.out.print((currentUser == null ? "[гость]" : "[" + currentUser.getUsername() + "]") + " > ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String cmd = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help":
                        printHelp(currentUser != null);
                        break;
                    case "exit":
                        break mainLoop;
                    case "register":
                        if (currentUser != null) {
                            System.out.println("Сначала выйдите из аккаунта командой 'logout'.");
                        } else {
                            currentUser = handleRegister(authService);
                        }
                        break;
                    case "login":
                        if (currentUser != null) {
                            System.out.println("Вы уже авторизованы. Используйте 'logout' для выхода.");
                        } else {
                            currentUser = handleLogin(authService);
                        }
                        break;
                    case "logout":
                        currentUser = null;
                        System.out.println("Вы вышли из аккаунта.");
                        break;

                    // Команды, доступные только после авторизации
                    case "add_income":
                        requireUser(currentUser);
                        handleAddIncome(walletService, currentUser);
                        break;
                    case "add_expense":
                        requireUser(currentUser);
                        handleAddExpense(walletService, currentUser);
                        break;
                    case "set_budget":
                        requireUser(currentUser);
                        handleSetBudget(walletService, currentUser);
                        break;
                    case "edit_budget":
                        requireUser(currentUser);
                        handleEditBudget(walletService, currentUser);
                        break;
                    case "budgets":
                        requireUser(currentUser);
                        handleListBudgets(currentUser);
                        break;
                    case "rename_category":
                        requireUser(currentUser);
                        handleRenameCategory(walletService, currentUser);
                        break;
                    case "summary":
                        requireUser(currentUser);
                        handleSummary(walletService, currentUser);
                        break;
                    case "report":
                        requireUser(currentUser);
                        handleReport(walletService, currentUser);
                        break;
                    case "list_tx":
                        requireUser(currentUser);
                        handleListTransactions(currentUser);
                        break;
                    case "export_csv":
                        requireUser(currentUser);
                        handleExportCsv(walletService, currentUser);
                        break;
                    case "import_csv":
                        requireUser(currentUser);
                        handleImportCsv(walletService, currentUser);
                        break;
                    default:
                        System.out.println("Неизвестная команда '" + cmd + "'. Введите 'help' для списка команд.");
                }
            } catch (IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Неверные данные: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }

        DataStore.save(data);
        System.out.println("Данные сохранены. До свидания!");
    }

    // ---------- CLI-помощники ----------

    private static void printHelp(boolean loggedIn) {
        System.out.println("=== Справка по командам ===");
        System.out.println("Базовые команды:");
        System.out.println("  help              - показать эту справку");
        System.out.println("  register          - регистрация нового пользователя");
        System.out.println("  login             - вход пользователя");
        System.out.println("  logout            - выход из аккаунта");
        System.out.println("  exit              - выход из программы и сохранение данных");

        if (loggedIn) {
            System.out.println();
            System.out.println("Работа с деньгами:");
            System.out.println("  add_income        - добавить доход (интерактивный ввод)");
            System.out.println("  add_expense       - добавить расход (интерактивный ввод)");
            System.out.println("  list_tx           - список операций (таблица)");

            System.out.println();
            System.out.println("Бюджеты и категории:");
            System.out.println("  set_budget        - установить/изменить бюджет по категории");
            System.out.println("  edit_budget       - изменить существующий бюджет");
            System.out.println("  budgets           - показать таблицу всех бюджетов");
            System.out.println("  rename_category   - переименовать категорию (во всех операциях и бюджетах)");

            System.out.println();
            System.out.println("Отчёты и статистика:");
            System.out.println("  summary           - сводка по всем категориям и бюджетам (текущий месяц)");
            System.out.println("  report            - отчёт по выборке (период + несколько категорий)");

            System.out.println();
            System.out.println("Экспорт / импорт:");
            System.out.println("  export_csv        - экспорт операций в CSV");
            System.out.println("  import_csv        - импорт операций из CSV");

            System.out.println();
            System.out.println("Примеры использования:");
            System.out.println("  add_income        → введите сумму, категорию ('ЗП') и описание");
            System.out.println("  set_budget        → 'Еда', затем месячный лимит (например, 20000)");
            System.out.println("  report            → задайте диапазон дат и список категорий ('Еда, Транспорт')");
            System.out.println("  export_csv        → укажите имя файла, например 'report.csv'");
        }
        System.out.println("============================");
    }

    private static UserAccount handleRegister(AuthService authService) {
        System.out.print("Введите логин: ");
        String username = scanner.nextLine().trim();
        System.out.print("Введите пароль (минимум 4 символа): ");
        String password = scanner.nextLine();
        UserAccount user = authService.register(username, password);
        System.out.println("Пользователь '" + user.getUsername() + "' успешно зарегистрирован и авторизован.");
        return user;
    }

    private static UserAccount handleLogin(AuthService authService) {
        System.out.print("Логин: ");
        String username = scanner.nextLine().trim();
        System.out.print("Пароль: ");
        String password = scanner.nextLine();
        UserAccount user = authService.login(username, password);
        System.out.println("Добро пожаловать, " + user.getUsername() + "!");
        return user;
    }

    private static void handleAddIncome(WalletService walletService, UserAccount user) {
        double amount = readPositiveDouble("Введите сумму дохода: ");
        String category = readNonEmptyString("Введите категорию дохода (например, ЗП, Премия): ");
        String description = readNonEmptyString("Описание (например, зарплата за октябрь): ");
        walletService.addIncome(user, amount, category, description);
        System.out.println("Доход добавлен. Текущий баланс: " + String.format("%.2f", user.getWallet().getBalance()));
    }

    private static void handleAddExpense(WalletService walletService, UserAccount user) {
        double amount = readPositiveDouble("Введите сумму расхода: ");
        String category = readNonEmptyString("Введите категорию расхода (например, Еда, Аренда): ");
        String description = readNonEmptyString("Описание (например, продукты): ");

        List<String> notifications = walletService.addExpense(user, amount, category, description);
        System.out.println("Расход добавлен. Текущий баланс: " + String.format("%.2f", user.getWallet().getBalance()));
        for (String note : notifications) {
            System.out.println(note);
        }
    }

    private static void handleSetBudget(WalletService walletService, UserAccount user) {
        String category = readNonEmptyString("Категория (например, Еда, Аренда): ");
        double limit = readPositiveDouble("Месячный лимит по этой категории: ");
        walletService.setBudget(user, category, limit);
        System.out.println("Бюджет по категории '" + category + "' установлен/обновлён: " + String.format("%.2f", limit));
    }

    private static void handleEditBudget(WalletService walletService, UserAccount user) {
        String category = readNonEmptyString("Категория бюджета, который хотите изменить: ");
        Wallet wallet = user.getWallet();
        CategoryBudget existing = wallet.getBudget(category);
        if (existing == null) {
            System.out.println("Бюджет по этой категории не найден. Используйте 'set_budget' для создания.");
            return;
        }
        System.out.println("Текущий лимит: " + String.format("%.2f", existing.getLimit()));
        double newLimit = readPositiveDouble("Новый месячный лимит: ");
        walletService.setBudget(user, category, newLimit);
        System.out.println("Бюджет обновлён.");
    }

    private static void handleListBudgets(UserAccount user) {
        Wallet wallet = user.getWallet();
        Map<String, CategoryBudget> budgets = wallet.getBudgets();
        if (budgets.isEmpty()) {
            System.out.println("Бюджеты пока не заданы.");
            return;
        }
        System.out.println("=== Бюджеты по категориям ===");
        System.out.printf("%-20s | %-12s%n", "Категория", "Лимит");
        System.out.println("---------------------+--------------");
        for (CategoryBudget b : budgets.values()) {
            System.out.printf("%-20s | %-12.2f%n", b.getName(), b.getLimit());
        }
    }

    private static void handleRenameCategory(WalletService walletService, UserAccount user) {
        String oldCat = readNonEmptyString("Старая категория: ");
        String newCat = readNonEmptyString("Новая категория: ");
        walletService.renameCategory(user, oldCat, newCat);
        System.out.println("Категория '" + oldCat + "' переименована в '" + newCat + "'.");
    }

    private static void handleSummary(WalletService walletService, UserAccount user) {
        List<String> summaryLines = walletService.buildSummary(user);
        System.out.println("===== Сводка =====");
        for (String line : summaryLines) {
            System.out.println(line);
        }
        System.out.println("==================");
    }

    private static void handleReport(WalletService walletService, UserAccount user) {
        System.out.println("Отчёт по выборке.");
        LocalDate from = readDateOrEmpty("Дата начала (ГГГГ-ММ-ДД, пусто - без ограничения): ");
        LocalDate to = readDateOrEmpty("Дата конца   (ГГГГ-ММ-ДД, пусто - без ограничения): ");
        if (from != null && to != null && to.isBefore(from)) {
            System.out.println("Неверный диапазон: дата конца раньше даты начала.");
            return;
        }
        System.out.print("Категории через запятую (пусто - все): ");
        String catsLine = scanner.nextLine().trim();
        Set<String> cats = null;
        if (!catsLine.isEmpty()) {
            cats = new HashSet<>();
            for (String c : catsLine.split(",")) {
                String trimmed = c.trim();
                if (!trimmed.isEmpty()) {
                    cats.add(trimmed.toLowerCase());
                }
            }
        }

        List<String> lines = walletService.buildFilteredReport(user, from, to, cats);
        System.out.println("===== Отчёт по выборке =====");
        for (String l : lines) {
            System.out.println(l);
        }
        System.out.println("============================");
    }

    private static void handleListTransactions(UserAccount user) {
        Wallet wallet = user.getWallet();
        List<Transaction> txs = wallet.getTransactions();
        if (txs.isEmpty()) {
            System.out.println("Операций пока нет.");
            return;
        }
        System.out.println("=== Последние операции ===");
        System.out.printf("%-10s | %-7s | %-15s | %-10s | %s%n",
                "Дата", "Тип", "Категория", "Сумма", "Описание");
        System.out.println("-----------+---------+-----------------+------------+------------------------");

        txs.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(50)
                .forEach(tx -> {
                    String typeLabel = tx.getType() == TransactionType.INCOME ? "Доход" : "Расход";
                    System.out.printf("%-10s | %-7s | %-15s | %-10.2f | %s%n",
                            tx.getDate(),
                            typeLabel,
                            tx.getCategory(),
                            tx.getAmount(),
                            tx.getDescription());
                });
    }

    private static void handleExportCsv(WalletService walletService, UserAccount user) {
        String filename = readNonEmptyString("Имя файла для экспорта (например, report.csv): ");
        try {
            walletService.exportTransactionsToCsv(user, filename);
            System.out.println("Операции экспортированы в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при экспорте: " + e.getMessage());
        }
    }

    private static void handleImportCsv(WalletService walletService, UserAccount user) {
        String filename = readNonEmptyString("Имя файла для импорта (CSV): ");
        try {
            int imported = walletService.importTransactionsFromCsv(user, filename);
            System.out.println("Импорт завершён. Добавлено операций: " + imported);
            System.out.println("Текущий баланс: " + String.format("%.2f", user.getWallet().getBalance()));
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
        }
    }

    private static void requireUser(UserAccount user) {
        if (user == null) {
            throw new IllegalStateException("Сначала авторизуйтесь (команды 'register' или 'login').");
        }
    }

    private static double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim().replace(",", ".");
            if (s.isEmpty()) {
                System.out.println("Значение не может быть пустым.");
                continue;
            }
            try {
                double v = Double.parseDouble(s);
                if (v <= 0) {
                    System.out.println("Сумма должна быть положительной.");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Введите корректное число, например 123.45");
            }
        }
    }

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.isEmpty()) {
                System.out.println("Значение не может быть пустым.");
            } else {
                return s;
            }
        }
    }

    private static LocalDate readDateOrEmpty(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = scanner.nextLine().trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Неверный формат даты. Используйте ГГГГ-ММ-ДД, например 2025-01-15.");
            }
        }
    }
}

// ================== МОДЕЛЬ ==================

/**
 * Объект верхнего уровня для сериализации.
 */
class AppData implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, UserAccount> users = new HashMap<>();

    public Map<String, UserAccount> getUsers() {
        return users;
    }
}

/**
 * Пользователь + его кошелёк.
 */
class UserAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password; // для простоты в открытом виде
    private final Wallet wallet;

    public UserAccount(String username, String password) {
        this.username = username;
        this.password = password;
        this.wallet = new Wallet();
    }

    public String getUsername() {
        return username;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public boolean checkPassword(String raw) {
        return password.equals(raw);
    }
}

/**
 * Кошелёк пользователя: баланс, операции, бюджеты.
 */
class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;

    private double balance = 0.0;
    private final List<Transaction> transactions = new ArrayList<>();
    private final Map<String, CategoryBudget> budgets = new HashMap<>(); // ключ - категория в нижнем регистре

    public double getBalance() {
        return balance;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public Map<String, CategoryBudget> getBudgets() {
        return budgets;
    }

    public void addTransaction(Transaction tx) {
        transactions.add(tx);
        if (tx.getType() == TransactionType.INCOME) {
            balance += tx.getAmount();
        } else if (tx.getType() == TransactionType.EXPENSE) {
            balance -= tx.getAmount();
        }
    }

    public void setBudget(String category, double limit) {
        String key = normalizeCategory(category);
        budgets.put(key, new CategoryBudget(category, limit));
    }

    public CategoryBudget getBudget(String category) {
        return budgets.get(normalizeCategory(category));
    }

    public double getTotalByType(TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public double getSpentForCategoryInMonth(String category, YearMonth ym) {
        String key = normalizeCategory(category);
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> normalizeCategory(t.getCategory()).equals(key))
                .filter(t -> YearMonth.from(t.getDate()).equals(ym))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    public Map<String, Double> getExpensesByCategoryForMonth(YearMonth ym) {
        Map<String, Double> result = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE &&
                    YearMonth.from(t.getDate()).equals(ym)) {
                String key = normalizeCategory(t.getCategory());
                result.put(key, result.getOrDefault(key, 0.0) + t.getAmount());
            }
        }
        return result;
    }

    public static String normalizeCategory(String category) {
        return category.trim().toLowerCase();
    }
}

/**
 * Тип операции.
 */
enum TransactionType {
    INCOME,
    EXPENSE
}

/**
 * Операция (доход/расход).
 */
class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final TransactionType type;
    private final double amount;
    private String category;
    private final String description;
    private final LocalDate date;

    public Transaction(TransactionType type, double amount, String category, String description, LocalDate date) {
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.description = description;
        this.date = date;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String newCategory) {
        this.category = newCategory;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getDate() {
        return date;
    }
}

/**
 * Бюджет по категории.
 */
class CategoryBudget implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name; // отображаемое имя категории
    private final double limit; // месячный лимит

    public CategoryBudget(String name, double limit) {
        this.name = name;
        this.limit = limit;
    }

    public String getName() {
        return name;
    }

    public double getLimit() {
        return limit;
    }
}

// ================== СЕРВИСЫ ==================

/**
 * Сервис авторизации и регистрации.
 */
class AuthService {

    private final AppData data;

    public AuthService(AppData data) {
        this.data = data;
    }

    public UserAccount register(String username, String password) {
        validateUsername(username);
        validatePassword(password);
        String key = normalizeUsername(username);
        if (data.getUsers().containsKey(key)) {
            throw new IllegalArgumentException("Пользователь с таким логином уже существует.");
        }
        UserAccount user = new UserAccount(username, password);
        data.getUsers().put(key, user);
        return user;
    }

    public UserAccount login(String username, String password) {
        validateUsername(username);
        String key = normalizeUsername(username);
        UserAccount user = data.getUsers().get(key);
        if (user == null || !user.checkPassword(password)) {
            throw new IllegalArgumentException("Неверный логин или пароль.");
        }
        return user;
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Логин не может быть пустым.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Слишком короткий пароль (минимум 4 символа).");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase();
    }
}

/**
 * Бизнес-логика кошелька/бюджетов.
 */
class WalletService {

    public void addIncome(UserAccount user, double amount, String category, String description) {
        validateAmount(amount);
        validateCategory(category);
        Wallet wallet = user.getWallet();
        Transaction tx = new Transaction(TransactionType.INCOME, amount, category, description, LocalDate.now());
        wallet.addTransaction(tx);
    }

    /**
     * Добавляет расход и возвращает список текстовых уведомлений.
     */
    public List<String> addExpense(UserAccount user, double amount, String category, String description) {
        validateAmount(amount);
        validateCategory(category);
        Wallet wallet = user.getWallet();

        Transaction tx = new Transaction(TransactionType.EXPENSE, amount, category, description, LocalDate.now());
        wallet.addTransaction(tx);

        List<String> notifications = new ArrayList<>();

        YearMonth ym = YearMonth.from(tx.getDate());
        CategoryBudget budget = wallet.getBudget(category);
        double spent = wallet.getSpentForCategoryInMonth(category, ym);

        if (budget == null) {
            notifications.add("Предупреждение: по категории '" + category + "' ещё не установлен бюджет.");
        } else {
            double limit = budget.getLimit();
            if (spent > limit) {
                notifications.add(String.format(
                        "ВНИМАНИЕ: бюджет по категории '%s' превышен. Потрачено %.2f из %.2f (перерасход %.2f).",
                        budget.getName(), spent, limit, spent - limit
                ));
            } else if (spent >= 0.9 * limit) {
                notifications.add(String.format(
                        "Осторожно: вы превысили 90%% бюджета по категории '%s'. Потрачено %.2f из %.2f.",
                        budget.getName(), spent, limit
                ));
            } else if (spent >= 0.8 * limit) {
                notifications.add(String.format(
                        "Предупреждение: израсходовано более 80%% бюджета по категории '%s'. Потрачено %.2f из %.2f.",
                        budget.getName(), spent, limit
                ));
            }
        }

        // Доп. уведомление: нулевой или отрицательный баланс
        if (wallet.getBalance() <= 0) {
            notifications.add(String.format(
                    "ВНИМАНИЕ: ваш баланс нулевой или отрицательный (%.2f).",
                    wallet.getBalance()
            ));
        }

        return notifications;
    }

    public void setBudget(UserAccount user, String category, double limit) {
        validateAmount(limit);
        validateCategory(category);
        user.getWallet().setBudget(category, limit);
    }

    public void renameCategory(UserAccount user, String oldCategory, String newCategory) {
        validateCategory(oldCategory);
        validateCategory(newCategory);

        Wallet wallet = user.getWallet();
        String oldKey = Wallet.normalizeCategory(oldCategory);
        String newKey = Wallet.normalizeCategory(newCategory);

        if (oldKey.equals(newKey)) {
            throw new IllegalArgumentException("Старая и новая категории совпадают.");
        }

        boolean foundInTx = false;
        for (Transaction t : wallet.getTransactions()) {
            String key = Wallet.normalizeCategory(t.getCategory());
            if (key.equals(oldKey)) {
                t.setCategory(newCategory);
                foundInTx = true;
            }
        }

        CategoryBudget oldBudget = wallet.getBudgets().remove(oldKey);
        if (oldBudget != null) {
            wallet.getBudgets().put(newKey, new CategoryBudget(newCategory, oldBudget.getLimit()));
        }

        if (!foundInTx && oldBudget == null) {
            throw new IllegalArgumentException("Категория '" + oldCategory + "' не найдена ни в операциях, ни в бюджетах.");
        }
    }

    /**
     * Сводка по текущему месяцу и бюджетам.
     */
    public List<String> buildSummary(UserAccount user) {
        Wallet wallet = user.getWallet();
        List<String> lines = new ArrayList<>();

        double balance = wallet.getBalance();
        double totalIncome = wallet.getTotalByType(TransactionType.INCOME);
        double totalExpense = wallet.getTotalByType(TransactionType.EXPENSE);
        YearMonth ym = YearMonth.now();

        lines.add(String.format("Текущий баланс: %.2f", balance));
        lines.add(String.format("Всего доходов: %.2f, всего расходов: %.2f", totalIncome, totalExpense));
        lines.add("");
        lines.add("Текущий месяц: " + ym.getMonthValue() + "." + ym.getYear());
        lines.add("Бюджеты и расходы по категориям (текущий месяц):");
        lines.add(String.format("%-20s | %-10s | %-10s | %-10s | %-12s",
                "Категория", "Потрачено", "Лимит", "Остаток", "Статус"));
        lines.add("---------------------+------------+------------+------------+-------------");

        Map<String, Double> spentByCat = wallet.getExpensesByCategoryForMonth(ym);

        if (wallet.getBudgets().isEmpty()) {
            lines.add("Бюджеты пока не заданы.");
        } else {
            for (CategoryBudget budget : wallet.getBudgets().values()) {
                String key = Wallet.normalizeCategory(budget.getName());
                double spent = spentByCat.getOrDefault(key, 0.0);
                double limit = budget.getLimit();
                double remaining = limit - spent;
                String status;
                if (spent > limit) {
                    status = "Перерасход";
                } else if (spent >= 0.9 * limit) {
                    status = "90%+";
                } else if (spent >= 0.8 * limit) {
                    status = "80%+";
                } else if (spent == 0) {
                    status = "Не тратилось";
                } else {
                    status = "OK";
                }
                lines.add(String.format("%-20s | %-10.2f | %-10.2f | %-10.2f | %-12s",
                        budget.getName(), spent, limit, remaining, status));
            }
        }

        // Категории, где были расходы, но бюджета нет
        boolean extraCatsHeader = false;
        for (Map.Entry<String, Double> entry : spentByCat.entrySet()) {
            String catKey = entry.getKey();
            if (!wallet.getBudgets().containsKey(catKey)) {
                if (!extraCatsHeader) {
                    lines.add("");
                    lines.add("Категории без бюджета:");
                    lines.add(String.format("%-20s | %-10s", "Категория", "Потрачено"));
                    lines.add("---------------------+------------");
                    extraCatsHeader = true;
                }
                lines.add(String.format("%-20s | %-10.2f", catKey, entry.getValue()));
            }
        }

        return lines;
    }

    /**
     * Отчёт по выборке: период + (опционально) несколько категорий.
     */
    public List<String> buildFilteredReport(UserAccount user, LocalDate from, LocalDate to, Set<String> categories) {
        Wallet wallet = user.getWallet();
        List<Transaction> txs = wallet.getTransactions();

        List<Transaction> filtered = new ArrayList<>();
        for (Transaction t : txs) {
            LocalDate d = t.getDate();
            if (from != null && d.isBefore(from)) continue;
            if (to != null && d.isAfter(to)) continue;
            if (categories != null && !categories.isEmpty()) {
                String key = Wallet.normalizeCategory(t.getCategory());
                if (!categories.contains(key)) continue;
            }
            filtered.add(t);
        }

        List<String> lines = new ArrayList<>();
        String periodStr;
        if (from == null && to == null) periodStr = "все даты";
        else if (from != null && to == null) periodStr = "с " + from + " и позже";
        else if (from == null) periodStr = "до " + to;
        else periodStr = "c " + from + " по " + to;

        String catsStr;
        if (categories == null || categories.isEmpty()) catsStr = "все категории";
        else catsStr = String.join(", ", categories);

        lines.add("Период: " + periodStr);
        lines.add("Категории: " + catsStr);
        lines.add("");

        if (filtered.isEmpty()) {
            lines.add("Нет данных для указанного периода/категорий.");
            return lines;
        }

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        Map<String, Double> expenseByCat = new HashMap<>();

        lines.add("Операции:");
        lines.add(String.format("%-10s | %-7s | %-15s | %-10s | %s",
                "Дата", "Тип", "Категория", "Сумма", "Описание"));
        lines.add("-----------+---------+-----------------+------------+------------------------");

        for (Transaction t : filtered) {
            String typeLabel = t.getType() == TransactionType.INCOME ? "Доход" : "Расход";
            if (t.getType() == TransactionType.INCOME) {
                totalIncome += t.getAmount();
            } else {
                totalExpense += t.getAmount();
                String key = Wallet.normalizeCategory(t.getCategory());
                expenseByCat.put(key, expenseByCat.getOrDefault(key, 0.0) + t.getAmount());
            }
            lines.add(String.format("%-10s | %-7s | %-15s | %-10.2f | %s",
                    t.getDate(),
                    typeLabel,
                    t.getCategory(),
                    t.getAmount(),
                    t.getDescription()));
        }

        lines.add("");
        lines.add(String.format("Всего доходов: %.2f", totalIncome));
        lines.add(String.format("Всего расходов: %.2f", totalExpense));

        if (!expenseByCat.isEmpty()) {
            lines.add("");
            lines.add("Расходы по категориям:");
            lines.add(String.format("%-20s | %-10s", "Категория", "Потрачено"));
            lines.add("---------------------+------------");
            for (Map.Entry<String, Double> e : expenseByCat.entrySet()) {
                lines.add(String.format("%-20s | %-10.2f", e.getKey(), e.getValue()));
            }
        }

        return lines;
    }

    public void exportTransactionsToCsv(UserAccount user, String filename) throws IOException {
        Wallet wallet = user.getWallet();
        List<Transaction> txs = wallet.getTransactions();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("date,type,category,description,amount");
            writer.newLine();
            for (Transaction t : txs) {
                String typeStr = t.getType() == TransactionType.INCOME ? "INCOME" : "EXPENSE";
                // Экранируем запятые и кавычки в описании/категории
                String categoryEsc = escapeCsv(t.getCategory());
                String descEsc = escapeCsv(t.getDescription());
                writer.write(t.getDate() + "," + typeStr + "," + categoryEsc + "," + descEsc + "," + t.getAmount());
                writer.newLine();
            }
        }
    }

    public int importTransactionsFromCsv(UserAccount user, String filename) throws IOException {
        Wallet wallet = user.getWallet();
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine(); // предполагаем, что первая строка — заголовок
            if (line == null) return 0;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCsvLine(line);
                if (parts.length < 5) {
                    System.out.println("Пропускаю строку (мало столбцов): " + line);
                    continue;
                }
                try {
                    LocalDate date = LocalDate.parse(parts[0].trim());
                    String typeStr = parts[1].trim().toUpperCase();
                    TransactionType type = "INCOME".equals(typeStr) ? TransactionType.INCOME : TransactionType.EXPENSE;
                    String category = parts[2].trim();
                    String description = parts[3].trim();
                    double amount = Double.parseDouble(parts[4].trim().replace(",", "."));
                    if (amount <= 0) {
                        System.out.println("Пропускаю строку (неположительная сумма): " + line);
                        continue;
                    }
                    Transaction tx = new Transaction(type, amount, category, description, date);
                    wallet.addTransaction(tx);
                    imported++;
                } catch (Exception e) {
                    System.out.println("Пропускаю строку (ошибка парсинга): " + line + " | " + e.getMessage());
                }
            }
        }
        return imported;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        boolean needQuotes = value.contains(",") || value.contains("\"") || value.contains("\n");
        String v = value.replace("\"", "\"\"");
        return needQuotes ? "\"" + v + "\"" : v;
    }

    // Очень простой парсер CSV-строки (поддерживает кавычки)
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '\"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                        current.append('\"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '\"') {
                    inQuotes = true;
                } else if (c == ',') {
                    result.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля.");
        }
    }

    private void validateCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Категория не может быть пустой.");
        }
    }
}

// ================== ХРАНЕНИЕ ДАННЫХ ==================

/**
 * Класс для сохранения/загрузки состояния в файл.
 */
class DataStore {
    private static final String DATA_FILE = "finance.dat";

    public static AppData load() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return new AppData();
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = in.readObject();
            if (obj instanceof AppData) {
                return (AppData) obj;
            } else {
                System.out.println("Формат файла данных не распознан. Будет создан новый файл.");
                return new AppData();
            }
        } catch (Exception e) {
            System.out.println("Не удалось загрузить данные (" + e.getMessage() + "). Будет создан новый файл.");
            return new AppData();
        }
    }

    public static void save(AppData data) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(data);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении данных: " + e.getMessage());
        }
    }
}
