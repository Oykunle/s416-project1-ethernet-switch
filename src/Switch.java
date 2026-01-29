import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Switch {

    private static final Map<String, String> switchTable = new HashMap<>();

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Switch <ID> <configPath>");
            return;
        }

        String myId = args[0];
        String configPath = args[1];

        Config cfg = Config.load(configPath);
        DeviceConfig me = cfg.devices.get(myId);
        if (me == null) throw new IllegalArgumentException("Unknown device ID: " + myId);

        List<DeviceConfig> neighbors = cfg.neighborsOf(myId);
        if (neighbors.isEmpty()) throw new IllegalStateException("Switch has no neighbors.");

        Map<String, DeviceConfig> ports = new HashMap<>();
        for (DeviceConfig n : neighbors) {
            String key = n.ip + ":" + n.port;
            ports.put(key, n);
        }

        DatagramSocket sock = new DatagramSocket(me.port);
        System.out.println("Switch " + myId + " listening on " + me.ip + ":" + me.port);
        System.out.println("Neighbors: " + neighbors);

        byte[] buf = new byte[2048];
        while (true) {
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            sock.receive(pkt);

            String inKey = pkt.getAddress().getHostAddress() + ":" + pkt.getPort();
            String frame = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8);

            String[] parts = frame.split(":", 3);
            if (parts.length < 3) {
                System.out.println("[Switch " + myId + "] Bad frame: " + frame);
                continue;
            }

            String src = parts[0];
            String dst = parts[1];

            if (!switchTable.containsKey(src)) {
                switchTable.put(src, inKey);
                printSwitchTable(myId);
            }

            if (switchTable.containsKey(dst)) {
                String outKey = switchTable.get(dst);
                DeviceConfig outNeighbor = ports.get(outKey);

                if (outNeighbor != null && !outKey.equals(inKey)) {
                    sendTo(sock, outNeighbor, frame);
                } else {
                    flood(sock, ports, inKey, frame);
                }
            } else {
                flood(sock, ports, inKey, frame);
            }
        }
    }

    private static void sendTo(DatagramSocket sock, DeviceConfig n, String frame) throws Exception {
        byte[] data = frame.getBytes(StandardCharsets.UTF_8);
        DatagramPacket out = new DatagramPacket(
                data, data.length,
                InetAddress.getByName(n.ip),
                n.port
        );
        sock.send(out);
    }

    private static void flood(DatagramSocket sock, Map<String, DeviceConfig> ports, String inKey, String frame) throws Exception {
        for (Map.Entry<String, DeviceConfig> e : ports.entrySet()) {
            String outKey = e.getKey();
            if (outKey.equals(inKey)) continue;
            sendTo(sock, e.getValue(), frame);
        }
    }

    private static void printSwitchTable(String switchId) {
        System.out.println("=== Switch Table @ " + switchId + " ===");
        for (Map.Entry<String, String> e : switchTable.entrySet()) {
            System.out.println("MAC " + e.getKey() + " -> port " + e.getValue());
        }
        System.out.println("===============================");
    }
}