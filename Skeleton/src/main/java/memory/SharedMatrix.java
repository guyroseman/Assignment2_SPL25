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
        if (matrix == null || matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }

        // Create copies of all vectors as row majors
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for(int i = 0; i < matrix.length; i++){
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }

        // Update vectors of this matrix to the new row major vectors
        this.vectors = newVectors;
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
        if(matrix == null || matrix.length == 0){
            this.vectors = new SharedVector[0];
            return;
        }

        // Create copies of all vectors as col majors
        SharedVector[] newVectors = new SharedVector[matrix.length];
        for(int i = 0; i < matrix.length; i++){
            newVectors[i] = new SharedVector(matrix[i], VectorOrientation.COLUMN_MAJOR);
        }

        // Update vectors of this matrix to the new col major vectors
        this.vectors = newVectors;
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        return null; 
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        // To ensure we get the latest array ahead of comparsion
        SharedVector[] tempVectors = this.vectors;

        if(index < 0 || index > vectors.length)
            return null;
        return tempVectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        // To ensure we get the latest array ahead of comparsion
        SharedVector[] tempVectors = this.vectors;
        
        if (tempVectors.length == 0){
            return null;
        }

        return tempVectors[0].getOrientation();
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