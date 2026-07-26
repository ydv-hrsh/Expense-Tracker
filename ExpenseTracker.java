package com.mycompany.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/** A Core Java, menu-driven expense tracker. Information is retained while the program runs. */
public class ExpenseTracker {
    private static final List<Expense> expenses = new ArrayList<>();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static int nextId = 1;
    private static BigDecimal monthlyBudget = BigDecimal.ZERO;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            int choice;
            do {
                printMenu();
                choice = readInt(scanner, "Enter your choice: ");

                switch (choice) {
                    case 1 -> addExpense(scanner);
                    case 2 -> updateExpense(scanner);
                    case 3 -> deleteExpense(scanner);
                    case 4 -> viewAllExpenses();
                    case 5 -> searchExpenses(scanner);
                    case 6 -> viewMonthlySummary(scanner);
                    case 7 -> viewTotalSummary();
                    case 8 -> filterByCategory(scanner);
                    case 9 -> setMonthlyBudget(scanner);
                    case 10 -> viewRemainingBudget(scanner);
                    case 11 -> System.out.println("\nGoodbye!");
                    default -> System.out.println("\nPlease enter a number from 1 to 11.");
                }
            } while (choice != 11);
        }
    }

    private static void printMenu() {
        System.out.println("""

                =========================================
                          EXPENSE TRACKER
                =========================================
                1. Add Expense
                2. Update Expense
                3. Delete Expense
                4. View All Expenses
                5. Search Expenses
                6. View Monthly Summary
                7. View Total Summary
                8. Filter by Category
                9. Set Monthly Budget
                10. View Remaining Budget
                11. Exit
                """);
    }

    private static void addExpense(Scanner scanner) {
        System.out.println("\n------------ ADD EXPENSE ------------");
        String title = readRequiredText(scanner, "Enter expense title:");
        String category = readRequiredText(scanner, "Enter category (Food, Travel, Bills, etc.):");
        BigDecimal amount = readPositiveAmount(scanner, "Enter amount: ");
        LocalDate date = readDate(scanner, "Enter date (dd-MM-yyyy), or press Enter for today: ", true);
        String note = readOptionalText(scanner, "Enter note (optional): ");

        Expense expense = new Expense(nextId++, title, category, amount, date, note);
        expenses.add(expense);
        System.out.println("Expense added successfully. ID: " + expense.getId());
    }

    private static void updateExpense(Scanner scanner) {
        System.out.println("\n------------ UPDATE EXPENSE ------------");
        Expense expense = findExpenseById(readInt(scanner, "Enter expense ID: "));
        if (expense == null) {
            System.out.println("Expense not found.");
            return;
        }

        System.out.println("Press Enter to keep the current value.");
        String title = readOptionalText(scanner, "New title [" + expense.getTitle() + "]: ");
        String category = readOptionalText(scanner, "New category [" + expense.getCategory() + "]: ");
        String amountText = readOptionalText(scanner, "New amount [" + money(expense.getAmount()) + "]: ");
        String dateText = readOptionalText(scanner, "New date (dd-MM-yyyy) [" + expense.getDate().format(DATE_FORMAT) + "]: ");
        String note = readOptionalText(scanner, "New note [" + expense.getNote() + "]: ");

        if (!title.isEmpty()) expense.setTitle(title);
        if (!category.isEmpty()) expense.setCategory(category);
        updateAmount(expense, amountText);
        updateDate(expense, dateText);
        if (!note.isEmpty()) expense.setNote(note);
        System.out.println("Expense updated successfully.");
    }

    private static void updateAmount(Expense expense, String amountText) {
        if (amountText.isEmpty()) return;
        try {
            BigDecimal amount = new BigDecimal(amountText);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                expense.setAmount(amount);
            } else {
                System.out.println("Amount must be greater than zero; old amount was kept.");
            }
        } catch (NumberFormatException exception) {
            System.out.println("Invalid amount; old amount was kept.");
        }
    }

    private static void updateDate(Expense expense, String dateText) {
        if (dateText.isEmpty()) return;
        try {
            expense.setDate(LocalDate.parse(dateText, DATE_FORMAT));
        } catch (DateTimeParseException exception) {
            System.out.println("Invalid date; old date was kept.");
        }
    }

    private static void deleteExpense(Scanner scanner) {
        Expense expense = findExpenseById(readInt(scanner, "\nEnter expense ID to delete: "));
        if (expense == null) {
            System.out.println("Expense not found.");
            return;
        }
        expenses.remove(expense);
        System.out.println("Expense deleted successfully.");
    }

    private static void viewAllExpenses() {
        System.out.println("\n============== ALL EXPENSES ==============");
        expenses.sort(Comparator.comparing(Expense::getDate).reversed());
        printExpenses(expenses);
    }

    private static void searchExpenses(Scanner scanner) {
        String keyword = readRequiredText(scanner, "\nSearch title, category, or note:").toLowerCase();
        List<Expense> matches = new ArrayList<>();
        for (Expense expense : expenses) {
            if (expense.getTitle().toLowerCase().contains(keyword)
                    || expense.getCategory().toLowerCase().contains(keyword)
                    || expense.getNote().toLowerCase().contains(keyword)) {
                matches.add(expense);
            }
        }
        System.out.println("\n============== SEARCH RESULTS ==============");
        printExpenses(matches);
    }

    private static void viewMonthlySummary(Scanner scanner) {
        YearMonth month = readMonth(scanner, "Enter month (MM-yyyy), or press Enter for current month: ");
        List<Expense> monthlyExpenses = expensesForMonth(month);
        System.out.println("\n========== SUMMARY: " + month + " ==========");
        printExpenses(monthlyExpenses);
        System.out.println("Total spent: " + money(totalOf(monthlyExpenses)));
    }

    private static void viewTotalSummary() {
        System.out.println("\n========== TOTAL SUMMARY ==========");
        System.out.println("Number of expenses: " + expenses.size());
        System.out.println("Total spent: " + money(totalOf(expenses)));
    }

    private static void filterByCategory(Scanner scanner) {
        String category = readRequiredText(scanner, "\nEnter category to filter:");
        List<Expense> matches = new ArrayList<>();
        for (Expense expense : expenses) {
            if (expense.getCategory().equalsIgnoreCase(category)) {
                matches.add(expense);
            }
        }
        System.out.println("\n========== CATEGORY: " + category + " ==========");
        printExpenses(matches);
        System.out.println("Category total: " + money(totalOf(matches)));
    }

    private static void setMonthlyBudget(Scanner scanner) {
        monthlyBudget = readPositiveAmount(scanner, "\nEnter monthly budget: ");
        System.out.println("Monthly budget set to " + money(monthlyBudget));
    }

    private static void viewRemainingBudget(Scanner scanner) {
        if (monthlyBudget.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("\nSet a monthly budget first (option 9).");
            return;
        }
        YearMonth month = readMonth(scanner, "Enter month (MM-yyyy), or press Enter for current month: ");
        BigDecimal spent = totalOf(expensesForMonth(month));
        BigDecimal remaining = monthlyBudget.subtract(spent);
        System.out.println("\n========== BUDGET: " + month + " ==========");
        System.out.println("Budget: " + money(monthlyBudget));
        System.out.println("Spent: " + money(spent));
        System.out.println(remaining.signum() >= 0
                ? "Remaining: " + money(remaining)
                : "Over budget by: " + money(remaining.abs()));
    }

    private static void printExpenses(List<Expense> expenseList) {
        if (expenseList.isEmpty()) {
            System.out.println("No expenses found.");
            return;
        }
        for (Expense expense : expenseList) {
            System.out.println("\nID       : " + expense.getId());
            System.out.println("Title    : " + expense.getTitle());
            System.out.println("Category : " + expense.getCategory());
            System.out.println("Amount   : " + money(expense.getAmount()));
            System.out.println("Date     : " + expense.getDate().format(DATE_FORMAT));
            if (!expense.getNote().isEmpty()) System.out.println("Note     : " + expense.getNote());
            System.out.println("-------------------------------------------");
        }
    }

    private static List<Expense> expensesForMonth(YearMonth month) {
        List<Expense> matches = new ArrayList<>();
        for (Expense expense : expenses) {
            if (YearMonth.from(expense.getDate()).equals(month)) matches.add(expense);
        }
        return matches;
    }

    private static BigDecimal totalOf(List<Expense> expenseList) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense expense : expenseList) total = total.add(expense.getAmount());
        return total;
    }

    private static Expense findExpenseById(int id) {
        for (Expense expense : expenses) if (expense.getId() == id) return expense;
        return null;
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readRequiredText(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + "\n> ");
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private static String readOptionalText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static BigDecimal readPositiveAmount(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
                if (amount.compareTo(BigDecimal.ZERO) > 0) return amount;
            } catch (NumberFormatException ignored) {
                // The message below explains both invalid input and zero/negative amounts.
            }
            System.out.println("Enter a valid amount greater than zero.");
        }
    }

    private static LocalDate readDate(Scanner scanner, String prompt, boolean allowToday) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (allowToday && input.isEmpty()) return LocalDate.now();
            try {
                return LocalDate.parse(input, DATE_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("Use the format dd-MM-yyyy, for example 26-07-2026.");
            }
        }
    }

    private static YearMonth readMonth(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return YearMonth.now();
            try {
                String[] parts = input.split("-");
                return YearMonth.of(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            } catch (RuntimeException exception) {
                System.out.println("Use the format MM-yyyy, for example 07-2026.");
            }
        }
    }

    private static String money(BigDecimal amount) {
        return "₹" + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static class Expense {
        private final int id;
        private String title;
        private String category;
        private BigDecimal amount;
        private LocalDate date;
        private String note;

        Expense(int id, String title, String category, BigDecimal amount, LocalDate date, String note) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.amount = amount;
            this.date = date;
            this.note = note;
        }

        int getId() { return id; }
        String getTitle() { return title; }
        String getCategory() { return category; }
        BigDecimal getAmount() { return amount; }
        LocalDate getDate() { return date; }
        String getNote() { return note; }
        void setTitle(String title) { this.title = title; }
        void setCategory(String category) { this.category = category; }
        void setAmount(BigDecimal amount) { this.amount = amount; }
        void setDate(LocalDate date) { this.date = date; }
        void setNote(String note) { this.note = note; }
    }
}
