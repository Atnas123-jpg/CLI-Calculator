import java.util.Scanner;
import java.util.ArrayList;

public class Calculator {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ArrayList<String> history = new ArrayList<>();

        System.out.println("=====================================");
        System.out.println("         SIMPLE CALCULATOR    ");
        System.out.println("=====================================");
        System.out.println("You can type things like: 2+5*3");
        System.out.println("You can also use brackets: (2+5)*3");
        System.out.println("Power: 2^3 Square root: √9 Percentage: 50%");
        System.out.println("_________________________________");
        System.out.println("Commands:");
        System.out.println("history  - view history");
        System.out.println("clear - clear history");
        System.out.println("exit - close the program");
        System.out.println("_________________________________");
        System.out.println();

        while (true) {
            System.out.println("Enter calculation: ");
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Calculator closed.");
                break;
            }
            if (input.equalsIgnoreCase("history")) {
                showHistory(history);
                continue;
            }
            if (input.equalsIgnoreCase("clear")) {
                history.clear();
                System.out.println("History cleared.");
                continue;
            }

            try {
                // Step 1: break the text into small pieces (numbers and symbols)
                ArrayList<String> pieces = splitIntoPieces(input);

                // Step 2: solve those pieces to get one final answer
                double answer = solveBrackets(pieces);
                System.out.println("Answer = " + answer);
                history.add(input + " = " + answer);

           } catch (ArithmeticException error) {
                System.out.println("Math error: " + error.getMessage());
            } catch (Exception error) {
                System.out.println("Invalid calculation.");
            }
            System.out.println();
        }
        scanner.close();
    }

    // Turn 2+5*3 into separate pieces: 2, +, 5, *, 3
    // Each piece is either a number or a symbol like + - * / ( )
    static ArrayList<String> splitIntoPieces(String input) {

        ArrayList<String> pieces = new ArrayList<>();
        String currentNumber = "";

        for (int i = 0; i < input.length(); i++) {
            char letter = input.charAt(i);
            if (letter == ' ') {
                continue; // ignore spaces
            }

            if (Character.isDigit(letter) || letter == '.') {
                // keep building the number, digit by digit
                currentNumber = currentNumber + letter;
            } else {
                // we hit a symbol, so the number before it is finished
                if (currentNumber.length() > 0) {
                    pieces.add(currentNumber);
                    currentNumber = "";
                }
                // add the symbol itself as its own piece
               pieces.add(String.valueOf(letter));
            }
        }
        // add the last number if there is one left over
        if (currentNumber.length() > 0) {
            pieces.add(currentNumber);
        }
        return pieces;
    }

    // Solve brackets first, starting with the innermost pair
    static double solveBrackets(ArrayList<String> pieces) {
        while (pieces.contains("(")) {
            int closePosition = pieces.indexOf(")");
            int openPosition = closePosition;

            // walk backwards until we find the matching "("
            while (!pieces.get(openPosition).equals("(")) {
                openPosition = openPosition - 1;
            }
            // take out just the pieces inside the brackets
            ArrayList<String> insidePieces =
                    new ArrayList<>(pieces.subList(openPosition + 1, closePosition));

            double insideAnswer = solveExpression(insidePieces);

            // remove the "(" ... ")" and everything inside it
            for (int i = closePosition; i >= openPosition; i--) {
                pieces.remove(i);
            }

            // put the answer back in its place, as one single number
            pieces.add(openPosition, String.valueOf(insideAnswer));
        }
        return solveExpression(pieces);
    }

    // Solve everything else, in order: square root, then percentage, then power,
    // then multiply/divide, then add/subtract
    static double solveExpression(ArrayList<String> pieces) {

        resolveNegatives(pieces);
        resolveSquareRoots(pieces);
        resolvePercent(pieces);

        return solvePower(pieces);
    }

    // Join a "-" sign with the number after it when it means a negative number
    // Example: "-", "5"  becomes  "-5"
    // This happens when "-" is at the start, or comes right after another symbol (like 3*-2)
    static void resolveNegatives(ArrayList<String> pieces) {

        int i = 0;
        while (i < pieces.size()) {
            if (pieces.get(i).equals("-")) {

                boolean atStart = (i == 0);
                boolean afterSymbol = false;

                if (i > 0) {
                    String before = pieces.get(i - 1);
                    afterSymbol = before.equals("+") || before.equals("-")
                            || before.equals("*") || before.equals("/")
                            || before.equals("^");
                }

                // for -5^2, wait: join the minus after the power is solved
                boolean beforePower = false;

                if (i + 2 < pieces.size() && pieces.get(i + 2).equals("^")) {
                    beforePower = true;
                }

                if ((atStart || afterSymbol) && !beforePower) {
                    pieces.set(i, "-" + pieces.get(i + 1));
                    pieces.remove(i + 1);
                }
            }
            i++;
        }
    }

    // Replace every √number with its square root value
    static void resolveSquareRoots(ArrayList<String> pieces) {

        while (pieces.contains("√")) {
            int position = pieces.indexOf("√");

            double number = Double.parseDouble(pieces.get(position + 1));
            double result = Math.sqrt(number);

            pieces.remove(position + 1);
            pieces.set(position, String.valueOf(result));
        }
    }

    // Replace every number% with that number divided by 100
    static void resolvePercent(ArrayList<String> pieces) {
        int i = 0;
        while (i < pieces.size()) {
            if (pieces.get(i).equals("%")) {

                double number = Double.parseDouble(pieces.get(i - 1));
                double result = number / 100.0;

                pieces.set(i - 1, String.valueOf(result));
                pieces.remove(i);

            } else {
                i++;
            }
        }
    }

    // Solve all ^ next, going left to right
    static double solvePower(ArrayList<String> pieces) {

        int i = 0;
        while (i < pieces.size()) {
            String piece = pieces.get(i);
            if (piece.equals("^")) {

                double base = Double.parseDouble(pieces.get(i - 1));
                double exponent = Double.parseDouble(pieces.get(i + 1));
                double result = Math.pow(base, exponent);

                pieces.set(i - 1, String.valueOf(result));
                pieces.remove(i + 1);
                pieces.remove(i);

            } else {
                i++;
            }
        }

        // the power is done, now join any minus that was waiting
        resolveNegatives(pieces);

        return solveMultiplyDivide(pieces);
    }

    // Solve all * and / first, going left to right
    static double solveMultiplyDivide(ArrayList<String> pieces) {

        int i = 0;
        while (i < pieces.size()) {
            String piece = pieces.get(i);
            if (piece.equals("*") || piece.equals("/")) {

                double left = Double.parseDouble(pieces.get(i - 1));
                double right = Double.parseDouble(pieces.get(i + 1));
                double result;

                if (piece.equals("*")) {
                    result = left * right;
                } else {
                    if (right == 0) {
                        throw new ArithmeticException("Cannot divide by zero");
          }
        result = left / right;
                }

                // replace the three pieces (left, symbol, right) with just the result
                pieces.set(i - 1, String.valueOf(result));
                pieces.remove(i + 1);
                pieces.remove(i);

            } else {
                i++;
            }
        }

        return solveAddSubtract(pieces);
    }

    // Solve + and - last, going left to right
    static double solveAddSubtract(ArrayList<String> pieces) {

        double answer = Double.parseDouble(pieces.get(0));
        int i = 1;
        while (i < pieces.size()) {

            String operator = pieces.get(i);
            double number = Double.parseDouble(pieces.get(i + 1));

            if (operator.equals("+")) {
                answer = answer + number;
            } else {
                answer = answer - number;
            }
            i = i + 2;
        } return answer;
    }

    // Show calculation history
    public static void showHistory(ArrayList<String> history) {

        System.out.println();

        if (history.size() == 0) {
            System.out.println("No history.");
        } else {
            System.out.println("History:");

            for (String item : history) {
                System.out.println(item);
            }
        }
        System.out.println();
    }
}
