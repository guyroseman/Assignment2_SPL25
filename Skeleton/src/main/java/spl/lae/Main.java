package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      // Validate Command Line Arguments
     if (args.length < 3) {
            System.err.println("Error: Missing arguments.");
            System.err.println("Usage: java -jar <jar_name> <num_threads> <input_path> <output_path>");
            return;
        }
        // init an empty LAE engine
        LinearAlgebraEngine engine = null;

        try {
            // Parse Arguments
            int numThreads = Integer.parseInt(args[0]);
            String inputPath = args[1];
            String outputPath = args[2];

            // Initialize Components
            InputParser parser = new InputParser();
            engine = new LinearAlgebraEngine(numThreads);

            System.out.println("Starting execution with " + numThreads + " threads...");
            long startTime = System.currentTimeMillis();
            
            // Parse input JSON into a computation graph
            ComputationNode rootNode = parser.parse(inputPath);
            
            //Adding associative nesting optimization
            recursiveAssociativeNesting(rootNode);

            // Run the engine to process the rootNode
            ComputationNode resultNode = engine.run(rootNode);

            // Write the resulting matrix to the output JSON
            OutputWriter.write(resultNode.getMatrix(), outputPath);

            // Performance Reporting
            long endTime = System.currentTimeMillis();
            System.out.println("Computation finished successfully in " + (endTime - startTime) + "ms.");
            
            // Print the internal worker report
            System.out.println(engine.getWorkerReport());

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            try {
                if (args.length >= 3) {
                    OutputWriter.write(e.getMessage(), args[2]);
                }
            } catch (IOException ioException) {
                System.err.println("Failed to write error to output file.");
            }
        }
    }

    /**
     * Recursive helper function to associativeNesting.
     * Traverses the tree bottom-up (Post-Order) and applies associativeNesting to every node.
     * This ensures that nested operations (like A+B+C) are correctly structured before execution.
     */
    private static void recursiveAssociativeNesting(ComputationNode node) {
        //check for null node
        if (node == null) {
            return;
        }

        // fix the children first (Recursion)
        if (node.getChildren() != null) {
            for (ComputationNode child : node.getChildren()) {
                recursiveAssociativeNesting(child);
            }
        }

        // Fix the current node (after children are already arranged)
        node.associativeNesting();
    }
}