package org.example.nursfire2.models;

public class PredictionResult {
    private String predictedClass;
    private String modelVersion; // Теперь это String, чтобы хранить "J48" или подобное
    private double confidence;   // Теперь это double, так как Weka возвращает double для уверенности

    // Это новый конструктор, который соответствует вашему вызову в PacketClassifier
    // Он принимает: (String для класса, String для версии модели, double для уверенности)
    public PredictionResult(String predictedClass, String modelVersion, double confidence) {
        this.predictedClass = predictedClass;
        this.modelVersion = modelVersion;
        this.confidence = confidence;
    }

    // --- Геттеры для доступа к данным ---
    public String getPredictedClass() {
        return predictedClass;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public double getConfidence() {
        return confidence;
    }

    // --- Необязательно: переопределение toString() для удобства отладки ---
    @Override
    public String toString() {
        return "PredictionResult{" +
                "predictedClass='" + predictedClass + '\'' +
                ", modelVersion='" + modelVersion + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}