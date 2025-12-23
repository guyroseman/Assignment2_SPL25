package memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class SharedVectorTest {

    // =================================================================
    // 1. CONSTRUCTOR & BASIC STATE TESTS
    // =================================================================

    /**
     * Test that constructor throws IllegalArgumentException when provided a null vector.
     */
    @Test
    void testConstructor_NullVector_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new SharedVector(null, VectorOrientation.ROW_MAJOR);
        });
        assertEquals("vector cant be null", exception.getMessage());
    }

    /**
     * Test that constructor creates an empty SharedVector successfully.
     */
    @Test
    void testConstructor_EmptyArray_CreatedSuccessfully() {
        double[] data = new double[0];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(0, v.length());
    }

    /**
     * Test that constructor creates a scalar SharedVector successfully.
     */
    @Test
    void testConstructor_ScalarVector_CreatedSuccessfully() {
        double[] data = new double[]{1.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(1, v.length());
        assertEquals(1.0, v.get(0));
    }

    /**
     * Test that constructor creates a regular SharedVector successfully.
     */
    @Test
    void testConstructor_RegularVector() {
        double[] data = new double[]{1.0, 2.0, 3.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(3, v.length());
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    // =================================================================
    // 2. GET METHOD TESTS
    // =================================================================

    /**
     * Test that get method throws IllegalArgumentException for negative index.
     */
    @Test
    void testGet_NegativeIndex_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v.get(-1));
    }

    /**
     * Test that get method throws IllegalArgumentException for out-of-bounds index.
     */
    @Test
    void testGet_IndexOutOfBounds_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v.get(2));
        assertEquals("Index out of bounds", e.getMessage());
    }

    /**
     * Test that get method returns correct value for valid index.
     */
    @Test
    void testGet_ValidIndex() {
        SharedVector v = new SharedVector(new double[]{10.5, 20.1}, VectorOrientation.ROW_MAJOR);
        assertEquals(20.1, v.get(1));
    }

    // =================================================================
    // 3. NEGATE TESTS
    // =================================================================

    /**
     * Test that negate method correctly negates regular values.
     */
    @Test
    void testNegate_RegularValues() {
        SharedVector v = new SharedVector(new double[]{5.0, 1.0}, VectorOrientation.ROW_MAJOR);
        v.negate(); 
        assertEquals(-5.0, v.get(0));
        assertEquals(-1.0, v.get(1));
    }

    /**
     * Test that negate method correctly handles zero value.
     */
    @Test
    void testNegate_ZeroHandling() {
        SharedVector v = new SharedVector(new double[]{0.0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(0.0, v.get(0));    
        assertEquals(Double.doubleToLongBits(0.0), Double.doubleToLongBits(v.get(0)));
    }

    /**
     * Test that negate method correctly negates mixed sign values.
     */
    @Test
    void testNegate_MixedSigns() {
        SharedVector v = new SharedVector(new double[]{5.0, -1.0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-5.0, v.get(0));
        assertEquals(1.0, v.get(1));
    }

    // =================================================================
    // 4. DOT PRODUCT TESTS
    // =================================================================

    /**
     * Test that dot method throws NullPointerException when other vector is null.
     */
    @Test
    void testDot_NullOther_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        assertThrows(NullPointerException.class, () -> v1.dot(null));
    }

    /**
     * Test that dot method computes correct result for scalar vectors.
     */
    @Test
    void testDot_Scalaric() {
        SharedVector v1 = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{2}, VectorOrientation.ROW_MAJOR);
        assertEquals(10.0, v1.dot(v2));
    }

    /**
     * Test that dot method computes correct result for standard vectors.
     */
    @Test
    void testDot_Standard() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        assertEquals(11.0, v1.dot(v2));
    }

    /**
     * Test that dot method handles vectors with infinity values.
     */
    @Test
    void testDot_Infinity() {
        SharedVector v1 = new SharedVector(new double[]{Double.POSITIVE_INFINITY, 1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 1}, VectorOrientation.ROW_MAJOR);
        assertEquals(Double.POSITIVE_INFINITY, v1.dot(v2));
    }

    /**
     * Test that dot method throws exception for dimension mismatch when other vector is longer.
     */
    @Test
    void testDot_DimensionMismatch_ThisLonger_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
    }

    /**
     * Test that dot can handle self-referencing vectors correctly.
     */
    @Test
    void testDot_SelfReference() {
        SharedVector v1 = new SharedVector(new double[]{2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(13.0, v1.dot(v1)); 
    }

    // =================================================================
    // 5. VEC-MAT-MUL TESTS
    // =================================================================

    /**
     * Test that vecMatMul method throws IllegalArgumentException when matrix is null.
     */
    @Test
    void testVecMatMul_NullMatrix_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(null));
        assertEquals("matrix cant be null", e.getMessage());
    }

    /**
     * Test that vecMatMul method throws IllegalArgumentException when dimensions do not match.
     */
    @Test
    void testVecMatMul_DimensionMismatch_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR); // Len 3
        
        // Matrix must be loaded properly
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{1, 0}, {0, 1}}); // 2x2 Matrix
        
        // v.len (3) != mat.rows (2)
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    /**
     * Test that vecMatMul method computes correct result for regular vector and matrix multiplication.
     */
    @Test
    void testVecMatMul_RegularRM() {
        // Vector [1, 2] * Identity Matrix [[1, 0], [0, 1]] = [1, 2]
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        // IMPORTANT: Must load as Column Major for vecMatMul logic to work
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{1, 0}, {0, 1}});
        
        v.vecMatMul(m);
        
        assertEquals(2, v.length());
        assertEquals(1.0, v.get(0));
        assertEquals(2.0, v.get(1));
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    /**
     * Test that vecMatMul method computes correct result for scalar vector and matrix multiplication.
     */
    @Test
    void testVecMatMul_Scalaric() {
        // Vector [5] * Matrix [[2, 3]] = Vector [10, 15]
        SharedVector v = new SharedVector(new double[]{5.0}, VectorOrientation.ROW_MAJOR);
        
        // IMPORTANT: Must load as Column Major.
        // Input [[2, 3]] (1 row, 2 cols). 
        // Loaded as Column Major -> creates 2 vectors: [2] and [3].
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{2.0, 3.0}}); 
        
        v.vecMatMul(m);
        
        assertEquals(2, v.length());
        assertEquals(10.0, v.get(0));
        assertEquals(15.0, v.get(1));
    }

    // =================================================================
    // 6. Councurrency TESTS
    // =================================================================

    /**
    * Test that two concurrent get call readers do not block each other.
    */
    @Test
    void testTwoReaders() throws InterruptedException {
        SharedVector v = new SharedVector(new double[]{5.0}, VectorOrientation.ROW_MAJOR);
        
        // Create two threads that just read
        Thread t1 = new Thread(() -> v.get(0));
        Thread t2 = new Thread(() -> v.get(0));

        t1.start();
        t2.start();
        
        t1.join();
        t2.join();
        
        // If we reached here without a timeout, it means they didn't deadlock
        assertEquals(5.0, v.get(0));
    }

    /**
     * Test that a get call reader waits for a writer to finish before proceeding.
     */
    @Test
    void testGetReaderWaitsForWriter() throws InterruptedException {
        SharedVector v = new SharedVector(new double[]{1.0}, VectorOrientation.ROW_MAJOR);
        SharedVector other = new SharedVector(new double[]{2.0}, VectorOrientation.ROW_MAJOR);
        double[] result = new double[1];

        // Thread B: The Writer
        Thread writer = new Thread(() -> {
            v.writeLock(); // Lock the gate
            try {
                Thread.sleep(500); // Hold the lock for half a second
                v.add(other);      // 1.0 + 2.0 = 3.0
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                v.writeUnlock();   // Unlock the gate
            }
        });

        // Thread A: The Reader
        Thread reader = new Thread(() -> {
            try {
                Thread.sleep(100); // Wait a tiny bit to make sure Writer locks first
                result[0] = v.get(0); // This will "pause" here until writer finishes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        writer.start();
        reader.start();

        writer.join();
        reader.join();

        // Result must be 3.0 because the reader was forced to wait for the addition
        assertEquals(3.0, result[0], "Reader should have waited and seen 3.0");
    }

    /**
     * Test that a writer waits for a get call reader to finish before proceeding.
     */
    @Test
    void testWriterWaitsForGetReader() throws InterruptedException {
        SharedVector v = new SharedVector(new double[]{10.0}, VectorOrientation.ROW_MAJOR);
        long startTime = System.currentTimeMillis();

        // Thread A: The Reader
        Thread reader = new Thread(() -> {
            v.readLock(); 
            try {
                Thread.sleep(1000); // Hold the lock for a full second
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                v.readUnlock(); 
            }
        });

        // Thread B: The Writer
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100); // Ensure reader gets in first
                v.writeLock();     // Should block here for ~900ms
                v.negate();
                v.writeUnlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        reader.start();
        writer.start();

        // While reader is sleeping (at 500ms mark), the writer SHOULD be blocked
        // IF THE LOCK WORKS: This readLock will succeed (shared) and value is still 10.0
        v.readLock();
        double midValue = v.get(0);
        v.readUnlock();
        
        assertEquals(10.0, midValue, "Writer should not have negated yet!");

        reader.join();
        writer.join();
        long duration = System.currentTimeMillis() - startTime;

        // IF THE LOCK WORKS: The total time must be at least 1000ms 
        // because the writer had to wait for the reader's full sleep.
        assertTrue(duration >= 1000, "Writer did not wait for the reader!");
        assertEquals(-10.0, v.get(0));
    }

    /**
     * Test that two parallel dot products with shared read locks do not block each other.
     *
     */
    @Test
    void testParallelDotProducts() throws InterruptedException {
        SharedVector v1 = new SharedVector(new double[]{2.0, 2.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3.0, 3.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v3 = new SharedVector(new double[]{4.0, 4.0}, VectorOrientation.ROW_MAJOR);

        v1.readLock(); // Thread A manually holds a read lock on v1
        
        // Thread B should still be able to perform a dot product using v1
        double[] resultB = new double[1];
        Thread threadB = new Thread(() -> {
            resultB[0] = v1.dot(v3); // This requires another readLock on v1
        });

        threadB.start();
        threadB.join(500); // Should finish almost instantly
        
        assertEquals(16.0, resultB[0], "Read-Read should be parallel and not block");
        v1.readUnlock();
    }

    @Test
    void testDotWaitsForAdd() throws InterruptedException {
        SharedVector v1 = new SharedVector(new double[]{1.0, 1.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1.0, 1.0}, VectorOrientation.ROW_MAJOR);
        SharedVector v3 = new SharedVector(new double[]{1.0, 1.0}, VectorOrientation.ROW_MAJOR);
        long startTime = System.currentTimeMillis();

        // Thread B: The Writer (Modifying v2)
        Thread threadB = new Thread(() -> {
            v2.writeLock(); // Point 1: Acquire writeLock
            try {
                Thread.sleep(1000); // Point 3: Simulate heavy addition
                v2.add(v3);         // v2 becomes [2.0, 2.0]
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                v2.writeUnlock();   // Point 4: Release
            }
        });

        // Thread A: The Reader (Dot product)
        double[] resultA = new double[1];
        Thread threadA = new Thread(() -> {
            try {
                Thread.sleep(100); // Ensure Thread B locks first
                // Point 2: v1.dot(v2) stops at D1 (v2.readLock)
                resultA[0] = v1.dot(v2); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        threadB.start();
        threadA.start();

        threadA.join();
        long duration = System.currentTimeMillis() - startTime;

        // The dot product must wait for the full 1000ms addition to finish
        assertTrue(duration >= 1000, "Dot product did not wait for Add to finish");
        assertEquals(4.0, resultA[0], "Dot result should use post-addition values");
    }

}