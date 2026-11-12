
package server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import model.NetworkDevice;

public class DeviceMonitor
{
    private final DeviceRegistry registry;
    private final BroadcastManager broadcastManager;

    private final Map<String, NetworkDevice> monitoringStates =
            new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private boolean started = false;

    public DeviceMonitor(DeviceRegistry registry, BroadcastManager broadcastManager)
    {
        this.registry = registry;
        this.broadcastManager = broadcastManager;
    }

    public synchronized void startMonitoring()
    {
        if (started)
        {
            return;
        }

        started = true;

        scheduler.scheduleWithFixedDelay(
                this::checkAllDevices,
                0,
                5,
                TimeUnit.SECONDS
        );

        LogManager.log("SYSTEM", "Network monitoring started");
    }

    private void checkAllDevices()
    {
        try
        {
            for (DeviceRegistry.Device device : registry.all())
            {
                checkDevice(device);
            }

            monitoringStates.keySet().removeIf(
                    name -> registry.get(name) == null
            );
        }
        catch (Exception e)
        {
            LogManager.log("ERROR", "Monitoring failed: " + e.getMessage());
        }
    }

    private void checkDevice(DeviceRegistry.Device device)
    {
        NetworkDevice state = monitoringStates.compute(
                device.name,
                (name, existing) ->
                {
                    if (existing == null
                            || !existing.getAddress().equals(device.address)
                            || existing.getPort() != device.port)
                    {
                        return new NetworkDevice(
                                device.name,
                                device.address,
                                device.port
                        );
                    }

                    return existing;
                }
        );

        boolean reachable = false;
        long duration = -1;

        long startTime = System.nanoTime();

        try
        {
            if (device.port == 0)
            {
                InetAddress address = InetAddress.getByName(device.address);
                reachable = address.isReachable(2000);
            }
            else
            {
                try (Socket socket = new Socket())
                {
                    socket.connect(
                            new InetSocketAddress(device.address, device.port),
                            2000
                    );

                    reachable = true;
                }
            }

            if (reachable)
            {
                duration = TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime() - startTime
                );
            }
        }
        catch (Exception e)
        {
            reachable = false;
        }

        if (registry.get(device.name) != device)
        {
            return;
        }

        String change = state.updateStatus(reachable, duration);

        if (!state.isChecked())
        {
            return;
        }

        String status = state.isOnline() ? "ONLINE" : "OFFLINE";

        device.status = status;
        device.time = String.valueOf(state.getResponseTime());

        String message = "DEVICE|"
                + device.name + "|"
                + device.address + "|"
                + status + "|"
                + device.time;

        broadcastManager.broadcast(message);

        if (change.equals("INITIAL"))
        {
            LogManager.log(
                    "STATUS",
                    device.name + " is " + status
            );
        }
        else if (change.equals("OFFLINE"))
        {
            LogManager.log(
                    "ALERT",
                    device.name + " is OFFLINE"
            );

            broadcastManager.broadcast(
                    "ALERT|" + device.name + "|Device unreachable"
            );
        }
        else if (change.equals("RECOVERED"))
        {
            LogManager.log(
                    "RECOVERY",
                    device.name + " is ONLINE again"
            );

            broadcastManager.broadcast(
                    "ALERT|" + device.name + "|Connection restored"
            );
        }
    }
}
