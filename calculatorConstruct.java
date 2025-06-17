import java.util.*;
import java.util.regex.*;
public class calculatorConstruct {
    static final Scanner sc = new Scanner(System.in);
    static final List<Double> sharedNumbers = new ArrayList<>();
    public static void main(String[] args) {
        collectInput();
        boolean exit = false;
        while (!exit) {
            System.out.println("\n=Main Menu =");
            System.out.println("1. Simple calculation");
            System.out.println("2. ArrayList operations");
            System.out.println("3. LinkedList operations");
            System.out.println("4. Queue operations");
            System.out.println("5. Evaluate expression");
            System.out.println("6. Exit");
            System.out.print("Choose an option (1–6): ");
            switch (sc.nextLine().trim()) {
                case "1" -> runStepByStepCalc();
                case "2" -> handleList(new ArrayList<>(sharedNumbers), "ArrayList");
                case "3" -> handleList(new LinkedList<>(sharedNumbers), "LinkedList");
                case "4" -> handleQueue();
                case "5" -> calculateExpression();
                case "6" -> exit = true;
                default -> System.out.println("Invalid choice – please select 1 to 6.");
            }
        }
        System.out.println("Goodbye!");
        sc.close();
    }
    private static void collectInput() {
        System.out.print("How many numbers do you want to input? ");
        int n = inputInt();
        System.out.println("Enter " + n + " numbers:");
        for (int i = 1; i <= n; i++) {
            sharedNumbers.add(inputDouble("  #" + i + ": "));
        }
    }
    private static void runStepByStepCalc() {
        Scanner scLocal = new Scanner(System.in);
        double result = 0;
        boolean validFirstInput = false;
        List<Double> numbers = new ArrayList<>();
        while (!validFirstInput) {
            try {
                System.out.print("Enter the first number: ");
                result = Double.parseDouble(scLocal.next());
                numbers.add(result);
                validFirstInput = true;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please enter a valid numeric value:");
            }
        }
        while (true) {
            System.out.print("Choose an operation (+, -, *, /): ");
            char operator = scLocal.next().charAt(0);
            double nextNumber = 0;
            boolean validNextInput = false;
            while (!validNextInput) {
                try {
                    System.out.print("Enter the next number: ");
                    nextNumber = Double.parseDouble(scLocal.next());
                    numbers.add(nextNumber);
                    validNextInput = true;
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number. Please enter a valid numeric value:");
                }
            }
            switch (operator) {
                case '+' -> result += nextNumber;
                case '-' -> result -= nextNumber;
                case '*' -> result *= nextNumber;
                case '/' -> {
                    if (nextNumber != 0) {
                        result /= nextNumber;
                    } else {
                        System.out.println("Error: Division by zero is not allowed.");
                        numbers.remove(numbers.size() - 1);
                        continue;
                    }
                }
                default -> {
                    System.out.println("Invalid operator. Please use +, -, *, or /.");
                    numbers.remove(numbers.size() - 1);
                    continue;
                }
            }
            System.out.print("Do you want to enter another number? (yes/no): ");
            String response = scLocal.next().toLowerCase();
            if (!response.equals("yes")) {
                break;
            }
        }
        numbers.add(result);
        Collections.sort(numbers);
        List<Double> evenNumbers = new ArrayList<>();
        List<Double> oddNumbers = new ArrayList<>();
        for (double num : numbers) {
            if (num % 1 == 0) {
                if (((int) num) % 2 == 0) evenNumbers.add(num);
                else oddNumbers.add(num);
            }
        }
        System.out.println("Result: " + result);
        System.out.println("Sorted numbers including result: " + numbers);
        System.out.println("Even numbers: " + evenNumbers);
        System.out.println("Odd numbers: " + oddNumbers);
    }
    private static void handleList(List<Double> list, String name) {
        System.out.println(name + " contents: " + list);
        List<Double> evens = new ArrayList<>(), odds = new ArrayList<>();
        for (double v : list) {
            if (((int) v) % 2 == 0) evens.add(v);
            else odds.add(v);
        }
        System.out.println("Even numbers: " + evens);
        System.out.println("Odd numbers: " + odds);
        if (askYesNo("Sort the list? (y/n): ")) {
            Collections.sort(list);
            System.out.println("Sorted " + name + ": " + list);
        }
    }
    private static void handleQueue() {
        System.out.print("Enter the size of the queue: ");
        int queueSize = inputInt();
        Queue<Double> q = new LinkedList<>();
        int rotationCount = 0;
        int addedSinceLastRotation = 0;
        for (Double num : sharedNumbers) {
            if (q.size() == queueSize) {
                q.poll();
            }
            q.offer(num);
            addedSinceLastRotation++;

            if (addedSinceLastRotation == queueSize) {
                rotationCount++;
                addedSinceLastRotation = 0;
            }
        }
        System.out.println("\nFinal Queue (capacity " + queueSize + "): " + q);
        System.out.println("Total full rotations performed: " + rotationCount);
    }
    private static void calculateExpression() {
        System.out.println("Expression Calculator");
        do {
            String expr = inputExpression();
            try {
                double result = evaluate(expr);
                System.out.println("Result: " + result);
            } catch (Exception ex) {
                System.out.println("Evaluation failed: " + ex.getMessage());
            }
        } while (askYesNo("Perform another calculation? (yes/no): "));
        System.out.println("Back to Main Menu.");
    }
    private static String inputExpression() {
        while (true) {
            System.out.print("Enter expression: ");
            String expr = sc.nextLine().trim();
            int pos = findUnmatched(expr);
            if (pos != -1) {
                char at = expr.charAt(pos);
                System.out.println("Unmatched '" + at + "' at position " + pos);
                int idx = askInsertPosition(expr.length(), at == '(' ? ")" : "(");
                expr = expr.substring(0, idx) + (at == '(' ? ")" : "(") + expr.substring(idx);
                System.out.println("Corrected expression: " + expr);
            }
            String corrected = validateNumbers(expr);
            if (corrected != null) {
                expr = corrected;
                continue;
            }
            return expr;
        }
    }
    private static int askInsertPosition(int length, String parenDesc) {
        while (true) {
            System.out.printf("Where do you want to insert %s? Enter position [0..%d]: ", parenDesc, length);
            try {
                int idx = Integer.parseInt(sc.nextLine());
                if (idx >= 0 && idx <= length) return idx;
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid position — try again.");
        }
    }
    private static int findUnmatched(String s) {
        Stack<Integer> st = new Stack<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') st.push(i);
            else if (c == ')') {
                if (st.isEmpty()) return i;
                st.pop();
            }
        }
        return st.isEmpty() ? -1 : st.pop();
    }
    private static String validateNumbers(String expr) {
        StringBuilder sb = new StringBuilder();
        boolean needFix = false;
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?|\\S)");
        Matcher matcher = pattern.matcher(expr);
        while (matcher.find()) {
            String token = matcher.group();
            try {
                if (token.matches("\\d+(\\.\\d+)?")) Double.parseDouble(token);
                sb.append(token);
            } catch (NumberFormatException ex) {
                needFix = true;
                System.out.printf("Invalid number '%s'. Enter replacement: ", token);
                sb.append(inputValidNumber());
            }
        }
        return needFix ? sb.toString() : null;
    }
    private static String inputValidNumber() {
        while (true) {
            String s = sc.nextLine().trim();
            if (s.matches("\\d+(\\.\\d+)?")) return s;
            System.out.print("Invalid number, try again: ");
        }
    }
    private static boolean askYesNo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim().toLowerCase();
            if (s.equals("y") || s.equals("yes")) return true;
            if (s.equals("n") || s.equals("no")) return false;
        }
    }
    private static double evaluate(String expr) {
        Deque<Double> vals = new ArrayDeque<>();
        Deque<Character> ops = new ArrayDeque<>();
        for (int i = 0; i < expr.length();) {
            char c = expr.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < expr.length() && (Character.isDigit(expr.charAt(j)) || expr.charAt(j) == '.')) j++;
                vals.push(Double.parseDouble(expr.substring(i, j)));
                i = j;
            } else if (c == '(') {
                ops.push(c);
                i++;
            } else if (c == ')') {
                while (ops.peek() != '(') processOp(vals, ops);
                ops.pop();
                i++;
            } else if ("+-*/%^".indexOf(c) >= 0) {
                while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(c)) processOp(vals, ops);
                ops.push(c);
                i++;
            } else {
                throw new RuntimeException("Invalid character: " + c);
            }
        }
        while (!ops.isEmpty()) processOp(vals, ops);
        return vals.pop();
    }
    private static void processOp(Deque<Double> vals, Deque<Character> ops) {
        char op = ops.pop();
        double b = vals.pop(), a = vals.pop();
        switch (op) {
            case '+' -> vals.push(a + b);
            case '-' -> vals.push(a - b);
            case '*' -> vals.push(a * b);
            case '/' -> {
                if (b == 0) throw new ArithmeticException("Division by zero");
                vals.push(a / b);
            }
            case '%' -> vals.push(a % b);
            case '^' -> vals.push(Math.pow(a, b));
            default -> throw new RuntimeException("Unknown operator " + op);
        }
    }
    private static int precedence(char op) {
        return switch (op) {
            case '+', '-' -> 1;
            case '*', '/', '%' -> 2;
            case '^' -> 3;
            default -> -1;
        };
    }
    private static double inputDouble(String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid number, try again: ");
            }
        }
    }
    private static int inputInt() {
        while (true) {
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("Invalid integer, try again: ");
            }
        }
    }
}
