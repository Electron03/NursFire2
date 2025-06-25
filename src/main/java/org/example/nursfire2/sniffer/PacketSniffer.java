package org.example.nursfire2.sniffer;

import org.example.nursfire2.ML.PacketClassifier;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.PredictionResult;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.awt.*;
import java.awt.TrayIcon.MessageType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketSniffer {
    static PacketClassifier classifier = new PacketClassifier();
    static long lastTimestamp = -1;

    public static void startSniffing() throws PcapNativeException, NotOpenException, InterruptedException {
        PcapNetworkInterface nif = getAvailableNetworkInterface();
        if (nif == null) {
            System.out.println("Error: No active network interface found.");
            return;
        }

        int snapLen = 65536;
        PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        int timeout = 10;

        PcapHandle handle = nif.openLive(snapLen, mode, timeout);

        PacketListener listener = packet -> {
            try {
                String id = UUID.randomUUID().toString();
                String srcIp = "unknown", dstIp = "unknown";
                int srcPort = -1, dstPort = -1;
                String protocol = packet.getClass().getSimpleName();
                int packetSize = packet.length();
                int ttl = -1;
                String flags = "NONE";
                int payloadSize = packet.getPayload() != null ? packet.getPayload().length() : 0;
                long timestamp = System.currentTimeMillis();

                if (packet.contains(IpV4Packet.class)) {
                    IpV4Packet ipPacket = packet.get(IpV4Packet.class);
                    srcIp = ipPacket.getHeader().getSrcAddr().getHostAddress();
                    dstIp = ipPacket.getHeader().getDstAddr().getHostAddress();
                    ttl = ipPacket.getHeader().getTtlAsInt();
                }

                if (packet.contains(TcpPacket.class)) {
                    TcpPacket tcp = packet.get(TcpPacket.class);
                    TcpPacket.TcpHeader tcpHeader = tcp.getHeader();
                    srcPort = tcpHeader.getSrcPort().valueAsInt();
                    dstPort = tcpHeader.getDstPort().valueAsInt();
                    if (tcpHeader.getSyn()) flags = "SYN";
                    else if (tcpHeader.getAck()) flags = "ACK";
                    else if (tcpHeader.getFin()) flags = "FIN";
                    else if (tcpHeader.getRst()) flags = "RST";
                    else if (tcpHeader.getPsh()) flags = "PSH";
                    else if (tcpHeader.getUrg()) flags = "URG";
                } else if (packet.contains(UdpPacket.class)) {
                    UdpPacket udp = packet.get(UdpPacket.class);
                    srcPort = udp.getHeader().getSrcPort().valueAsInt();
                    dstPort = udp.getHeader().getDstPort().valueAsInt();
                }

                // Сохраняем в БД
                DatabaseManager.insertCapturedPacket(id, srcIp, dstIp, srcPort, dstPort, protocol, packetSize, packet.getRawData());
                DatabaseManager.insertPacketMetadata(id, ttl, flags, payloadSize, "Unknown", -1f);

                System.out.println("Packet: " + protocol + " | " + srcIp + " → " + dstIp);

                // === Новые признаки ===
                double protocolCode = protocol.contains("Tcp") ? 0 :
                        protocol.contains("Udp") ? 1 :
                                protocol.contains("Icmp") ? 2 : -1;

                double tcpFlagCode = switch (flags) {
                    case "SYN" -> 0;
                    case "ACK" -> 1;
                    case "FIN" -> 2;
                    case "RST" -> 3;
                    case "PSH" -> 4;
                    case "URG" -> 5;
                    default -> 6;
                };

                int numConnections = ttl > 0 ? 64 - ttl : 0;

                double interArrivalTime = lastTimestamp > 0 ? (timestamp - lastTimestamp) / 1000.0 : 0.5;
                lastTimestamp = timestamp;

                double[] features = new double[] {
                        packetSize,
                        protocolCode,
                        numConnections,
                        tcpFlagCode,
                        dstPort,
                        interArrivalTime
                };

                PredictionResult prediction = classifier.classify(features);

                String predictionId = UUID.randomUUID().toString();
                DatabaseManager.insertMLPrediction(predictionId, id, prediction.getModelVersion(), prediction.getPredictedClass(), prediction.getConfidence());

                if ("normal".equalsIgnoreCase(prediction.getPredictedClass())) {
                    System.out.println("✅ Normal packet.");
                } else {
                    String attackId = UUID.randomUUID().toString();
                    DatabaseManager.insertAttack(attackId, id, prediction.getPredictedClass(), 5, "ML Detection");

                    if (SystemTray.isSupported()) {
                        SystemTray tray = SystemTray.getSystemTray();
                        TrayIcon trayIcon = new TrayIcon(new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB), "Java Notification");
                        trayIcon.setImageAutoSize(true);
                        trayIcon.setToolTip("Java уведомление");
                        tray.add(trayIcon);

                        trayIcon.displayMessage("Внимание", "Зафиксирована атака", MessageType.WARNING);
                    }

                    System.out.println("⚠️  Attack detected!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                handle.loop(-1, listener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("Started packet sniffing... (Press Ctrl+C to stop)");
        Thread.sleep(10_000);
        handle.breakLoop(); // Прерывает цикл захвата пакетов
        executor.shutdown();
        handle.close();
        System.out.println("Sniffing stopped after 10 seconds.");
    }

    private static PcapNetworkInterface getAvailableNetworkInterface() {
        try {
            List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
            for (PcapNetworkInterface nif : allDevs) {
                System.out.println("Interface: " + nif.getName());
                for (PcapAddress addr : nif.getAddresses()) {
                    if (addr.getAddress() != null && addr.getAddress().getHostAddress().contains(".")) {
                        System.out.println("IP address: " + addr.getAddress().getHostAddress());
                    }
                }
            }
            return allDevs.get(3); // или выбрать нужный вручную
        } catch (PcapNativeException e) {
            e.printStackTrace();
        }
        return null;
    }
}
