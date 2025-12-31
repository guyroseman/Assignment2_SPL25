package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        if (vector==null) {
            throw new IllegalArgumentException("vector cant be null");
        }
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        readLock(); // Acquire read lock to ensure consistent read 
        try{
            if(index < 0 || index >= this.vector.length){
                throw new IllegalArgumentException("Index out of bounds");
            }
            return this.vector[index];
        }finally{
            readUnlock(); // Read finished then unlock in finally block to make sure unlock always happens so other threads can access it
        }
    }

    public int length() {
        readLock(); // Acquire read lock in order to ensure consistent read
        try{
            return this.vector.length;
        }
        finally{
            readUnlock(); // Read finished then unlock in finally block to make sure unlock always happens so other threads can access it
        }
    }

    public VectorOrientation getOrientation() {
        readLock(); // Acquire read lock to ensure consistent read
        try{
            return this.orientation;
        }
        finally{
            readUnlock(); // Read finished then unlock in finally block to make sure unlock always happens so other threads can access it
        }
    }

    public void writeLock() {
        this.lock.writeLock().lock();
    }

    public void writeUnlock() {
        this.lock.writeLock().unlock();
    }

    public void readLock() {
        this.lock.readLock().lock();
    }

    public void readUnlock() {
        this.lock.readLock().unlock();
    }

    public void transpose() {
        if (orientation == VectorOrientation.ROW_MAJOR) {
            orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    public void add(SharedVector other) {
        // lock other to ensure consistent reading
        // Access to this.vector is safe because the caller holds the WRITE LOCK
        other.readLock(); 
        try {
            for (int i = 0; i < vector.length; i++) {
                // Access to this.vector[i] is safe because the caller holds the WRITE LOCK
                this.vector[i] += other.vector[i];
            }
        } finally {
            other.readUnlock(); // unlock read lock in finally block to make sure unlock always happens so other threads can access it
        }
    }

    public void negate() {
        for(int i = 0; i < vector.length; i++){
            if (vector[i]!=0) {
                this.vector[i] = this.vector[i] * -1;
            }
        }
    }

    public double dot(SharedVector other) {
        if (this.vector.length!=other.vector.length) {
            throw new IllegalArgumentException("Vectors must be of the same length for dot product.");
        }
        // lock other to ensure consistent reading
        // Access to this.vector is safe because the caller holds the WRITE LOCK
        other.readLock(); 
        try{
            double sum = 0;

            // Compute dot product
            for(int i = 0; i < vector.length; i++){
            sum += this.vector[i] * other.vector[i];
            }
            return sum;

        }finally{
            other.readUnlock(); // unlock read lock in finally block to make sure unlock always happens so other threads can access it
        }
    }


    public void vecMatMul(SharedMatrix matrix) {
        // Resolve dimensions
        int matRows;
        int matCols;
        if (matrix==null) {
            throw  new IllegalArgumentException("matrix cant be null");
        }
        VectorOrientation matOrient = matrix.getOrientation();

        // Determine matrix dimensions based on orientation
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

        // Matrix is Col Major (We made it like this in LAE)
        for (int col = 0; col < matCols; col++) {
            SharedVector colVector = matrix.get(col);   
            tempResult[col] = this.dot(colVector); 
        }
        
        // Update vector to result
        this.vector = tempResult;
        this.orientation = VectorOrientation.ROW_MAJOR;
    }
}
    
