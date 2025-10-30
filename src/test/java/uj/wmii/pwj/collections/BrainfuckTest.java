package uj.wmii.pwj.collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static uj.wmii.pwj.collections.BrainfuckTest.ExceptionExpectancy.EXCEPTION_EXPECTED;
import static uj.wmii.pwj.collections.BrainfuckTest.ExceptionExpectancy.NO_EXCEPTION;
import static uj.wmii.pwj.collections.BrainfuckTest.WhitespacePolicy.DO_NOTHING;
import static uj.wmii.pwj.collections.BrainfuckTest.WhitespacePolicy.TRIM;

public class BrainfuckTest {

    @Nested
    @DisplayName("Initialisation errors")
    class BFErrors {
        @Test
        void emptyProgram() {
            assertThatIllegalArgumentException().isThrownBy(() -> Brainfuck.createInstance(null));
        }

        @Test
        void nullOutput() {
            assertThatIllegalArgumentException().isThrownBy(() -> Brainfuck.createInstance("++", null, System.in, 2));
        }

        @Test
        void nullInput() {
            assertThatIllegalArgumentException().isThrownBy(() -> Brainfuck.createInstance("++", System.out, null, 2));
        }

        @Test
        void invalidStackSize() {
            assertThatIllegalArgumentException().isThrownBy(() -> Brainfuck.createInstance("++", System.out, System.in, -1));
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("brainfuckInput")
    @DisplayName("Programs execution")
    void brainfuckProgram(String testName, String program, String inString, int stackSize, String expectedResult, ExceptionExpectancy exceptionExpectancy) throws IOException {
        var out = expectedResult != null ? new ByteArrayOutputStream(expectedResult.length()) : null;
        var in = inString != null ? new ByteArrayInputStream(inString.getBytes()) : System.in;
        Brainfuck bf = Brainfuck.createInstance(program, out != null ? new PrintStream(out): System.out, in, stackSize);
        try {
            bf.execute();
        } catch (Exception e) {
            if (exceptionExpectancy == EXCEPTION_EXPECTED) {
                System.out.println("Expected exception, all is fine. " + e);
            } else {
                throw e;
            }
        }
        if (out != null) {
            var result = out.toString();
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    private static Stream<Arguments> brainfuckInput() {
        return Stream.of(
                Arguments.of("Add", file("add.bf"), null, 64, file("add.out", TRIM), NO_EXCEPTION),
                Arguments.of("Hello World", file("hello.bf"), null, 32, file("hello.out"), NO_EXCEPTION),
                Arguments.of("ROT 13", file("rot13.bf"), file("rot13.in", TRIM), 32, file("rot13.out", TRIM), NO_EXCEPTION),
                Arguments.of("Fibonacci", file("fib.bf"), null, 32, file("fib.out"), EXCEPTION_EXPECTED)
        );
    }

    private static String file(String name) {
        return file(name, DO_NOTHING);
    }

    private static String file(String name, WhitespacePolicy whitespacePolicy) {
        var input = BrainfuckTest.class.getResourceAsStream("bf/" + name);
        var br = new BufferedReader(new InputStreamReader(input));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(l -> sb.append(l).append("\n"));
        return whitespacePolicy == TRIM ? sb.toString().strip() : sb.toString();
    }

    enum ExceptionExpectancy {
        EXCEPTION_EXPECTED,
        NO_EXCEPTION
    }

    enum WhitespacePolicy {
        DO_NOTHING,
        TRIM
    }
}
