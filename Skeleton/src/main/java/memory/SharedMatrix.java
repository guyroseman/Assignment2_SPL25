package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if (matrix == null) {
            this.vectors = new SharedVector[0]; 
            return;
        }

        this.vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            this.vectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return null;
    }

    public int length() {
        // TODO: return number of stored vectors
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return null;
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        if (vecs == null){
             return; 
        }
        for (SharedVector vec : vecs) {
            if (vec != null) { 
                vec.readLock();
            }
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        if (vecs == null){
             return; 
        }
        for (int i = vecs.length - 1; i >= 0; i--) {
            SharedVector vec = vecs[i];
            if (vec != null) { 
                vec.readUnlock();
            }
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        if (vecs == null){
             return; 
        }
        for (SharedVector vec : vecs) {
            if (vec != null) { 
                vec.writeLock();
            }
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        if (vecs == null){
             return; 
        }
        for (int i = vecs.length - 1; i >= 0; i--) {
            SharedVector vec = vecs[i];
            if (vec != null) { 
                vec.writeUnlock();
            }
        }
    }
}