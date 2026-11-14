
package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class DeviceStorage
{
    private static final Path FILE = AppPaths.devicesFile();

    public static synchronized List<String> loadDevices()
    {
        List<String> savedDevices = new ArrayList<>();

        if (!Files.exists(FILE))
        {
            return savedDevices;
        }

        try
        {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);

            for (String line : lines)
            {
                String[] parts = line.split("\\|", -1);

                if (parts.length != 3)
                {
                    continue;
                }

                String name = parts[0].trim();
                String address = parts[1].trim();

                try
                {
                    int port = Integer.parseInt(parts[2].trim());

                    if (!name.isEmpty() && !address.isEmpty()
                            && port >= 0 && port <= 65535)
                    {
                        savedDevices.add(name + "|" + address + "|" + port);
                    }
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Invalid device port: " + line);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("Failed to load devices: " + e.getMessage());
        }

        return savedDevices;
    }

    public static synchronized boolean saveDevices(DeviceRegistry registry)
    {
        List<String> lines = new ArrayList<>();

        for (DeviceRegistry.Device device : registry.all())
        {
            lines.add(device.name + "|" + device.address + "|" + device.port);
        }

        Path temporaryFile = null;

        try
        {
            Files.createDirectories(FILE.toAbsolutePath().getParent());

            temporaryFile = Files.createTempFile(
                    FILE.toAbsolutePath().getParent(),
                    "devices-",
                    ".tmp"
            );

            Files.write(
                    temporaryFile,
                    lines,
                    StandardCharsets.UTF_8
            );

            Files.move(
                    temporaryFile,
                    FILE.toAbsolutePath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            return true;
        }
        catch (IOException e)
        {
            System.out.println("Failed to save devices: " + e.getMessage());
            return false;
        }
        finally
        {
            if (temporaryFile != null)
            {
                try
                {
                    Files.deleteIfExists(temporaryFile);
                }
                catch (IOException e)
                {
                    System.out.println("Failed to delete temporary file: " + e.getMessage());
                }
            }
        }
    }

    public static synchronized void loadIntoRegistry(DeviceRegistry registry)
    {
        List<String> savedDevices = loadDevices();

        for (String line : savedDevices)
        {
            String[] parts = line.split("\\|", -1);

            String name = parts[0];
            String address = parts[1];
            int port = Integer.parseInt(parts[2]);

            registry.add(name, address, port);
        }
    }
}
