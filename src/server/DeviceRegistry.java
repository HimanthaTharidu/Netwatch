
package server;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceRegistry
{
    public static class Device
    {
        final String name;
        final String address;
        final int port;

        volatile String status = "UNKNOWN";
        volatile String time = "--";

        Device(String name, String address, int port)
        {
            this.name = name;
            this.address = address;
            this.port = port;
        }
    }

    private final Map<String, Device> devices = new ConcurrentHashMap<>();

    public boolean add(String name, String address, int port)
    {
        if (name == null || address == null)
        {
            return false;
        }

        name = name.trim();
        address = address.trim();

        if (name.isEmpty() || address.isEmpty())
        {
            return false;
        }

        if (port < 0 || port > 65535)
        {
            return false;
        }

        for (Device device : devices.values())
        {
            if (device.address.equals(address) && device.port == port)
            {
                return false;
            }
        }

        return devices.putIfAbsent(name, new Device(name, address, port)) == null;
    }

    public boolean remove(String name, String address)
    {
        Device device = devices.get(name);

        if (device == null || !device.address.equals(address))
        {
            return false;
        }

        return devices.remove(name, device);
    }

    public Device get(String name)
    {
        return devices.get(name);
    }

    public Collection<Device> all()
    {
        return devices.values();
    }

    public int size()
    {
        return devices.size();
    }
}
