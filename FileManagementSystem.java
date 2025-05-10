import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class FileManagementSystem {

    static final long DISK_SPACE = 10240;
    static final int MAX_USERS = 10;
    static final int MAX_OPEN_FILES = 5;
    static final String FILE_STORAGE_DIR = "file_storage/";

    static Map<String, User> users = new HashMap<>();
    static Map<User, List<String>> openFiles = new ConcurrentHashMap<>();
    static long usedDiskSpace = 0;
    static Semaphore semaphore = new Semaphore(1);
    static Scanner scanner = new Scanner(System.in);

    static {
        new File(FILE_STORAGE_DIR).mkdirs();
    }

    static class User {
        String username;
        String password;
        boolean isAdmin;

        User(String username, String password, boolean isAdmin) {
            this.username = username;
            this.password = password;
            this.isAdmin = isAdmin;
        }
    }

    static void addUser(String username, String password, boolean isAdmin) {
        users.put(username, new User(username, password, isAdmin));
        openFiles.put(users.get(username), new ArrayList<>());
        System.out.println("User '" + username + "' added successfully.");
    }

    static boolean authenticateUser(String username, String password) {
        return users.containsKey(username) && users.get(username).password.equals(password);
    }

    static void registerUser() {
        if (users.size() >= MAX_USERS) {
            System.out.println("User limit reached. Cannot register new users.");
            return;
        }

        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        if (users.containsKey(username)) {
            System.out.println("Username already exists. Please try another.");
            return;
        }

        System.out.print("Enter new password: ");
        String password = scanner.nextLine();
        addUser(username, password, false);
    }

    static void userMenu() {
        while (true) {
            System.out.println("\n=== MAIN MENU ===");
            System.out.println("1. Login");
            System.out.println("2. Exit");
            System.out.println("3. Register");

            System.out.print("Choose an option: ");
            int choice;

            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                continue;
            }

            switch (choice) {
                case 1 -> {
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String password = scanner.nextLine();
                    if (authenticateUser(username, password)) {
                        User user = users.get(username);
                        System.out.println("\nLogin successful! Welcome, " + username + ".");
                        fileOperationsMenu(user);
                    } else {
                        System.out.println("Invalid username or password.");
                    }
                }
                case 2 -> {
                    System.out.println("Exiting system... Goodbye!");
                    System.exit(0);
                }
                case 3 -> registerUser();
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    static void fileOperationsMenu(User user) {
        while (true) {
            System.out.println("\n=== FILE OPERATIONS ===");
            System.out.println("1. Create File");
            System.out.println("2. Read File");
            System.out.println("3. Write File");
            System.out.println("4. Delete File");
            System.out.println("5. Display System Status");
            System.out.println("6. Open File");
            System.out.println("7. Close File");
            System.out.println("8. Share File");
            System.out.println("9. List All Files");
            System.out.println("10. Logout");

            System.out.print("Choose an option: ");
            int choice;

            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.nextLine();
                continue;
            }

            switch (choice) {
                case 1 -> createFile(user);
                case 2 -> readFile(user);
                case 3 -> writeFile(user);
                case 4 -> deleteFile(user);
                case 5 -> displaySystemStatus();
                case 6 -> {
                    System.out.print("Enter file name to open: ");
                    openFile(user, scanner.nextLine());
                }
                case 7 -> {
                    System.out.print("Enter file name to close: ");
                    closeFile(user, scanner.nextLine());
                }
                case 8 -> {
                    System.out.print("Enter file name to share: ");
                    String fileName = scanner.nextLine();
                    System.out.print("Enter target username: ");
                    String targetUserName = scanner.nextLine();
                    shareFile(user, fileName, targetUserName);
                }
                case 9 -> listAllFiles();
                case 10 -> {
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    static void createFile(User user) {
        try {
            semaphore.acquire();
            System.out.print("Enter file name: ");
            String fileName = scanner.nextLine();
            Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

            if (Files.exists(filePath)) {
                System.out.println("Error: File already exists.");
                return;
            }

            System.out.print("Enter file content: ");
            String content = scanner.nextLine();
            byte[] bytes = content.getBytes();

            if (usedDiskSpace + bytes.length > DISK_SPACE) {
                System.out.println("Error: Not enough disk space.");
                return;
            }

            Files.write(filePath, bytes);
            usedDiskSpace += bytes.length;
            System.out.println("File created successfully at: " + filePath.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("Error creating file: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    static void readFile(User user) {
        System.out.print("Enter file name: ");
        String fileName = scanner.nextLine();
        Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

        if (!Files.exists(filePath)) {
            System.out.println("Error: File not found.");
            return;
        }

        try {
            String content = new String(Files.readAllBytes(filePath));
            System.out.println("\n=== FILE CONTENT ===");
            System.out.println(content);
            System.out.println("====================");
            System.out.println("File size: " + Files.size(filePath) + " bytes");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    static void writeFile(User user) {
        try {
            semaphore.acquire();
            System.out.print("Enter file name: ");
            String fileName = scanner.nextLine();
            Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

            if (!Files.exists(filePath)) {
                System.out.println("Error: File not found.");
                return;
            }

            long oldSize = Files.size(filePath);
            System.out.print("Enter new content: ");
            String newContent = scanner.nextLine();
            byte[] newBytes = newContent.getBytes();

            if (usedDiskSpace - oldSize + newBytes.length > DISK_SPACE) {
                System.out.println("Error: Not enough disk space.");
                return;
            }

            Files.write(filePath, newBytes, StandardOpenOption.TRUNCATE_EXISTING);
            usedDiskSpace = usedDiskSpace - oldSize + newBytes.length;
            System.out.println("File updated successfully.");
        } catch (IOException | InterruptedException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    static void deleteFile(User user) {
        try {
            semaphore.acquire();
            System.out.print("Enter file name: ");
            String fileName = scanner.nextLine();
            Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

            if (!Files.exists(filePath)) {
                System.out.println("Error: File not found.");
                return;
            }

            long fileSize = Files.size(filePath);
            Files.delete(filePath);
            usedDiskSpace -= fileSize;
            System.out.println("File deleted successfully.");
        } catch (IOException | InterruptedException e) {
            System.out.println("Error deleting file: " + e.getMessage());
        } finally {
            semaphore.release();
        }
    }

    static void openFile(User user, String fileName) {
        Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

        if (!Files.exists(filePath)) {
            System.out.println("Error: File not found.");
            return;
        }

        List<String> userOpenFiles = openFiles.get(user);
        if (userOpenFiles.size() >= MAX_OPEN_FILES) {
            System.out.println("Error: Max open files reached.");
            return;
        }

        if (userOpenFiles.contains(fileName)) {
            System.out.println("File already open.");
            return;
        }

        userOpenFiles.add(fileName);
        System.out.println("File '" + fileName + "' opened.");
    }

    static void closeFile(User user, String fileName) {
        List<String> userOpenFiles = openFiles.get(user);
        if (!userOpenFiles.contains(fileName)) {
            System.out.println("File is not open.");
            return;
        }

        userOpenFiles.remove(fileName);
        System.out.println("File '" + fileName + "' closed.");
    }

    static void shareFile(User user, String fileName, String targetUserName) {
        Path filePath = Paths.get(FILE_STORAGE_DIR + fileName);

        if (!Files.exists(filePath)) {
            System.out.println("Error: File not found.");
            return;
        }

        if (!users.containsKey(targetUserName)) {
            System.out.println("Error: Target user not found.");
            return;
        }

        System.out.println("File '" + fileName + "' shared with " + targetUserName + ".");
    }

    static void displaySystemStatus() {
        System.out.println("\n=== SYSTEM STATUS ===");
        System.out.println("Total disk space: " + DISK_SPACE + " bytes");
        System.out.println("Used disk space: " + usedDiskSpace + " bytes");
        System.out.println("Available space: " + (DISK_SPACE - usedDiskSpace) + " bytes");
        System.out.println("Number of users: " + users.size());

        File[] files = new File(FILE_STORAGE_DIR).listFiles();
        System.out.println("Number of files: " + (files != null ? files.length : 0));
    }

    static void listAllFiles() {
        System.out.println("\n=== FILES IN SYSTEM ===");
        File dir = new File(FILE_STORAGE_DIR);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files available.");
        } else {
            for (File file : files) {
                System.out.println("- " + file.getName());
            }
        }
    }

    static void concurrencyTest() {
        System.out.println("\n--- Running Concurrency Test ---");

        Runnable task1 = () -> {
            try {
                semaphore.acquire();
                Path file = Paths.get(FILE_STORAGE_DIR + "concurrent_test.txt");
                Files.write(file, "User1 writing...\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.println("User1 finished writing.");
            } catch (Exception e) {
                System.out.println("User1 error: " + e.getMessage());
            } finally {
                semaphore.release();
            }
        };

        Runnable task2 = () -> {
            try {
                semaphore.acquire();
                Path file = Paths.get(FILE_STORAGE_DIR + "concurrent_test.txt");
                Files.write(file, "User2 writing...\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.println("User2 finished writing.");
            } catch (Exception e) {
                System.out.println("User2 error: " + e.getMessage());
            } finally {
                semaphore.release();
            }
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            System.out.println("Concurrency test interrupted.");
        }

        System.out.println("--- Concurrency Test Complete ---");
    }

    public static void main(String[] args) {
        System.out.println("=== Advanced File Management System ===");
        System.out.println("Initializing system...");
        addUser("admin", "admin123", true);

        // Optional: Uncomment this to test concurrency
        // concurrencyTest();

        userMenu();
    }
}
