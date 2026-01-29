import java.io.*;
import java.util.*;

public class Config {
    public final Map<String, DeviceConfig> devices = new HashMap<>();
    public final List<Link> links = new ArrayList<>();

    public static Config load(String path) throws IOException {
        Config cfg = new Config();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts[0].equalsIgnoreCase("DEVICE")) {
                    // DEVICE <ID> <IP> <PORT>
                    String id = parts[1];
                    String ip = parts[2];
                    int port = Integer.parseInt(parts[3]);
                    cfg.devices.put(id, new DeviceConfig(id, ip, port));
                } else if (parts[0].equalsIgnoreCase("LINK")) {
                    // LINK <ID1> <ID2>
                    cfg.links.add(new Link(parts[1], parts[2]));
                } else {
                    throw new IllegalArgumentException("Bad config line: " + line);
                }
            }
        }
        return cfg;
    }

    public List<DeviceConfig> neighborsOf(String id) {
        List<DeviceConfig> out = new ArrayList<>();
        for (Link link : links) {
            if (link.touches(id)) {
                String other = link.other(id);
                out.add(devices.get(other));
            }
        }
        return out;
    }
}