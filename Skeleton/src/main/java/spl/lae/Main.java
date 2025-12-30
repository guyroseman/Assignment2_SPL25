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

            System.out.println("Starting execution with " + numThreads + " threads...");
            long startTime = System.currentTimeMillis();
            
            // Parse input JSON into a computation graph
            ComputationNode rootNode = parser.parse(inputPath);
            
            //Adding associative nesting optimization
            recursiveAssociativeNesting(rootNode);

            // Initialize the Linear Algebra Engine with the specified number of threads
            engine = new LinearAlgebraEngine(numThreads);

            // Run the engine to process the rootNode
            ComputationNode resultNode = engine.run(rootNode);

            // Write the resulting matrix to the output JSON
            OutputWriter.write(resultNode.getMatrix(), outputPath);

            // Performance Reporting
            long endTime = System.currentTimeMillis();
            System.out.println("Computation finished successfully in " + (endTime - startTime) + "ms.");
            
            // Print the internal worker report
            System.out.println(engine.getWorkerReport());
            // Calculate and print the fairness score
            System.out.println("Fairness Score: " + get_fairness_score(engine)+"\n");
            

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
            try {
                if (args.length >= 3) {
                    OutputWriter.write(e.getMessage(), args[2]);
                }
            } catch (IOException ioException) {
                System.err.println("Failed to write error to output file.");
                throw new IOException("Fatal: Could not write error to output file", ioException);            }
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


    private static double get_fairness_score(LinearAlgebraEngine engine) {
        // 1. Get the raw report string
        String report = engine.getWorkerReport();
        if (report == null || report.isEmpty()) {
            return 0.0;
        }

        java.util.List<Double> fatigueValues = new java.util.ArrayList<>();
        
        // 2. Parse the string line by line
        // Format: "Worker 0: Fatigue=123.45, TimeUsed=..."
        String[] lines = report.split("\n");
        for (String line : lines) {
            if (line.contains("Fatigue=")) {
                try {
                    // Extract value between "Fatigue=" and ","
                    int startIndex = line.indexOf("Fatigue=") + 8; // length of "Fatigue=" is 8
                    int endIndex = line.indexOf(",", startIndex);
                    
                    if (startIndex > 7 && endIndex > startIndex) {
                        String valStr = line.substring(startIndex, endIndex);
                        fatigueValues.add(Double.parseDouble(valStr));
                    }
                } catch (Exception e) {
                    // Ignore parsing errors for header lines or malformed lines
                }
            }
        }

        if (fatigueValues.isEmpty()) {
            return 0.0;
        }

        // 3. Calculate Average (Mean)
        double sum = 0.0;
        for (double f : fatigueValues) {
            sum += f;
        }
        double mean = sum / fatigueValues.size();

        // 4. Calculate Sum of Squared Deviations
        double sumSquaredDeviations = 0.0;
        for (double f : fatigueValues) {
            double deviation = f - mean;
            sumSquaredDeviations += (deviation * deviation);
        }

        return sumSquaredDeviations;
    }
}