
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetWatchServer
{
    private static final int PORT = 5000;

    public static void main(String[] args) throws IOException
    {
        BroadcastManager broadcastManager = new BroadcastManager();

        DeviceRegistry registry = new DeviceRegistry();

        DeviceStorage.loadIntoRegistry(registry);

        if (registry.size() == 0)
        {
            registry.add("Localhost", "127.0.0.1", 0);
            registry.add("TestService", "127.0.0.1", 6000);

            DeviceStorage.saveDevices(registry);
        }

        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            System.out.println("NetWatch server listening on port " + PORT);

            LogManager.log("SYSTEM", "NetWatch server started on port " + PORT);

            DeviceMonitor monitor = new DeviceMonitor(registry, broadcastManager);

            monitor.startMonitoring();

            while (true)
            {
                Socket socket = serverSocket.accept();

                System.out.println(
                        "Connection from " + socket.getInetAddress().getHostAddress()
                );

                pool.submit(
                        new ClientHandler(socket, broadcastManager, registry)
                );
            }
        }
        finally
        {
            pool.shutdownNow();
        }
    }
}
