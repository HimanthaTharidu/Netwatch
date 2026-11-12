
package server;

import java.net.ServerSocket;
import java.net.Socket;

public class TestService
{
    public static void main(String[] args)
    {
        try (ServerSocket server = new ServerSocket(6000))
        {
            System.out.println(
                    "Test service running on port 6000");

            while (true)
            {
                try (Socket client = server.accept())
                {
                    System.out.println("Probe received");
                }
            }
        }
        catch (Exception e)
        {
            System.out.println(
                    "Test service stopped: " + e.getMessage());
        }
    }
}
