package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable{

    private final Socket socket;
    private final BroadcastManager broadcastManager;
    private final DeviceRegistry registry;
    private BufferedWriter writer;

    private final Object writeLock = new Object();
    private final AtomicBoolean closed = new AtomicBoolean();
    public ClientHandler(Socket socket, BroadcastManager broadcastManager, DeviceRegistry registry){
            this.socket = socket;
            this.broadcastManager = broadcastManager;
            this.registry = registry;
    }

    @Override
    public void run(){
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            broadcastManager.registerClient(this);
            sendSnapshot();
            String line;
            while ((line = reader.readLine()) != null){
                try {
                    handle(line);
                }catch (RuntimeException e){
                    send("ERROR|could not process command");
                }
            }
        }catch (IOException e) {
            System.out.println("Connection problem: " + e.getMessage());
        } finally {
            close();
        }
    }

    //---------------command handling------------------------------
    private void handle(String line) {
        System.out.println("Command received " + line);
        String[] parts = line.trim().split("\\|");
        String command = parts[0].toUpperCase();

        switch (command){
            case "GET_STATUS":
                sendSnapshot();
                break;
            case "GET_LOGS":
                send("SUCCESS|no logs"); //replace with LogManager
                break;
            case "ADD_DEVICE":
                addDevice(parts);
                break;
            case "REMOVE_DEVICE":
                removeDevice(parts);
                break;
            default:
                send("ERROR|unknown command");
        }
    }

    private void addDevice(String[] parts){
        //ADD_DEVICE|name|address|port
        if (parts.length != 4) {
            send("ERROR|usage: ADD_DEVICE|name|address|port");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(parts[3]);
        }catch (NumberFormatException e){
            send("ERROR|port must be a number");
            return;
        }

        String name = parts[1];
        String address = parts[2];

        if (name.isEmpty() || address.isEmpty()){
            send("ERROR|name and address cannot be empty");
            return;
        }
        if (registry.add(name, address, port)){
            broadcastManager.broadcast("ADDED|" + name + "|" + address + "|" + address);
        }else {
            send("ERROR|device already exist: " + name);
        }


    }

    private void removeDevice(String[] parts){
        //REMOVE_DEVICE|name|address
        if (parts.length != 3){
            send("ERROR|usage: REMOVE_DEVICE|name|address");
            return;
        }

        String name = parts[1].trim();
        String address = parts[2].trim();

        if (name.isEmpty() || address.isEmpty()){
            send("ERROR|name and address cannot be empty");
            return;
        }

        if (registry.remove(name, address)){
            broadcastManager.broadcast("REMOVED|" + name + "|" + address);
        }else {
            send("ERROR|no such device: " + name);
        }
    }

    public void sendSnapshot(){
        for (DeviceRegistry.Device d : registry.all()){
            send("DEVICE|" + d.name + "|" + d.address + "|" + d.status + "|" + d.time);
        }

    }

    public void send(String line){
        if(closed.get()) return;
        try {
            synchronized (writeLock){
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        }catch (IOException e) {
            System.out.println("send failed, removing client: " + e.getMessage());
            close();
        }
    }

    //-----------------cleanup---------------------------

    private void close(){
        if (!closed.compareAndSet(false, true)){
            return;
        }
        broadcastManager.removeClient(this);
        try {
            socket.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
