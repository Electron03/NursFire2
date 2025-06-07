package org.example.nursfire2.models;

public class MLPredictionLogEntry {
    private final String packetId;
    private final String modelVersion;
    private final String predictedClass;
    private final float confidence;
    private final String timestamp;

    public MLPredictionLogEntry(String packetId, String modelVersion, String predictedClass, float confidence, String timestamp) {
        this.packetId = packetId;
        this.modelVersion = modelVersion;
        this.predictedClass = predictedClass;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }

    public String getPacketId() { return packetId; }
    public String getModelVersion() { return modelVersion; }
    public String getPredictedClass() { return predictedClass; }
    public float getConfidence() { return confidence; }
    public String getTimestamp() { return timestamp; }
}
