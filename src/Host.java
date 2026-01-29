import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Host {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java Host <ID> <configPath>");
            return;
        }

        String myId = args[0];
        String configPath = args[1];

        Config cfg = Config.load(configPath);
        DeviceConfig me = cfg.devices.get(myId);
        if (me == null) throw new IllegalArgumentException("Unknown device ID: " + myId);

        List<DeviceConfig> neighbors = cfg.neighborsOf(myId);
        if (neighbors.size() != 1) {
            throw new IllegalStateException("Host must have exactly 1 neighbor. Found: " + neighbors);
        }
        DeviceConfig connectedSwitch = neighbors.get(0);

        DatagramSocket sock = new DatagramSocket(me.port);
        System.out.println("Host " + myId + " listening on " + me.ip + ":" + me.port +
                " (connected to " + connectedSwitch + ")");

        Thread receiver = new Thread(() -> {
            byte[] buf = new byte[2048];
            while (true) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    sock.receive(pkt);

                    String frame = new String(pkt.getData(), 0, pkt.getLength(), StandardCharsets.UTF_8);
                    String[] parts = frame.split(":", 3);
                    if (parts.length < 3) {
                        System.out.println("[Host " + myId + "] Bad frame: " + frame);
                        continue;
                    }

                    String src = parts[0];
                    String dst = parts[1];
                    String msg = parts[2];

                    if (!dst.equals(myId)) {
                        System.out.println("[Host " + myId + "] MAC mismatch (flooded). dst=" + dst + " from=" + src);
                    } else {
                        System.out.println("[Host " + myId + "] from " + src + ": " + msg);
                    }
                } catch (Exception e) {
                    System.out.println("[Host " + myId + "] Receiver error: " + e.getMessage());
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Enter <DEST_ID> <message>: ");
            if (!sc.hasNext()) break;

            String dest = sc.next();
            String message = sc.nextLine().trim();

            String frame = myId + ":" + dest + ":" + message;
            byte[] data = frame.getBytes(StandardCharsets.UTF_8);

            DatagramPacket out = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName(connectedSwitch.ip),
                    connectedSwitch.port
            );
            sock.send(out);
        }
    }
}