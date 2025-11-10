package org.example;

import java.io.*;
import java.time.LocalDate;
import java.time.YearMonth;
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
                    case "summary":
                        requireUser(currentUser);
                        handleSummary(walletService, currentUser);
                        break;
                    case "list_tx":
                        requireUser(currentUser);
                        handleListTransactions(currentUser);
                        break;
                    default:
                        System.out.println("Неизвестная команда. Введите 'help' для списка команд.");
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
        System.out.println("Доступные команды:");
        System.out.println("  help          - показать эту справку");
        System.out.println("  register      - регистрация нового пользователя");
        System.out.println("  login         - вход пользователя");
        System.out.println("  logout        - выход из аккаунта");
        System.out.println("  exit          - выход из программы и сохранение данных");
        if (loggedIn) {
            System.out.println("  add_income    - добавить доход");
            System.out.println("  add_expense   - добавить расход");
            System.out.println("  set_budget    - установить/изменить бюджет по категории");
            System.out.println("  summary       - сводка по категориям и остатки бюджетов");
            System.out.println("  list_tx       - список операций");
        }
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

        String notification = walletService.addExpense(user, amount, category, description);
        System.out.println("Расход добавлен. Текущий баланс: " + String.format("%.2f", user.getWallet().getBalance()));
        if (notification != null && !notification.isEmpty()) {
            System.out.println(notification);
        }
    }

    private static void handleSetBudget(WalletService walletService, UserAccount user) {
        String category = readNonEmptyString("Категория (например, Еда, Аренда): ");
        double limit = readPositiveDouble("Месячный лимит по этой категории: ");
        walletService.setBudget(user, category, limit);
        System.out.println("Бюджет по категории '" + category + "' установлен: " + String.format("%.2f", limit));
    }

    private static void handleSummary(WalletService walletService, UserAccount user) {
        List<String> summaryLines = walletService.buildSummary(user);
        System.out.println("----- Сводка -----");
        for (String line : summaryLines) {
            System.out.println(line);
        }
        System.out.println("------------------");
    }

    private static void handleListTransactions(UserAccount user) {
        Wallet wallet = user.getWallet();
        List<Transaction> txs = wallet.getTransactions();
        if (txs.isEmpty()) {
            System.out.println("Операций пока нет.");
            return;
        }
        System.out.println("Последние операции:");
        txs.stream()
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .limit(50)
                .forEach(tx -> {
                    System.out.println(String.format(
                            "%s | %s | %s | %s | %.2f",
                            tx.getDate(),
                            tx.getType() == TransactionType.INCOME ? "Доход " : "Расход",
                            tx.getCategory(),
                            tx.getDescription(),
                            tx.getAmount()
                    ));
                });
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

    private String normalizeCategory(String category) {
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
    private final String category;
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
     * Добавляет расход и возвращает текст уведомления, если нужно предупредить.
     */
    public String addExpense(UserAccount user, double amount, String category, String description) {
        validateAmount(amount);
        validateCategory(category);
        Wallet wallet = user.getWallet();

        Transaction tx = new Transaction(TransactionType.EXPENSE, amount, category, description, LocalDate.now());
        wallet.addTransaction(tx);

        YearMonth ym = YearMonth.from(tx.getDate());
        CategoryBudget budget = wallet.getBudget(category);
        double spent = wallet.getSpentForCategoryInMonth(category, ym);

        if (budget == null) {
            return "Предупреждение: по категории '" + category + "' ещё не установлен бюджет.";
        } else {
            double limit = budget.getLimit();
            if (spent > limit) {
                return String.format(
                        "ВНИМАНИЕ: бюджет по категории '%s' превышен. Потрачено %.2f из %.2f (перерасход %.2f).",
                        budget.getName(), spent, limit, spent - limit
                );
            } else if (spent >= 0.9 * limit) {
                return String.format(
                        "Осторожно: вы почти исчерпали бюджет по категории '%s'. Потрачено %.2f из %.2f.",
                        budget.getName(), spent, limit
                );
            }
        }
        return null;
    }

    public void setBudget(UserAccount user, String category, double limit) {
        validateAmount(limit);
        validateCategory(category);
        user.getWallet().setBudget(category, limit);
    }

    /**
     * Строит строки сводки по категориям и бюджетам.
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
        lines.add("Бюджеты и расходы по категориям:");

        Map<String, Double> spentByCat = wallet.getExpensesByCategoryForMonth(ym);

        // Сначала категории с бюджетами
        if (wallet.getBudgets().isEmpty()) {
            lines.add("  Бюджеты пока не заданы.");
        } else {
            for (CategoryBudget budget : wallet.getBudgets().values()) {
                String key = budget.getName().trim().toLowerCase();
                double spent = spentByCat.getOrDefault(key, 0.0);
                double limit = budget.getLimit();
                double remaining = limit - spent;
                lines.add(String.format(
                        "  %-15s: потрачено %.2f из %.2f, остаток %.2f",
                        budget.getName(), spent, limit, remaining
                ));
            }
        }

        // Категории, где были расходы, но бюджета нет
        for (Map.Entry<String, Double> entry : spentByCat.entrySet()) {
            String catKey = entry.getKey();
            if (!wallet.getBudgets().containsKey(catKey)) {
                lines.add(String.format(
                        "  %-15s: потрачено %.2f (бюджет не задан)",
                        catKey, entry.getValue()
                ));
            }
        }

        return lines;
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
