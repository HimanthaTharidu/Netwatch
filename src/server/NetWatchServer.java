package server;

import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.*;

public class NetWatchServer {
    private static final int PORT = 5000;

    public static void main(String[] args)throws IOException{
        BroadcastManager broadcastManager = new BroadcastManager();
        DeviceRegistry registry = new DeviceRegistry();

        registry.add("Router", "192.168.1.1", 0);
        registry.add("TestService", "127.0.0.1", 9000);

        startFakeMonitor(registry, broadcastManager);

        ExecutorService pool = Executors.newCachedThreadPool();

        try( ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("NetWatch server listening on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Connection from " + socket.getInetAddress().getHostAddress());
                pool.submit(new ClientHandler(socket, broadcastManager, registry));
            }
        }
    }

    //TEMPORARY
    //DeviceMonitor replace this (Lakma)
    public static void startFakeMonitor(DeviceRegistry registry, BroadcastManager broadcastManager){
        ScheduledExecutorService schedular = Executors.newSingleThreadScheduledExecutor();
        Random random = new Random();

        schedular.scheduleWithFixedDelay(() -> {
            try {
                for (DeviceRegistry.Device d : registry.all()) {
                    d.status = random.nextInt(4) == 0 ? "OFFLINE" : "ONLINE";
                    d.time = LocalTime.now().withNano(0).toString();
                    broadcastManager.broadcast(
                            "DEVICE|" + d.name + "|" + d.address + "|" + d.status + "|" + d.time);
                }
            } catch (Exception e) {
                // If an exception escapes, the scheduler SILENTLY stops repeating the task
                System.out.println("Monitor error: " + e);
            }

        }, 0, 5, TimeUnit.SECONDS);
    }
}
