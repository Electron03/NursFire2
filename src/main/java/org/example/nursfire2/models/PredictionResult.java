package org.example.nursfire2.models;

public class PredictionResult {
    private String predictedClass;
    private float confidence;
    private String modelVersion;

    public PredictionResult(String predictedClass, float confidence, String modelVersion) {
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.modelVersion = modelVersion;
    }

    public String getPredictedClass() {
        return predictedClass;
    }

    public float getConfidence() {
        return confidence;
    }

    public String getModelVersion() {
        return modelVersion;
    }
}