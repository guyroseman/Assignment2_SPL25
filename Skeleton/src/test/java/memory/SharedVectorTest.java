package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedVectorTest {

    // =================================================================
    // 1. CONSTRUCTOR & BASIC STATE TESTS
    // =================================================================

    @Test
    void testConstructor_NullVector_ThrowsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new SharedVector(null, VectorOrientation.ROW_MAJOR);
        });
        assertEquals("vector cant be null", exception.getMessage());
    }

    @Test
    void testConstructor_EmptyArray_CreatedSuccessfully() {
        double[] data = new double[0];
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(0, v.length());
    }

    @Test
    void testConstructor_ScalarVector_CreatedSuccessfully() {
        double[] data = new double[]{1.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        assertEquals(1, v.length());
        assertEquals(1.0, v.get(0));
    }

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

    @Test
    void testGet_NegativeIndex_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> v.get(-1));
    }

    @Test
    void testGet_IndexOutOfBounds_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v.get(2));
        assertEquals("Index out of bounds", e.getMessage());
    }

    @Test
    void testGet_ValidIndex() {
        SharedVector v = new SharedVector(new double[]{10.5, 20.1}, VectorOrientation.ROW_MAJOR);
        assertEquals(20.1, v.get(1));
    }

    // =================================================================
    // 3. NEGATE TESTS
    // =================================================================

    @Test
    void testNegate_RegularValues() {
        SharedVector v = new SharedVector(new double[]{5.0, 1.0}, VectorOrientation.ROW_MAJOR);
        v.negate(); 
        assertEquals(-5.0, v.get(0));
        assertEquals(-1.0, v.get(1));
    }

    @Test
    void testNegate_ZeroHandling() {
        SharedVector v = new SharedVector(new double[]{0.0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-0.0, v.get(0));
        assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits(v.get(0)));
    }

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

    @Test
    void testDot_NullOther_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        assertThrows(NullPointerException.class, () -> v1.dot(null));
    }

    @Test
    void testDot_Scalaric() {
        SharedVector v1 = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{2}, VectorOrientation.ROW_MAJOR);
        assertEquals(10.0, v1.dot(v2));
    }

    @Test
    void testDot_Standard() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        assertEquals(11.0, v1.dot(v2));
    }

    @Test
    void testDot_Infinity() {
        SharedVector v1 = new SharedVector(new double[]{Double.POSITIVE_INFINITY, 1}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 1}, VectorOrientation.ROW_MAJOR);
        assertEquals(Double.POSITIVE_INFINITY, v1.dot(v2));
    }

    @Test
    void testDot_DimensionMismatch_ThisLonger_ThrowsException() {
        SharedVector v1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> v1.dot(v2));
    }

    @Test
    void testDot_SelfReference() {
        SharedVector v1 = new SharedVector(new double[]{2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(13.0, v1.dot(v1)); 
    }

    // =================================================================
    // 5. VEC-MAT-MUL TESTS
    // =================================================================

    @Test
    void testVecMatMul_NullMatrix_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);
        Exception e = assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(null));
        assertEquals("matrix cant be null", e.getMessage());
    }

    @Test
    void testVecMatMul_DimensionMismatch_ThrowsException() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR); // Len 3
        
        // Matrix must be loaded properly
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{1, 0}, {0, 1}}); // 2x2 Matrix
        
        // v.len (3) != mat.rows (2)
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

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
}
