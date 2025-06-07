package org.example.nursfire2.sniffer;

import org.example.nursfire2.ML.PacketClassifier;
import org.example.nursfire2.database.DatabaseManager;
import org.example.nursfire2.models.PredictionResult;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import java.util.List;
import java.util.UUID;

public class PacketSniffer {
    static PacketClassifier classifier = new PacketClassifier();

    public static void startSniffing() throws PcapNativeException, NotOpenException, InterruptedException {
        //получение актуального интерфейса
        PcapNetworkInterface nif = getAvailableNetworkInterface();
        if (nif == null) {
            System.out.println("Error: No active network interface found.");
            return;
        }

        int snapLen = 65536;
        PcapNetworkInterface.PromiscuousMode mode = PcapNetworkInterface.PromiscuousMode.PROMISCUOUS;
        int timeout = 10;
        //перхват пакетов
        PcapHandle handle = nif.openLive(snapLen, mode, timeout);

        PacketListener listener = packet -> {
            try {
                String id = UUID.randomUUID().toString();
                String srcIp = "unknown", dstIp = "unknown";
                int srcPort = -1, dstPort = -1;
                String protocol = packet.getClass().getSimpleName();
                int packetSize = packet.length();
                int ttl = -1;
                String flags = "";
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
                    flags += tcpHeader.getSyn() ? "SYN " : "";
                    flags += tcpHeader.getAck() ? "ACK " : "";
                    flags += tcpHeader.getFin() ? "FIN " : "";
                    flags += tcpHeader.getRst() ? "RST " : "";
                    flags += tcpHeader.getPsh() ? "PSH " : "";
                    flags += tcpHeader.getUrg() ? "URG " : "";
                    flags = flags.trim();
                } else if (packet.contains(UdpPacket.class)) {
                    UdpPacket udp = packet.get(UdpPacket.class);
                    srcPort = udp.getHeader().getSrcPort().valueAsInt();
                    dstPort = udp.getHeader().getDstPort().valueAsInt();
                }

                // Сохранение пакета в базу данных
                DatabaseManager.insertCapturedPacket(id, srcIp, dstIp, srcPort, dstPort, protocol, packetSize, packet.getRawData());
                DatabaseManager.insertPacketMetadata(id, ttl, flags, payloadSize, "Unknown", -1f);

                System.out.println("Packet: " + protocol + " | " + srcIp + " → " + dstIp);

                // Признаки для классификации
                double[] features = new double[] {
                        srcPort, dstPort, packetSize, ttl, payloadSize,
                        timestamp,  // Время пакета в миллисекундах
                        (flags.contains("SYN") ? 1 : 0), // Признак наличия флага SYN
                        (flags.contains("ACK") ? 1 : 0), // Признак наличия флага ACK
                        (flags.contains("FIN") ? 1 : 0), // Признак наличия флага FIN
                        (flags.contains("RST") ? 1 : 0), // Признак наличия флага RST
                        (flags.contains("PSH") ? 1 : 0), // Признак наличия флага PSH
                        (flags.contains("URG") ? 1 : 0)  // Признак наличия флага URG
                };

                // Классификация пакета
                PredictionResult prediction = classifier.classify(features);

                // Сохранение логов предсказания
                String predictionId = UUID.randomUUID().toString();
                DatabaseManager.insertMLPrediction(predictionId, id, prediction.getModelVersion(), prediction.getPredictedClass(), prediction.getConfidence());

                if ("attack".equals(prediction.getPredictedClass())) {
                    String attackId = UUID.randomUUID().toString();
                    DatabaseManager.insertAttack(attackId, id, prediction.getPredictedClass(), 5, "ML Detection");
                    System.out.println(" Attack detected!");
                } else {
                    System.out.println(" Successful packet.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        // Запуск перехвата в отдельном потоке
        System.out.println("Started packet sniffing... (Press Ctrl+C to stop)");
        handle.loop(-1, listener);  // Это блокирует текущий поток, но не блокирует UI поток
    }

    private static PcapNetworkInterface getAvailableNetworkInterface() {
        try {
            List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
            // Перебираем все интерфейсы
            for (PcapNetworkInterface nif : allDevs) {
                System.out.println("Interface: " + nif.getName());

                // Перебираем все адреса, связанные с этим интерфейсом
                for (PcapAddress addr : nif.getAddresses()) {
                    // Если у адреса есть IPv4-адрес, выводим его
                    if (addr.getAddress() != null && addr.getAddress().getHostAddress().contains(".")) {
                        System.out.println("IP address: " + addr.getAddress().getHostAddress());
                    }
                }
            }
            PcapNetworkInterface nif = allDevs.get(3);
            return nif;
        } catch (PcapNativeException e) {
            e.printStackTrace();
        }
        return null;
    }
}
