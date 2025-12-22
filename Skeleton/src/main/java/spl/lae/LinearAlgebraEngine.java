package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        try {
            //check if the root is matrix node
            if(computationRoot.getNodeType() == ComputationNodeType.MATRIX){
                throw new IllegalArgumentException("The root node cannot be a matrix.");
            }
            ComputationNode resolvableNode = computationRoot.findResolvable();
            while (resolvableNode != null) {
                loadAndCompute(resolvableNode);
                resolvableNode.resolve(leftMatrix.readRowMajor());
                resolvableNode = computationRoot.findResolvable();
            }
            return computationRoot;
        } finally {
             try {
                executor.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
    }

    public void loadAndCompute(ComputationNode node) {

        switch (node.getNodeType()) {
            case ADD:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
                rightMatrix.loadRowMajor(node.getChildren().get(1).getMatrix());
                for(int i=0;i<leftMatrix.length();i++){
                    if(leftMatrix.get(i).length() != rightMatrix.get(i).length()){ 
                        throw new IllegalArgumentException("Matrix dimensions do not match for addition.");
                    }
                }
                executor.submitAll(createAddTasks());
                break;
            case MULTIPLY:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
                rightMatrix.loadColumnMajor(node.getChildren().get(1).getMatrix());
                 if (leftMatrix.length() > 0 && rightMatrix.length() > 0) {
                    int colsA = leftMatrix.get(0).length();
                    int rowsB = rightMatrix.get(0).length(); 
                    if (colsA != rowsB) {
                         throw new IllegalArgumentException("Matrix dimensions do not match for multiplication.");
                    }
                }
                executor.submitAll(createMultiplyTasks());
                break;
            case NEGATE:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
                executor.submitAll(createNegateTasks());
                break;
            case TRANSPOSE:
                leftMatrix.loadRowMajor(node.getChildren().get(0).getMatrix());
                executor.submitAll(createTransposeTasks());
                break;
            default:
                throw new IllegalArgumentException("Unsupported operation: " + node.getNodeType());
        }
    }

    public List<Runnable> createAddTasks() {
        // Each task adds one row from rightMatrix to the corresponding row in leftMatrix
        List<Runnable> tasks = new ArrayList<>();
        int len = leftMatrix.length(); 

        
        for (int i = 0; i < len; i++) {
            final SharedVector targetVector = leftMatrix.get(i);
            final SharedVector sourceVector = rightMatrix.get(i);
            
            Runnable task = () -> {
                // We must acquire the Write Lock on the target before calling the method
                targetVector.writeLock();
                try {
                    // .add() internally acquires Read Lock on sourceVector
                    targetVector.add(sourceVector);
                } finally {
                    targetVector.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // Each task multiplies one row from leftMatrix with rightMatrix
        List<Runnable> tasks = new ArrayList<>();
        int len = leftMatrix.length();

        for (int i = 0; i < len; i++) {
            final SharedVector targetVector = leftMatrix.get(i);
            final SharedMatrix sourceMatrix = rightMatrix;

            Runnable task = () -> {
                // We must acquire the Write Lock on the target before calling the method
                targetVector.writeLock();
                try {
                    // .vecMatMul() internally acquires Read Lock on sourceMatrix
                    targetVector.vecMatMul(sourceMatrix);
                } finally {
                    targetVector.writeUnlock();
                }
            };
            tasks.add(task);
        }
            return tasks;
        // check read lock responsibillity upon "source" matrix in vecmatmul
    }

    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new ArrayList<>();
        int len = leftMatrix.length();

        for(int i = 0; i < len; i++){
            final SharedVector targetVector = leftMatrix.get(i);

            Runnable task = () -> {
                targetVector.writeLock();
                try{
                    targetVector.negate();
                }finally{
                    targetVector.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new ArrayList<>();
        int len = leftMatrix.length();

        for(int i = 0; i < len; i++){
            final SharedVector targetVector = leftMatrix.get(i);

            Runnable task = () -> {
                targetVector.writeLock();
                try{
                    targetVector.transpose();
                }finally{
                    targetVector.writeUnlock();
                }
            };
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Worker Activity Report ---\n");
        sb.append(executor.getWorkerReport());
        return sb.toString();
    }
}
