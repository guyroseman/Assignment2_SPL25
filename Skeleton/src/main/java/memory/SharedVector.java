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
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors
        other.readLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                // Access to this.vector[i] is safe because the caller holds the WRITE LOCK
                this.vector[i] += other.get(i); 
            }
        } finally {
            other.readUnlock();
        }
    }

    public void negate() {
        // TODO: negate vector
        for(int i = 0; i < vector.length; i++){
            this.vector[i] = this.vector[i] * -1;
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        other.readLock(); 
        try{
            double sum = 0;

            for(int i = 0; i < vector.length; i++){
            sum += this.vector[i] * other.get(i);
            }

            return sum;

        }finally{
            other.readUnlock();
        }
    }

    // check read lock responsibillity upon "source" matrix in vecmatmul
    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        
        // Resolve dimensions
        int matRows;
        int matCols;
        VectorOrientation matOrient = matrix.getOrientation();

        if (matOrient == VectorOrientation.ROW_MAJOR) {
            matRows = matrix.length();
            matCols = matrix.get(0).length();
        } else {
            matCols = matrix.length();
            matRows = matrix.get(0).length();
        }

        // Validate Dimensions
        if (this.vector.length != matRows) {
            throw new IllegalArgumentException("Dimension mismatch: Vector length " +
                    this.vector.length + " != Matrix rows " + matRows);
        }

        double[] tempResult = new double[matCols];

        // Matrix is Col Major
        if (matOrient == VectorOrientation.COLUMN_MAJOR) {
            for (int col = 0; col < matCols; col++) {
                SharedVector colVector = matrix.get(col); 
                
                tempResult[col] = this.dot(colVector); 
            }
        }
        
        // Matrix is Row-Major
        else {
            for (int col = 0; col < matCols; col++) {
                double sum = 0;

                for (int row = 0; row < matRows; row++) {
                    double matVal = matrix.get(row).get(col); // .get(col) calls for readlock/unlock

                    sum += this.vector[row] * matVal;
                }
                tempResult[col] = sum;
            }
        }
        this.vector = tempResult;
        this.orientation = VectorOrientation.ROW_MAJOR;
    }
}
    
