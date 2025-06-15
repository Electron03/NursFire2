package org.example.nursfire2.models;

public class AttackEntry {
    private final String id;
    private final String packetId;
    private final String attackType;
    private final int severity;
    private final String detectionMethod;
    private final String detectedAt;

    public AttackEntry(String id, String packetId, String attackType, int severity, String detectionMethod, String detectedAt) {
        this.id = id;
        this.packetId = packetId;
        this.attackType = attackType;
        this.severity = severity;
        this.detectionMethod = detectionMethod;
        this.detectedAt = detectedAt;
    }

    public String getId() {
        return id;
    }

    public String getPacketId() {
        return packetId;
    }

    public String getAttackType() {
        return attackType;
    }

    public int getSeverity() {
        return severity;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public String getDetectedAt() {
        return detectedAt;
    }
}
