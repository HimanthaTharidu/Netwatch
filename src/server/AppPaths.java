package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Resolves persistent files from one stable NetWatch home directory. */
final class AppPaths
{
    private static final Path HOME_DIRECTORY = resolveHomeDirectory();
    private static final Path DEVICES_FILE = resolveFile(
            "devices.txt",
            Path.of("data", "devices.txt")
    );
    private static final Path LOG_FILE = resolveFile(
            "network.log",
            Path.of("logs", "network.log")
    );

    private AppPaths()
    {
    }

    static Path devicesFile()
    {
        return DEVICES_FILE;
    }

    static Path logFile()
    {
        return LOG_FILE;
    }

    private static Path resolveHomeDirectory()
    {
        String configuredHome = System.getProperty("netwatch.home");
        Path home = configuredHome == null || configuredHome.isBlank()
                ? Path.of(System.getProperty("user.home"), ".netwatch")
                : Path.of(configuredHome);

        return home.toAbsolutePath().normalize();
    }

    private static Path resolveFile(String fileName, Path legacyPath)
    {
        Path target = HOME_DIRECTORY.resolve(fileName);
        Path legacy = legacyPath.toAbsolutePath().normalize();

        if (Files.exists(target) || !Files.isRegularFile(legacy))
        {
            return target;
        }

        try
        {
            Files.createDirectories(HOME_DIRECTORY);
            Files.copy(legacy, target, StandardCopyOption.COPY_ATTRIBUTES);
            System.out.println(
                    "Migrated NetWatch data from " + legacy + " to " + target
            );
        }
        catch (IOException e)
        {
            System.err.println(
                    "Could not migrate NetWatch data from " + legacy
                            + " to " + target + ": " + e.getMessage()
            );
        }

        return target;
    }
}