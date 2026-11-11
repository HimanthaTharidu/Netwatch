package server;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BroadcastManager {

    private final Set<ClientHandler> clients = new CopyOnWriteArraySet<>();

    public void registerClient(ClientHandler handler) {
        clients.add(handler);
        System.out.println("Client connected. Total clients = " + clients.size());
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        System.out.println("Client disconnected. Total clients = " + clients.size());
    }

    public void broadcast(String line){
        for (ClientHandler handler : clients){
            handler.send(line);
        }
    }

}
