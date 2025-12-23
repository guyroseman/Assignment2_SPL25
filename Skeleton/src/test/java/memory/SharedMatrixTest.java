package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class SharedMatrixTest {

    //----------------------------------------------------------------------
    // Constructor Tests
    //----------------------------------------------------------------------

    /**
     * Test that the constructor throws an exception when given a null input.
     */
    @Test
    @DisplayName("Constructor: Reject Null Input")
    void testConstructor_NullInput() {
        // Even if the Parser fails, the Constructor must reject null
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null),
            "Matrix must reject null double[][] input.");
    }

    /**
     * Test that the constructor create scalar (1x1) matrices correctly.
     */
    @Test
    void testConstructor_Scalar() {
        double[][] scalarData = {{42.0}};
        SharedMatrix m = new SharedMatrix(scalarData);

        assertEquals(1, m.length(), "Scalar matrix should have length 1.");
        assertNotNull(m.get(0), "Row 0 should not be null.");
        assertEquals(42.0, m.get(0).get(0), "Value should match input.");
    }

    /**
     * Test constructor deep copy behavior.
     */
    @Test
    void testConstructor_DeepCopyIdentity() {
        double[] row0 = {1.0, 2.0};
        double[][] data = {row0};
        
        SharedMatrix m = new SharedMatrix(data);
        
        // Modify the original array
        row0[0] = 99.0;

        // Maniac check: If your constructor doesn't clone, the matrix will change!
        // Note: Your SharedVector constructor usually clones the array internally.
        assertNotEquals(99.0, m.get(0).get(0), 
            "The matrix should be isolated from changes to the original input array.");
    }

    /**
     * Test constructor with a very large number of rows.
     */
    void testLargeRowCount() {
        // 5,000 rows, each with its own SharedVector and ReadWriteLock
        int rows = 5000;
        int cols = 10;
        double[][] largeData = new double[rows][cols];
        
        // Fill with some data
        for (int i = 0; i < rows; i++) {
            largeData[i][0] = i;
        }

        // The constructor must iterate 5,000 times and allocate 5,000 locks
        SharedMatrix matrix = new SharedMatrix(largeData);

        assertEquals(rows, matrix.length(), "Matrix should successfully allocate all rows.");
        assertEquals(4999.0, matrix.get(4999).get(0), "Data in the last row should be intact.");
    }

    // ----------------------------------------------------------------------
    // loadRowMajor Tests
    // ----------------------------------------------------------------------

    /**
     * Test loadRowMajor with null input.
     */
    @Test
    void testLoadNull() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 1}});
        assertThrows(IllegalArgumentException.class, () -> matrix.loadRowMajor(null),
            "Method must reject null input to protect internal storage.");
    }

    /**
     * Test loadRowMajor with structural changes.
     */
    @Test
    void testStructuralUpdate() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 1}, {2, 2}});
        double[][] newData = {{10, 20, 30}};
        
        matrix.loadRowMajor(newData);

        // Verify the internal vector array was swapped and resized
        assertEquals(1, matrix.length(), "Matrix should now have 1 row.");
        assertEquals(3, matrix.get(0).length(), "The new row should have 3 columns.");
        assertEquals(10.0, matrix.get(0).get(0));
    }

    /**
     * Test loadRowMajor with empty input.
     */
    @Test
    void testLoadEmpty() {
        double[][] initial = {{1, 1}};
        SharedMatrix matrix = new SharedMatrix(initial);
        
        // According to your code, empty matrix input results in a 'return' (no change)
        matrix.loadRowMajor(new double[0][0]);
        
        assertEquals(1, matrix.length(), "Matrix should remain unchanged if input is empty.");
        assertEquals(1.0, matrix.get(0).get(0));
    }

    /**
     * Test loadRowMajor deep copy behavior.
     */
    @Test
    void testDataIsolation() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 1}});
        double[][] source = {{5, 5}};
        
        matrix.loadRowMajor(source);
        source[0][0] = -99.0; // Modify the array outside the class

        // Internal SharedVector must remain 5.0
        assertNotEquals(-99.0, matrix.get(0).get(0), 
            "Matrix must clone data so external changes don't affect it.");
    }

    /**
     * Stress test: Repeatedly load matrices of increasing size.
     */
    @Test
    @DisplayName("Stress: Repeated Structure Swapping")
    void testRepeatedLoadStress() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{0}});
        
        // Test 1000 consecutive swaps with different sizes
        for (int i = 1; i <= 1000; i++) {
            double[][] data = new double[1][i]; // Increasing column width
            matrix.loadRowMajor(data);
            assertEquals(i, matrix.get(0).length());
        }
    }

    /**
     * Test loadRowMajor waits for locks to release.
     */
    @Test
    void testLoadWaitsForLocks() throws InterruptedException {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 1}});
        SharedVector row0 = matrix.get(0);

        // Simulate another thread working on row0 by holding its write lock
        row0.writeLock();

        Thread loaderThread = new Thread(() -> {
            // This should hang until row0.writeUnlock() is called
            matrix.loadRowMajor(new double[][]{{9, 9}, {9, 9}});
        });

        loaderThread.start();
        Thread.sleep(100); // Give thread time to attempt lock

        // Matrix shouldn't have changed yet
        assertEquals(1, matrix.length(), "LoadRowMajor should be blocked waiting for row0 lock.");

        row0.writeUnlock(); // Release the row
        loaderThread.join(2000); // Now it should finish

        assertEquals(2, matrix.length(), "Matrix should update only after locks are released.");
    }

    // ----------------------------------------------------------------------
    // loadColumnMajor Tests
    // ----------------------------------------------------------------------

    /**
     * Test loadColumnMajor with null input.
     */
    @Test
    void testLoadColNull() {
        SharedMatrix matrix = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> matrix.loadColumnMajor(null));
    }

    /**
     * Test loadColumnMajor with structural changes.
     */
    @Test
    @DisplayName("Logic: Column-Major Transposition (2x3 -> 3 Vectors)")
    void testTransposeLogic() {
        // Input: 2 rows, 3 columns
        double[][] data = {
            {10, 20, 30},
            {40, 50, 60}
        };
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        // 1. Check storage length (should match number of columns)
        assertEquals(3, matrix.length(), "Storage should have 3 vectors (one per column).");

        // 2. Check Orientation
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());

        // 3. Check data mapping: The first vector should be the first COLUMN [10, 40]
        SharedVector firstCol = matrix.get(0);
        assertEquals(10.0, firstCol.get(0));
        assertEquals(40.0, firstCol.get(1));
        
        // Check the last vector: should be [30, 60]
        SharedVector lastCol = matrix.get(2);
        assertEquals(30.0, lastCol.get(0));
        assertEquals(60.0, lastCol.get(1));
    }

    /**
     * Test loadColumnMajor with scalar (1x1) input.
     */
    @Test
    void testScalarColumnLoad() {
        double[][] data = {{5.0}};
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        assertEquals(1, matrix.length());
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        assertEquals(5.0, matrix.get(0).get(0));
    }

    /**
     * Test loadColumnMajor data isolation (deep copy).
     */
    @Test
    void testIsolation() {
        double[][] data = {{1.0, 2.0}};
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data);

        // Modify original source
        data[0][1] = 99.0;

        // Matrix should be unaffected because it created new SharedVectors
        assertEquals(2.0, matrix.get(1).get(0), "Matrix should hold a copy of the column data.");
    }

    /**
     * Test loadColumnMajor Stress test: Load a very high-dimensional matrix.
     */
    @Test
    void testStressTranspose() {
        // 100 rows, 10 columns
        int rows = 100;
        int cols = 10;
        double[][] largeData = new double[rows][cols];
        
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(largeData);

        assertEquals(cols, matrix.length(), "Should create a vector for every column.");
        assertEquals(rows, matrix.get(0).length(), "Each column vector should match row height.");
    }

    /**
     * Test loadColumnMajor waits for locks to release.
     */
    @Test
    void testConcurrencyBlocking() throws InterruptedException {
        // Start with a Row-Major matrix
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 1}});
        SharedVector row0 = matrix.get(0);

        // Lock the row to prevent the matrix from being swapped
        row0.writeLock();

        Thread loader = new Thread(() -> {
            // This will wait in acquireAllVectorWriteLocks(oldVectors)
            matrix.loadColumnMajor(new double[][]{{10}, {20}});
        });

        loader.start();
        Thread.sleep(100);

        // Orientation hasn't changed yet because loader is blocked
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        row0.writeUnlock();
        loader.join(2000);

        // Now it should be updated
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
        assertEquals(10.0, matrix.get(0).get(0));
    }

    //----------------------------------------------------------------------
    // readRowMajor Tests
    //----------------------------------------------------------------------
    
    /**
     * Test readRowMajor from Row-Major storage.
     */
    @Test
    void testReadFromRowStorage() {
        double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
        SharedMatrix matrix = new SharedMatrix(data);

        // Logic: Should return exactly what was put in
        double[][] result = matrix.readRowMajor();
        
        assertArrayEquals(data[0], result[0]);
        assertArrayEquals(data[1], result[1]);
    }

    /**
     * Test readRowMajor from Column-Major storage.
     */
    @Test
    void testReadFromColStorage() {
        // Logical Shape: 2 Rows, 3 Columns
        double[][] data = {
            {10, 20, 30},
            {40, 50, 60}
        };
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data); // Internal storage is now 3 vectors of length 2

        // Logic: readRowMajor must flip 3x2 storage back into a 2x3 array
        double[][] result = matrix.readRowMajor();

        assertEquals(2, result.length, "Result must have 2 rows");
        assertEquals(3, result[0].length, "Result must have 3 columns");
        assertEquals(20.0, result[0][1]); // Check middle of first row
        assertEquals(40.0, result[1][0]); // Check start of second row
    }

    /**
     * Test readRowMajor from an empty matrix.
     */
    @Test
    void testReadEmpty() {
        // Logic: tempVectors.length == 0 check
        SharedMatrix matrix = new SharedMatrix(new double[0][0]);
        double[][] result = matrix.readRowMajor();
        assertEquals(0, result.length);
    }

    /**
     * Test readRowMajor snapshot safety under concurrent modification.
     */
    @Test
    void testReadSnapshot() throws InterruptedException {
        // Thread A starts reading 'oldData'. 
        // Thread B swaps to 'newData'.
        // Thread A should finish reading 'oldData' without crashing.
        double[][] oldData = {{1, 1}};
        SharedMatrix matrix = new SharedMatrix(oldData);

        // Manually hold a lock on the only row to 'pause' the matrix
        matrix.get(0).readLock();

        Thread writer = new Thread(() -> {
            // This will block until we release the lock below
            matrix.loadRowMajor(new double[][]{{9, 9}, {9, 9}});
        });

        writer.start();
        
        // Logic: Even while writer is waiting, readRowMajor should see the OLD array
        double[][] result = matrix.readRowMajor();
        assertEquals(1, result.length);
        assertEquals(1.0, result[0][0]);

        matrix.get(0).readUnlock(); // Let writer finish
        writer.join();
    }

    // ----------------------------------------------------------------------
    // readRowMajor() Tests
    // ----------------------------------------------------------------------

    /**
     * Test readRowMajor standard case.
     */
    @Test
    void testReadRowMajor_Standard() {
        double[][] input = {{1.0, 2.0}, {3.0, 4.0}};
        SharedMatrix matrix = new SharedMatrix(input);
        
        double[][] result = matrix.readRowMajor();
        
        assertEquals(2, result.length);
        assertArrayEquals(input[0], result[0]);
        assertArrayEquals(input[1], result[1]);
    }

    /**
     * Test readRowMajor from Column-Major storage.
     */
    @Test
    void testReadRowMajor_FromColumnStorage() {
        // Logic: Input a 2x3 matrix. If stored as Columns, readRowMajor must flip it back.
        double[][] data = {{1, 2, 3}, {4, 5, 6}};
        SharedMatrix matrix = new SharedMatrix();
        matrix.loadColumnMajor(data); // Internal: 3 SharedVectors of length 2

        double[][] result = matrix.readRowMajor();

        assertEquals(2, result.length, "Should return 2 rows.");
        assertEquals(3, result[0].length, "Should return 3 columns.");
        assertEquals(2.0, result[0][1], "Value at [0][1] should be 2.0");
        assertEquals(4.0, result[1][0], "Value at [1][0] should be 4.0");
    }

    /**
     * Test readRowMajor from an empty matrix.
     */
    @Test
    void testReadRowMajor_Empty() {
        SharedMatrix matrix = new SharedMatrix(new double[0][0]);
        double[][] result = matrix.readRowMajor();
        assertEquals(0, result.length);
    }

    // ----------------------------------------------------------------------
    // get() Tests
    // ----------------------------------------------------------------------

    /**
     * Test get() with valid index.
     */
    @Test
    void testGet_ValidIndex() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{10.5}, {20.5}});
        SharedVector v = matrix.get(1);
        assertNotNull(v);
        assertEquals(20.5, v.get(0));
    }

    /**
     * Test get() with out-of-bounds index.
     */
    @Test
    void testGet_OutOfBounds() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0}});
        assertThrows(IllegalArgumentException.class, () -> matrix.get(-1));
        assertThrows(IllegalArgumentException.class, () -> matrix.get(1));
    }

    // ----------------------------------------------------------------------
    // length() Tests
    // ----------------------------------------------------------------------

    /**
     * Test length() method.
     */
    @Test
    void testLength() {
        SharedMatrix matrix = new SharedMatrix(new double[5][2]);
        assertEquals(5, matrix.length());
        
        matrix.loadRowMajor(new double[2][10]);
        assertEquals(2, matrix.length());
    }

    // ----------------------------------------------------------------------
    // getOrientation() Tests
    // ----------------------------------------------------------------------

    /**
     * Test getOrientation() for both Row-Major and Column-Major storage.
     */
    @Test
    void testGetOrientation() {
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1, 2}});
        assertEquals(VectorOrientation.ROW_MAJOR, matrix.getOrientation());

        matrix.loadColumnMajor(new double[][]{{1, 2}});
        assertEquals(VectorOrientation.COLUMN_MAJOR, matrix.getOrientation());
    }

    /**
     * Test getOrientation() on an empty matrix.
     */
    @Test
    void testGetOrientation_Empty() {
        SharedMatrix matrix = new SharedMatrix(new double[0][0]);
        assertNull(matrix.getOrientation(), "Orientation of 0 vectors should be null.");
    }

    // ----------------------------------------------------------------------
    // read/write lock Tests
    // ----------------------------------------------------------------------

    /**
     * Test locking behavior during concurrent loadRowMajor calls.
     */
    @Test
    void testLocking_SnapshotSafety() throws InterruptedException {
        // This tests the logic of 'SharedVector[] tempVectors = this.vectors'
        // combined with the locking helpers.
        SharedMatrix matrix = new SharedMatrix(new double[][]{{1.0}});
        SharedVector originalVector = matrix.get(0);

        // Hold a read lock manually to prevent any writer from finishing a loadRowMajor
        originalVector.readLock();

        Thread readerThread = new Thread(() -> {
            // Should read old value 1.0 even if writer is trying to swap
            double[][] result = matrix.readRowMajor();
            assertEquals(1.0, result[0][0]);
        });

        Thread writerThread = new Thread(() -> {
            // Attempts to swap 'this.vectors' to new array, but blocks on row lock
            matrix.loadRowMajor(new double[][]{{99.0}});
        });

        writerThread.start();
        readerThread.start();
        Thread.sleep(100);

        originalVector.readUnlock(); // Gate opens
        readerThread.join();
        writerThread.join();

        assertEquals(99.0, matrix.get(0).get(0));
    }

    /**
     * Test lock helper methods with null input.
     */
    @Test
    void testLockHelpers_NullSafety() {
        // These methods should simply return if passed a null array
        // (Verifying the private helper logic used in readRowMajor)
        assertDoesNotThrow(() -> {
            // We can't call private directly, but we know readRowMajor calls them.
            // If the code is correct, an empty matrix won't crash the lock calls.
            SharedMatrix m = new SharedMatrix(new double[0][0]);
            m.readRowMajor(); 
        });
    }
}
