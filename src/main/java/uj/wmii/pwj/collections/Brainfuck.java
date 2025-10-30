package uj.wmii.pwj.collections;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

public interface Brainfuck {
    /**
     * Executes uploaded program.
     */
    void execute();

    /**
     * Creates a new instance of Brainfuck interpreter with given program, using standard IO and stack of 1024 size.
     * @param program brainfuck program to interpret
     * @return new instance of the interpreter
     * @throws IllegalArgumentException if program is null or empty
     */
    static Brainfuck createInstance(String program) {
        return createInstance(program, System.out, System.in, 1024);
    }

    /**
     * Creates a new instance of Brainfuck interpreter with given parameters.
     * @param program brainfuck program to interpret
     * @param out output stream to be used by interpreter implementation
     * @param in input stream to be used by interpreter implementation
     * @param stackSize maximum stack size, that is allowed for this interpreter
     * @return new instance of the interpreter
     * @throws IllegalArgumentException if: program is null or empty, OR out is null, OR in is null, OR stackSize is below 1.
     */
    static Brainfuck createInstance(String program, PrintStream out, InputStream in, int stackSize) throws IllegalArgumentException {
        if (program == null || program.isEmpty()) {
            throw new IllegalArgumentException("Program can't be null nor empty.");
        }
        if (in == null) {
            throw new IllegalArgumentException("Input stream can't be null.");
        }
        if (out == null) {
            throw new IllegalArgumentException("Output stream can't be null.");
        }
        if(stackSize < 1) {
            throw new IllegalArgumentException("Stack size must be positive.");
        }
        return new BrainfuckInterpreter(program, out, in, stackSize);
    }

    public class BrainfuckInterpreter implements Brainfuck {
        String program;
        PrintStream out;
        InputStream in;
        int stackSize;
        HashMap<Integer, Integer> memo;
        BrainfuckInterpreter(String program, PrintStream out, InputStream in, int stackSize) {
            this.program = program;
            this.out = out;
            this.in = in;
            this.stackSize = stackSize;
            this.memo = new HashMap<>();
        }

        @Override
        public void execute(){
            byte[] stack = new byte[stackSize];
            int dataPointer = 0;
            int instructionPointer = 0;
            char a;
            int length = program.length();
            while(instructionPointer < length) {
                a = program.charAt(instructionPointer);
                switch (a) {
                    case '>':
                        dataPointer++;
                        break;
                    case '<':
                        dataPointer--;
                        break;
                    case '+':
                        stack[dataPointer]++;
                        break;
                    case '-':
                        stack[dataPointer]--;
                        break;
                    case '.':
                        out.print((char) stack[dataPointer]);
                        break;
                    case ',':
                        try {
                            stack[dataPointer] = (byte) in.read();
                        }
                        catch (IOException e) {
                            out.print("");
                        }
                        break;
                    case '[':
                        if (stack[dataPointer] == 0) {
                            instructionPointer = findClosingParenthesis(instructionPointer);
                        }
                        break;
                    case ']':
                        if (stack[dataPointer] != 0) {
                            instructionPointer = findOpeningParenthesis(instructionPointer);
                        }
                        break;
                }
                instructionPointer++;
            }
        }

        int findClosingParenthesis(int instructionPointer) {
            int originalPointer = instructionPointer;
            Integer result = memo.get(originalPointer);
            if (result != null) {
                return result;
            }
            int parenCount = 1;
            while (parenCount > 0) {
                instructionPointer++;
                if (program.charAt(instructionPointer) == '[') {
                    parenCount++;
                }
                else if (program.charAt(instructionPointer) == ']') {
                    parenCount--;
                }
            }
            memo.put(originalPointer, instructionPointer);
            return instructionPointer;
        }

        int findOpeningParenthesis(int instructionPointer) {
            int originalPointer = instructionPointer;
            Integer result = memo.get(originalPointer);
            if (result != null) {
                return result;
            }
            int parenCount = -1;
            while (parenCount < 0) {
                instructionPointer--;
                if (program.charAt(instructionPointer) == '[') {
                    parenCount++;
                }
                else if (program.charAt(instructionPointer) == ']') {
                    parenCount--;
                }
            }
            memo.put(originalPointer, instructionPointer);
            return instructionPointer;
        }
    }

}
