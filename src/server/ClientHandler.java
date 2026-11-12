
package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable
{
    private final Socket socket;
    private final BroadcastManager broadcastManager;
    private final DeviceRegistry registry;

    private BufferedWriter writer;

    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ClientHandler(Socket socket, BroadcastManager broadcastManager, DeviceRegistry registry)
    {
        this.socket = socket;
        this.broadcastManager = broadcastManager;
        this.registry = registry;
    }

    @Override
    public void run()
    {
        try
        {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            socket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            broadcastManager.registerClient(this);

            sendSnapshot();

            String line;

            while (!closed.get() && (line = reader.readLine()) != null)
            {
                try
                {
                    handle(line);
                }
                catch (RuntimeException e)
                {
                    LogManager.log(
                            "ERROR",
                            "Client command failed: " + e.getMessage()
                    );

                    send("ERROR|Could not process command");
                }
            }
        }
        catch (IOException e)
        {
            if (!closed.get())
            {
                System.out.println(
                        "Connection problem: " + e.getMessage()
                );
            }
        }
        finally
        {
            close();
        }
    }

    private void handle(String line)
    {
        if (line == null || line.trim().isEmpty())
        {
            send("ERROR|Empty command");
            return;
        }

        String[] parts = line.trim().split("\\|", -1);

        String command = parts[0].trim().toUpperCase();

        switch (command)
        {
            case "GET_STATUS":
                sendSnapshot();
                break;

            case "GET_LOGS":
                sendRecentLogs();
                break;

            case "ADD_DEVICE":
                addDevice(parts);
                break;

            case "REMOVE_DEVICE":
                removeDevice(parts);
                break;

            case "EXIT":
                send("SUCCESS|Goodbye");
                close();
                break;

            default:
                send("ERROR|Unknown command");
                break;
        }
    }

    private void addDevice(String[] parts)
    {
        if (parts.length != 4)
        {
            send("ERROR|Usage: ADD_DEVICE|name|address|port");
            return;
        }

        String name = parts[1].trim();
        String address = parts[2].trim();

        if (name.isEmpty() || address.isEmpty())
        {
            send("ERROR|Name and address cannot be empty");
            return;
        }

        int port;

        try
        {
            port = Integer.parseInt(parts[3].trim());
        }
        catch (NumberFormatException e)
        {
            send("ERROR|Port must be a number");
            return;
        }

        if (port < 0 || port > 65535)
        {
            send("ERROR|Port must be between 0 and 65535");
            return;
        }

        synchronized (registry)
        {
            if (!registry.add(name, address, port))
            {
                send("ERROR|Device already exists or invalid details");
                return;
            }

            if (!DeviceStorage.saveDevices(registry))
            {
                registry.remove(name, address);

                send("ERROR|Failed to save device");
                return;
            }
        }

        LogManager.log(
                "DEVICE_ADDED",
                name + " added at " + address + ":" + port
        );

        broadcastManager.broadcast(
                "ADDED|" + name + "|" + address + "|" + port
        );

        send("SUCCESS|Device added");
    }

    private void removeDevice(String[] parts)
    {
        if (parts.length != 3)
        {
            send("ERROR|Usage: REMOVE_DEVICE|name|address");
            return;
        }

        String name = parts[1].trim();
        String address = parts[2].trim();

        if (name.isEmpty() || address.isEmpty())
        {
            send("ERROR|Name and address cannot be empty");
            return;
        }

        int oldPort;

        synchronized (registry)
        {
            DeviceRegistry.Device device = registry.get(name);

            if (device == null || !device.address.equals(address))
            {
                send("ERROR|Device not found or address does not match");
                return;
            }

            oldPort = device.port;

            if (!registry.remove(name, address))
            {
                send("ERROR|Failed to remove device");
                return;
            }

            if (!DeviceStorage.saveDevices(registry))
            {
                registry.add(name, address, oldPort);

                send("ERROR|Failed to save device removal");
                return;
            }
        }

        LogManager.log(
                "DEVICE_REMOVED",
                name + " removed from " + address
        );

        broadcastManager.broadcast(
                "REMOVED|" + name + "|" + address
        );

        send("SUCCESS|Device removed");
    }

    public void sendSnapshot()
    {
        for (DeviceRegistry.Device device : registry.all())
        {
            send(
                    "DEVICE|"
                    + device.name + "|"
                    + device.address + "|"
                    + device.status + "|"
                    + device.time
            );
        }
    }

    private void sendRecentLogs()
    {
        List<String> logs = LogManager.getRecentLogs(50);

        for (String entry : logs)
        {
            send("LOG|" + entry);
        }

        send("SUCCESS|Logs retrieved");
    }

    public void send(String line)
    {
        if (closed.get())
        {
            return;
        }

        synchronized (writeLock)
        {
            if (closed.get() || writer == null)
            {
                return;
            }

            try
            {
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
            catch (IOException e)
            {
                System.out.println(
                        "Send failed: " + e.getMessage()
                );

                close();
            }
        }
    }

    private void close()
    {
        if (!closed.compareAndSet(false, true))
        {
            return;
        }

        broadcastManager.removeClient(this);

        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            System.out.println(
                    "Socket close failed: " + e.getMessage()
            );
        }
    }
}
