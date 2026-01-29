public class DeviceConfig {
    public final String id;
    public final String ip;
    public final int port;

    public DeviceConfig(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public String toString() {
        return id + "(" + ip + ":" + port + ")";
    }
}