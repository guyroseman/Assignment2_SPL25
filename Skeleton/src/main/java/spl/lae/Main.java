package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
      // TODO: main
     if (args.length < 3) {
            System.err.println("Error: Missing arguments.");
            System.err.println("Usage: java -jar <jar_name> <num_threads> <input_path> <output_path>");
            return;
        }

        // אתחול משתנים מחוץ ל-try כדי שיהיו זמינים ב-finally
        LinearAlgebraEngine engine = null;

        try {
            int numThreads = Integer.parseInt(args[0]);
            String inputPath = args[1];
            String outputPath = args[2];

            InputParser parser = new InputParser();
            engine = new LinearAlgebraEngine(numThreads);

            System.out.println("Starting execution with " + numThreads + " threads...");
            long startTime = System.currentTimeMillis();

            ComputationNode rootNode = parser.parse(inputPath);
            ComputationNode resultNode = engine.run(rootNode);

            OutputWriter.write(resultNode.getMatrix(), outputPath);

            long endTime = System.currentTimeMillis();
            System.out.println("Computation finished successfully in " + (endTime - startTime) + "ms.");
            
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
        } finally {
          /* 
            // ביצוע סגירה מסודרת (Graceful Shutdown) במקום System.exit(0)
            if (engine != null) {
                try {
                    engine.shutdown();
                } catch (InterruptedException e) {
                    System.err.println("Shutdown interrupted: " + e.getMessage());
                }
            }
          */
        }
       
    }
}