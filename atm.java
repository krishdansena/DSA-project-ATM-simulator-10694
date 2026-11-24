import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ATMSimulator {
    private static final Path DATA_FILE = Path.of("accounts.txt");
    private static final int[] NOTES = {500, 200, 100};
    private static final Scanner INPUT = new Scanner(System.in);
    private static final Map<String, Account> accounts = new LinkedHashMap<>();
    private static final Deque<String> customerQueue = new ArrayDeque<>();
    private static final Deque<String> transactions = new ArrayDeque<>();
    private static Account activeAccount;

    public static void main(String[] args) {
        loadAccounts();
        if (accounts.isEmpty()) {
            accounts.put("ACC101", new Account("ACC101", "Riya", "1234", 5000));
            saveAccounts();
        }
        // Add existing accounts to the service queue in file order.
        customerQueue.addAll(accounts.keySet());

        System.out.println("=== ATM Simulator ===");
        System.out.println("Customers waiting: " + customerQueue.size());
        while (true) {
            showMainMenu();
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1 -> serveNextCustomer();
                case 2 -> showQueue();
                case 0 -> {
                    saveAccounts();
                    System.out.println("Goodbye.");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void showMainMenu() {
        System.out.println("\n1. Serve next customer");
        System.out.println("2. View customer queue");
        System.out.println("0. Exit");
    }

    private static void serveNextCustomer() {
        String accountNumber = customerQueue.poll();
        if (accountNumber == null) {
            System.out.println("No customers are waiting.");
            return;
        }
        Account account = accounts.get(accountNumber);
        System.out.println("Serving: " + account.name + " (" + account.number + ")");
        if (!login(account)) {
            return;
        }
        activeAccount = account;
        System.out.println("Login successful. Welcome, " + account.name + ".");
        System.out.println("Balance: " + account.balance);
        accountMenu();
        activeAccount = null;
    }

    private static boolean login(Account account) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            String pin = readLine("PIN: ").trim();
            if (account.pin.equals(pin)) return true;
            int attemptsLeft = 3 - attempt;
            if (attemptsLeft > 0) {
                System.out.println("Incorrect PIN. Attempts left: " + attemptsLeft);
            } else {
                System.out.println("Incorrect PIN. Card blocked after 3 attempts.");
            }
        }
        return false;
    }
    private static void accountMenu() {
        while (activeAccount != null) {
            System.out.println("\n1. Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Mini statement");
            System.out.println("5. Finish session");
            int choice = readInt("Choose an option: ");
            switch (choice) {
                case 1 -> System.out.println("Balance: " + activeAccount.balance);
                case 2 -> deposit();
                case 3 -> withdraw();
                case 4 -> miniStatement();
                case 5 -> { saveAccounts(); return; }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private static void deposit() {
        int amount = readInt("Deposit amount: ");
        if (amount <= 0) {
            System.out.println("Amount must be greater than 0.");
            return;
        }
        activeAccount.balance += amount;
        addTransaction("Deposit " + amount + " | Balance " + activeAccount.balance);
        saveAccounts();
        System.out.println("Deposit successful. Balance: " + activeAccount.balance);
    }

    private static void withdraw() {
        int amount = readInt("Withdrawal amount: ");
        if (amount <= 0 || amount % 100 != 0) {
            System.out.println("Enter an amount greater than 0 in multiples of 100.");
            return;
        }
        if (amount > activeAccount.balance) {
            System.out.println("Insufficient balance. Balance: " + activeAccount.balance);
            return;
        }

        int remaining = amount;
        List<String> dispensed = new ArrayList<>();
        for (int note : NOTES) {
            int count = remaining / note;
            if (count > 0) {
                dispensed.add(note + " x " + count);
                remaining %= note;
            }
        }
        if (remaining != 0) {
            System.out.println("Unable to dispense that amount with available denominations.");
            return;
        }
        activeAccount.balance -= amount;
        addTransaction("Withdraw " + amount + " | Balance " + activeAccount.balance);
        saveAccounts();
        System.out.println("Dispensed: " + String.join(", ", dispensed));
        System.out.println("Balance: " + activeAccount.balance);
    }

    private static void miniStatement() {
        System.out.println("Mini statement (latest first):");
        if (transactions.isEmpty()) {
            System.out.println("No transactions yet.");
            return;
        }
        int count = 0;
        for (String transaction : transactions) {
            if (transaction.startsWith(activeAccount.number + "|")) {
                System.out.println("- " + transaction.substring(transaction.indexOf('|') + 1).trim());
                if (++count == 5) break;
            }
        }
        if (count == 0) System.out.println("No transactions yet.");
    }

    private static void addTransaction(String description) {
        transactions.push(activeAccount.number + "|" + description);
    }

    private static void showQueue() {
        if (customerQueue.isEmpty()) {
            System.out.println("No customers are waiting.");
            return;
        }
        System.out.println("Queue:");
        for (String number : customerQueue) {
            Account a = accounts.get(number);
            System.out.println(a.name + " (" + a.number + ")");
        }
    }

    private static void loadAccounts() {
        if (!Files.exists(DATA_FILE)) return;
        try {
            for (String line : Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split(",", -1);
                if (fields.length != 4) continue;
                try {
                    Account account = new Account(fields[0].trim(), fields[1].trim(),
                            fields[2].trim(), Integer.parseInt(fields[3].trim()));
                    accounts.put(account.number, account);
                } catch (NumberFormatException ignored) {
                    System.out.println("Skipping invalid account record: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read accounts.txt: " + e.getMessage());
        }
    }

    private static void saveAccounts() {
        List<String> lines = new ArrayList<>();
        lines.add("# accountNumber,name,pin,balance");
        for (Account a : accounts.values()) {
            lines.add(a.number + "," + a.name + "," + a.pin + "," + a.balance);
        }
        try {
            Files.write(DATA_FILE, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Could not save accounts.txt: " + e.getMessage());
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = readLine("").trim();
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static String readLine(String prompt) {
        if (!prompt.isEmpty()) System.out.print(prompt);
        return INPUT.nextLine();
    }

    private static class Account {
        final String number;
        final String name;
        final String pin;
        int balance;

        Account(String number, String name, String pin, int balance) {
            this.number = number;
            this.name = name;
            this.pin = pin;
            this.balance = balance;
        }
    }
}