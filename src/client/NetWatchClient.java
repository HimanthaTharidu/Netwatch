package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class NetWatchClient {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private volatile boolean connected = false;
    private volatile boolean reconnecting = false;
    private volatile boolean manuallyDisconnected = false;

    private Consumer<String> messageHandler;

    private final String host;
    private final int port;

    public NetWatchClient(String host, int port){
        this.host = host;
        this.port = port;
    }


    public synchronized void connect() throws IOException {

        manuallyDisconnected = false;

        socket = new Socket(host, port);

        reader = new BufferedReader(
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

        connected = true;

        System.out.println("Connected to NetWatch server.");
    }

    public synchronized void sendCommand(String command)
            throws IOException {

        if (!connected || socket == null || socket.isClosed()) {
            throw new IOException("Client is not connected.");
        }

        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    public void startListening(Consumer<String> messageHandler) {

        this.messageHandler = messageHandler;

        Thread listenerThread = new Thread(() -> {

            while (connected && !manuallyDisconnected) {

                try {

                    String message;

                    while (connected &&
                           (message = reader.readLine()) != null) {

                        this.messageHandler.accept(message);
                    }

                    if (connected) {
                        throw new IOException("Server disconnected.");
                    }

                } catch (IOException e) {

                    if (!manuallyDisconnected) {
                        System.out.println(
                            "Connection lost: " + e.getMessage()
                        );
                    }

                } finally {

                    closeConnection();
                    if (!manuallyDisconnected && messageHandler != null)
                    {
                        messageHandler.accept("CONNECTION|DISCONNECTED");
                    }
                }

                if (!manuallyDisconnected) {
                    reconnect();
                }
            }

        });

        listenerThread.setName("NetWatch-Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void reconnect() {

        if (reconnecting || manuallyDisconnected) {
            return;
        }

        reconnecting = true;

        try {

            while (!connected && !manuallyDisconnected) {

                System.out.println(
                    "Attempting to reconnect to NetWatch server..."
                );

                try {

                    Thread.sleep(5000);

                    if (manuallyDisconnected) {
                        break;
                    }

                    connect();

                    System.out.println(
                        "Reconnected to NetWatch server."
                    );

                    messageHandler.accept(
                        "CONNECTION|RECONNECTED"
                    );

                    String message;

                    while (connected &&
                           (message = reader.readLine()) != null) {

                        messageHandler.accept(message);
                    }

                    if (connected) {
                        throw new IOException("Server disconnected.");
                    }

                } catch (IOException e) {

                    closeConnection();

                    if (!manuallyDisconnected) {
                        System.out.println(
                            "Reconnection failed: " + e.getMessage()
                        );
                    }
                }
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            reconnecting = false;
        }
    }

    private synchronized void closeConnection() {

        connected = false;

        try {

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {

            System.out.println(
                "Error closing connection: " + e.getMessage()
            );
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {

        manuallyDisconnected = true;
        closeConnection();

        System.out.println("Disconnected from NetWatch server.");
    }
}
