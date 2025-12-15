package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        
        // Acquire read lock
        readLock();
        try{
            return this.vector[index];
        }
        // Release read lock
        finally{
            readUnlock();
        }
    }

    public int length() {
        // TODO: return vector length
        
        // Acquire read lock
        readLock();
        try{
            return this.vector.length;
        }
        // Release read lock
        finally{
            readUnlock();
        }
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        
        // Acquire read lock
        readLock();
        try{
            return this.orientation;
        }
        // Release read lock
        finally{
            readUnlock();
        }
    }

    public void writeLock() {
        // TODO: acquire write lock
        this.lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        this.lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
        writeLock();
        try{
            if (orientation == VectorOrientation.ROW_MAJOR) {
                orientation = VectorOrientation.COLUMN_MAJOR;
            } else {
                orientation = VectorOrientation.ROW_MAJOR;
            }
        }
        finally{
            writeUnlock();
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        double[] otherSnapshot;
        
        // Acquire read lock
        other.readLock(); 
        try {
            // Creates an immutable copy of the other vector
            otherSnapshot = java.util.Arrays.copyOf(other.vector, other.vector.length);
        } finally {
        // Release read lock
        other.readUnlock();
        }

        // Aquire write lock
        this.writeLock();
        try {
            // Make additon between vectors
            for (int i = 0; i < vector.length; i++) {
                this.vector[i] += otherSnapshot[i];
            }
        } finally {
            // Release write lock
            this.writeUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        this.writeLock();
        try{
            for(int i = 0; i < vector.length; i++){
            this.vector[i] = this.vector[i] * -1;
            }
        }finally{
            this.writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        
        double[] otherSnapshot;

        // Immute copy other
        other.readLock(); 
        try {
            // Creates an immutable copy of the other vector
            otherSnapshot = java.util.Arrays.copyOf(other.vector, other.vector.length);
        } finally {
        // Release read lock
        other.readUnlock();
        }

        // Calculate dot result
        this.readLock();
        try{
            double sum = 0;

            for(int i = 0; i < vector.length; i++){
            // add to sum dot result of this with the immutable copy of other
            sum += this.vector[i] * otherSnapshot[i];
            }
    
            return sum;
        }finally{
            this.readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        
        // Acquire write lock as we modify the vector (in-place update).
        writeLock();
        try {
            int matRows;
            int matCols;

            // Resolve Matrix dimensions
            if (matrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
                matRows = matrix.length();
                matCols = matrix.get(0).length();
            } else {
                matCols = matrix.length();
                matRows = matrix.get(0).length();
            }

            // Validate Vector and Matrix dimensions 
            if (this.vector.length != matRows) {
                // Throw exception for dimension mismatch: V[1xN] * M[NxK]
                throw new IllegalArgumentException("Dimension mismatch: Vector length " +
                        this.vector.length + " != Matrix rows " + matRows);
            }

            // Create a temp vector to store result with size of [1 X matCols]
            double[] tempResult = new double[matCols];

            // Perform Multiplication when matrix stores columns as vectors
            if (matrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
                // Result[col] = V dot M[col]
                for (int col = 0; col < matCols; col++) {
                    SharedVector colVector = matrix.get(col);

                    tempResult[col] = this.dot(colVector);
                }
            }
            // Perform Multiplication when matrix stores rows as vectors (Standard Path)
            else {
                // Result[col] = Sum(V[row] * M[row][col])
                for (int col = 0; col < matCols; col++) {
                    double sum = 0;

                    for (int row = 0; row < matRows; row++) {
                        // Safety: matrix.get(row).get(col) handles the ReadLock on the specific row vector.
                        double matVal = matrix.get(row).get(col);

                        // Access to this.vector[row] is safe because we hold the WriteLock on 'this'.
                        sum += this.vector[row] * matVal;
                    }
                    tempResult[col] = sum;
                }
            }

            // Atomic Commit: Replace the internal vector data and update orientation
            this.vector = tempResult;
            this.orientation = VectorOrientation.ROW_MAJOR;

        } finally {
            writeUnlock();
        }
    }
}
    
