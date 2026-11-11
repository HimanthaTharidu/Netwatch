package server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceRegistry {

    public static class Device{
        final String name;
        final String address;
        final int port;
        volatile String status = "UNKNOWN";
        volatile String time = "--";

        Device(String name, String address, int port){
            this.address = address;
            this.name = name;
            this.port = port;
        }
    }
    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    public boolean add(String name, String address, int port){
        return devices.putIfAbsent(name, new Device(name, address, port)) == null;
    }

    public boolean remove(String name, String address){
        Device d = devices.get(name);
        if (d == null || d.address.equals(address)){
            return false;
        }
        return devices.remove(name, d);
    }

    public Device get(String name) {
        return devices.get(name);
    }

    public Collection<Device> all() {
        return devices.values();
    }

    public int size() {
        return devices.size();
    }
}
