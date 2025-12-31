package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // Handle empty or null matrix
        if (matrix == null) {
            throw new IllegalArgumentException("matrix cant be null");
        }

        // Initialize vectors from the provided matrix
        this.vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            this.vectors[i] = new SharedVector(matrix[i].clone(), VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadRowMajor(double[][] matrix) {
        // Capture the current state ("old matrix")
        SharedVector[] oldVectors = this.vectors;

        // Handle null matrix
        if (matrix == null) {
            throw new IllegalArgumentException("matrix cant be null");
        }
        // Handle empty matrix
        if (matrix.length == 0) {
            return;
        }

        // Lock all old vectors for writing
        // This ensures no other thread is reading/writing them while we swap
        acquireAllVectorWriteLocks(oldVectors);
        try {

            // Create new SharedVectors for the new matrix
            SharedVector[] newVectors = new SharedVector[matrix.length];

            for (int i = 0; i < matrix.length; i++) {
                double[] row = matrix[i].clone();
                newVectors[i] = new SharedVector(row, VectorOrientation.ROW_MAJOR);
            }

            // Swap in the new vectors
            this.vectors = newVectors;

        } finally {
            releaseAllVectorWriteLocks(oldVectors); // Release locks on the old vectors to allow others to access them
        }
    }

    public void loadColumnMajor(double[][] matrix) {
        // Capture the current state ("old matrix")
        SharedVector[] oldVectors = this.vectors;

         // Handle null matrix
        if (matrix == null) {
            throw new IllegalArgumentException("matrix cant be null");
        }
        // Handle empty matrix
        if (matrix.length == 0) {
            return;
        }

        // Lock all old vectors for writing
        // This ensures no other thread is reading/writing them while we swap
        acquireAllVectorWriteLocks(oldVectors);
        try {

            // Determine dimensions
            int rows = matrix.length;
            int cols = matrix[0].length; // Assuming a rectangular matrix

            // Create new SharedVectors for column-major storage
            SharedVector[] newVectors = new SharedVector[cols];

            // Transpose logic: Convert input rows into column vectors
            for (int col = 0; col < cols; col++) {
                double[] columnData = new double[rows];
                for (int row = 0; row < rows; row++) {
                    columnData[row] = matrix[row][col];
                }
                // Create SharedVector for this column
                newVectors[col] = new SharedVector(columnData, VectorOrientation.COLUMN_MAJOR);
            }

            // Swap in the new vectors
            this.vectors = newVectors;

        } finally {
            releaseAllVectorWriteLocks(oldVectors); // Release locks on the old vectors to allow others to access them
        }
    }

    public double[][] readRowMajor() {
       
        SharedVector[] tempVectors = this.vectors;

         // Handle empty matrix
        if (tempVectors.length == 0) {
            return new double[0][0];
        }       

        // Lock all temppVectors since they hold a pointer to the orginal matrix's vectors
        // this ensures consistent reading
        acquireAllVectorReadLocks(tempVectors);
        try {

            // Determine dimensions and orientation from the tempVectors
            int rows;
            int cols;
            VectorOrientation orient = tempVectors[0].getOrientation();

            if (orient == VectorOrientation.ROW_MAJOR) {
                rows = tempVectors.length;
                cols = tempVectors[0].length();
            } else {
                // If stored as columns, dimensions are flipped relative to storage
                cols = tempVectors.length;      // Number of stored vectors = number of columns
                rows = tempVectors[0].length(); // Length of each vector = number of rows
            }

            double[][] result = new double[rows][cols];

            // Read Data
            if (orient == VectorOrientation.ROW_MAJOR) {
                // Standard copy: Matrix rows map directly to storage vectors
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        result[i][j] = tempVectors[i].get(j);
                    }
                }
            } else {
                // Transpose copy: Matrix columns map to storage vectors
                for (int j = 0; j < cols; j++) {
                    for (int i = 0; i < rows; i++) {
                        // tempVectors[j] is the column vector at index j
                        // We read the i-th element from it to fill result[i][j]
                        result[i][j] = tempVectors[j].get(i);
                    }
                }
            }
            return result;

        } finally {
            releaseAllVectorReadLocks(tempVectors); // Release locks on the vectors to allow others to access them 
        } 
    }

    public SharedVector get(int index) {
        // To ensure we get the latest array ahead of comparsion
        SharedVector[] tempVectors = this.vectors;

        if(index < 0 || index >= tempVectors.length)
            throw new IllegalArgumentException("Index out of bounds");
        return tempVectors[index];
    }

    public int length() {
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // To ensure we get the latest array ahead of comparsion
        SharedVector[] tempVectors = this.vectors;
        
        if (tempVectors.length == 0){
            return null;
        }

        return tempVectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        if (vecs == null){
             return; 
        }
        for (SharedVector vec : vecs) {
            vec.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        if (vecs == null){
             return; 
        }
        for (int i = vecs.length - 1; i >= 0; i--) {
            SharedVector vec = vecs[i];
            vec.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        if (vecs == null){
             return; 
        }
        for (SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        if (vecs == null){
             return; 
        }
        for (int i = vecs.length - 1; i >= 0; i--) {
            SharedVector vec = vecs[i];
            vec.writeUnlock();
        }
    }
}