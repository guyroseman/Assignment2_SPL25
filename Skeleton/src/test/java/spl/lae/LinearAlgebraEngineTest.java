package spl.lae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
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

    @BeforeEach
    void setUp() {
        inputPath = tempDir.resolve("input.json");
        outputPath = tempDir.resolve("output.json");
    }

    private void writeInput(String content) throws IOException {
        Files.writeString(inputPath, content);
    }

    private JsonNode readOutput() throws IOException {
        assertTrue(Files.exists(outputPath), "Output file was not created");
        return mapper.readTree(outputPath.toFile());
    }

    /**
     * Input with Associative Nesting
     * A + B + C -> Should be treated as (A + B) + C
     */
    @Test
    void testAssociativeNesting() throws IOException {
        // [1] + [2] + [3] = [6]
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
     * Input with 4 Matrices Nested
     * Op(Op(Op(A, B), C), D)
     */
    @Test
    void testFourNestedMatrices() throws IOException {
        // (((1+1) + 1) + 1) = 4
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
     * A Lot of Threads (Stress Test)
     * Running with more threads than tasks/rows to ensure no concurrency crashes.
     */
    @Test
    void testHighThreadCount() throws IOException {
        String json = "{" +
                "\"operator\": \"+\"," +
                "\"operands\": [ [[1,1],[1,1]], [[2,2],[2,2]] ]" +
                "}";
        writeInput(json);

        // Run with 100 threads
        Main.main(new String[]{"100", inputPath.toString(), outputPath.toString()});

        JsonNode res = readOutput().get("result");
        assertEquals(3.0, res.get(0).get(0).asDouble());
    }

    //Interrupts in the middle of the code
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS) // Fail if it deadlocks
    void testInterruption() throws InterruptedException, IOException {
        // Create a large computation to ensure it runs long enough to interrupt
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
                // Ignore IO errors during interrupt test
            }
        });

        mainThread.start();
        Thread.sleep(50); // Let it start
        mainThread.interrupt(); // interrupt
        mainThread.join(); // Wait for it to die

        //verify the thread is actually dead
        assertFalse(mainThread.isAlive(), "Main thread should have exited after interruption");
    }

    //Check for right result with all actions (ADD, MULTIPLY, NEGATE, TRANSPOSE)
    @Test
    void testAllOperationsComplex() throws IOException {
        String json = "{" +
                "\"operator\": \"*\"," + // Multiply
                "\"operands\": [" +
                "  {" +
                "    \"operator\": \"+\"," + // Add
                "    \"operands\": [" +
                "      { \"operator\": \"T\", \"operands\": [ [[1, 2]] ] }," + // Transpose
                "      [[3], [4]]" + // Column vector
                "    ]" +
                "  }," +
                "  { \"operator\": \"-\", \"operands\": [ [[1]] ] }" + // Negate
                "]" +
                "}";
        writeInput(json);

        Main.main(new String[]{"4", inputPath.toString(), outputPath.toString()});

        JsonNode result = readOutput().get("result");
        assertNotNull(result);
        assertEquals(-4.0, result.get(0).get(0).asDouble());
        assertEquals(-6.0, result.get(1).get(0).asDouble());
    }

    /**
     * Invalid Matrix Dimensions
     * Adding [1] (1x1) to [1, 2] (1x2)
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
     * Matrix with Null Values
     * JSON containing null inside a matrix row.
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

    //Input with Twice Nesting (A + B + C) * (D + E + F)
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

    /**
     * Huge Input
     * Generate a large matrix addition (500x500)
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

        long start = System.currentTimeMillis();
        Main.main(new String[]{"8", inputPath.toString(), outputPath.toString()});
        long end = System.currentTimeMillis();

        JsonNode res = readOutput().get("result");
        assertEquals(2.0, res.get(0).get(0).asDouble());
        System.out.println("Huge input took: " + (end-start) + "ms");
    }

    @Test
    void testIncorrectFilePaths() throws IOException {
        // Case A: Missing Input -> Main should catch the error and write it to the output file.
        Path missingInput = tempDir.resolve("ghost.json");
        Main.main(new String[]{"2", missingInput.toString(), outputPath.toString()});
        
        // Verify the output file contains the error message
        JsonNode root = readOutput();
        assertTrue(root.has("error"), "Should report error for missing input");

        // Case B: Invalid Output -> Main tries to write result/error, fails, and throws exception.
        Files.deleteIfExists(outputPath);
        Path invalidOutput = tempDir;
        
        // Write a dummy input file so the parser passes or fails (doesn't matter), 
        // eventually triggering a write attempt.
        writeInput("{\"operator\": \"T\", \"operands\": [[[1]]]}"); 

        assertThrows(IOException.class, () -> {
            Main.main(new String[]{"2", inputPath.toString(), invalidOutput.toString()});
        }, "Should throw IOException when output path is unwritable");
    }
}