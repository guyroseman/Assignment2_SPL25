package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class LinearAlgebraEngineTest {

    @TempDir
    Path tempDir;

    private Path inputPath;
    private Path outputPath;
    private final ObjectMapper mapper = new ObjectMapper();

    // Stream Capturing (To keep the console clean like the reference)
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        inputPath = tempDir.resolve("input.json");
        outputPath = tempDir.resolve("output.json");

        // Silence standard output to match the clean style of SharedVectorTest
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
    }

    @AfterEach
    void tearDown() {
        // Restore streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private void writeInput(String content) throws IOException {
        Files.writeString(inputPath, content);
    }

    private JsonNode readOutput() throws IOException {
        assertTrue(Files.exists(outputPath), "Output file was not created");
        return mapper.readTree(outputPath.toFile());
    }

    // =================================================================
    // 1. BASIC LOGIC & NESTING TESTS
    // =================================================================

    /**
     * Test that associative nesting is handled correctly (A + B + C).
     */
    @Test
    void testAssociativeNesting() throws IOException {
        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [" +
                "  [[1]], [[2]], [[3]]" +
                "]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"2", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertNotNull(res, "Result should exist");
        assertEquals(6.0, res.get(0).get(0).asDouble());
    }

    /**
     * Test that deeply nested matrix operations are resolved correctly.
     */
    @Test
    void testFourNestedMatrices() throws IOException {
        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [" +
                "  {" +
                "     \"operator\": \"+\"," +
                "     \"operands\": [" +
                "        {" +
                "           \"operator\": \"+\"," +
                "           \"operands\": [ [[1]], [[1]] ]" +
                "        }," +
                "        [[1]]" +
                "     ]" +
                "  }," +
                "  [[1]]" +
                "]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"4", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertEquals(4.0, res.get(0).get(0).asDouble());
    }

    /**
     * Test that mixed nesting structures are calculated correctly.
     */
    @Test
    void testTwiceNesting() throws IOException {
        String json = "{" +
                "\"operator\": \"*\"," +
                "\"operands\": [" +
                "  {" + 
                "     \"operator\": \"+\"," + 
                "     \"operands\": [ [[1]], [[1]], [[1]] ]" + 
                "  }," +
                "  {" + 
                "     \"operator\": \"+\"," + 
                "     \"operands\": [ [[1]], [[1]], [[1]] ]" + 
                "  }" +
                "]" +
                "}";
        
        writeInput(json);
        
        Main.main(new String[]{"4", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertEquals(9.0, res.get(0).get(0).asDouble());
    }

    // =================================================================
    // 2. COMPLEX OPERATIONS TESTS
    // =================================================================

    /**
     * Test a complex scenario involving Add, Multiply, Negate, and Transpose.
     */
    @Test
    void testAllOperationsComplex() throws IOException {
        String json = "{" +
                "\"operator\": \"*\"," + 
                "\"operands\": [" +
                "  {" +
                "    \"operator\": \"+\"," + 
                "    \"operands\": [" +
                "      { \"operator\": \"T\", \"operands\": [ [[1, 2]] ] }," + 
                "      [[3], [4]]" + 
                "    ]" +
                "  }," +
                "  { \"operator\": \"-\", \"operands\": [ [[1]] ] }" + 
                "]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"4", inputPath.toString(), outputPath.toString()});

        JsonNode result = readOutput().get("result");
        assertNotNull(result);
        assertEquals(-4.0, result.get(0).get(0).asDouble());
        assertEquals(-6.0, result.get(1).get(0).asDouble());
    }

    // =================================================================
    // 3. ERROR HANDLING & BOUNDARY TESTS
    // =================================================================

    /**
     * Test that invalid matrix dimensions result in an error in the output file.
     */
    @Test
    void testInvalidDimensions() throws IOException {
        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [ [[1]], [[1, 2]] ]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"2", inputPath.toString(), outputPath.toString()});

        JsonNode root = readOutput();
        assertTrue(root.has("error"), "Output should contain error");
        assertTrue(root.get("error").asText().contains("dimensions"), "Error should mention dimensions");
    }

    /**
     * Test handling of null values within a matrix.
     */
    @Test
    void testMatrixWithNullValues() throws IOException {
        String json = "[[1, null]]";
        String wrappedJson = "{ \"operator\": \"-\", \"operands\": [ [[1, null]] ] }";
        
        writeInput(wrappedJson);

        Main.main(new String[]{"2", inputPath.toString(), outputPath.toString()});

        JsonNode root = readOutput();
        assertTrue(root.has("result") || root.has("error"));
    }

    /**
     * Test that Main throws IOException when file paths are incorrect or unwritable.
     */
    @Test
    void testIncorrectFilePaths() throws IOException {
        // Part 1: Missing input file
        Path missingInput = tempDir.resolve("ghost.json");
        Main.main(new String[]{"2", missingInput.toString(), outputPath.toString()});
        
        JsonNode root = readOutput();
        assertTrue(root.has("error"), "Should report error for missing input");

        // Part 2: Invalid output path (throws exception)
        Files.deleteIfExists(outputPath);
        Path invalidOutput = tempDir; // Directory cannot be written as file
        
        writeInput("{\"operator\": \"T\", \"operands\": [[[1]]]}"); 

        assertThrows(IOException.class, () -> {
            Main.main(new String[]{"2", inputPath.toString(), invalidOutput.toString()});
        });
    }

    // =================================================================
    // 4. CONCURRENCY & STRESS TESTS
    // =================================================================

    /**
     * Test system stability with a high thread count relative to tasks.
     */
    @Test
    void testHighThreadCount() throws IOException {
        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [ [[1,1],[1,1]], [[2,2],[2,2]] ]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"100", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertEquals(3.0, res.get(0).get(0).asDouble());
    }

    /**
     * Test system performance and stability with huge input matrices.
     */
    @Test
    void testHugeInput() throws IOException {
        int size = 500;
        StringBuilder matrix = new StringBuilder("[");
        for(int i=0; i<size; i++) {
            matrix.append("[");
            for(int j=0; j<size; j++) {
                matrix.append("1").append(j < size-1 ? "," : "");
            }
            matrix.append("]").append(i < size-1 ? "," : "");
        }
        matrix.append("]");

        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [" + matrix + "," + matrix + "]" +
                "}";
        
        writeInput(json);

        Main.main(new String[]{"8", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertEquals(2.0, res.get(0).get(0).asDouble());
    }

    /**
     * Test that the main thread handles interruption correctly.
     */
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testInterruption() throws InterruptedException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"operator\": \"+\", \"operands\": [");
        for(int i=0; i<1000; i++) {
            sb.append("[[1]],");
        }
        sb.append("[[1]] ] }");
        writeInput(sb.toString());

        Thread mainThread = new Thread(() -> {
            try {
                Main.main(new String[]{"2", inputPath.toString(), outputPath.toString()});
            } catch (IOException e) {
                // Ignore IO errors
            }
        });

        mainThread.start();
        Thread.sleep(50);
        mainThread.interrupt(); 
        mainThread.join(); 

        assertFalse(mainThread.isAlive(), "Main thread should have exited after interruption");
    }
}