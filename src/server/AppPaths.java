package server;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Resolves persistent files from one stable NetWatch data directory. */
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

        if (configuredHome != null && !configuredHome.isBlank())
        {
            return Path.of(configuredHome).toAbsolutePath().normalize();
        }

        Path projectData = findProjectDataDirectory(
                Path.of("").toAbsolutePath().normalize()
        );

        if (projectData == null)
        {
            try
            {
                Path classesLocation = Path.of(
                        AppPaths.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI()
                ).toAbsolutePath().normalize();

                projectData = findProjectDataDirectory(classesLocation);
            }
            catch (URISyntaxException | RuntimeException e)
            {
                // Fall back to the user's stable application data folder.
            }
        }

        if (projectData != null)
        {
            return projectData;
        }

        return Path.of(System.getProperty("user.home"), ".netwatch")
                .toAbsolutePath().normalize();
    }

    private static Path findProjectDataDirectory(Path start)
    {
        Path current = Files.isDirectory(start) ? start : start.getParent();

        while (current != null)
        {
            Path projectData = current.resolve("src").resolve("data");

            if (Files.isDirectory(current.resolve("src").resolve("server")))
            {
                return projectData;
            }

            if (Files.isDirectory(current.resolve("server"))
                    && Files.isDirectory(current.resolve("data")))
            {
                return current.resolve("data");
            }

            current = current.getParent();
        }

        return null;
    }

    private static Path resolveFile(String fileName, Path legacyPath)
    {
        Path target = HOME_DIRECTORY.resolve(fileName);

        try
        {
            if (Files.isRegularFile(target) && Files.size(target) > 0)
            {
                return target;
            }

            Path previousHomeFile = Path.of(
                    System.getProperty("user.home"), ".netwatch", fileName
            ).toAbsolutePath().normalize();
            Path legacyFile = legacyPath.toAbsolutePath().normalize();

            Path[] migrationSources = { previousHomeFile, legacyFile };

            for (Path source : migrationSources)
            {
                if (!source.equals(target)
                        && Files.isRegularFile(source)
                        && Files.size(source) > 0)
                {
                    Files.createDirectories(HOME_DIRECTORY);
                    Files.copy(
                            source,
                            target,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES
                    );
                    System.out.println(
                            "Migrated NetWatch data from " + source
                                    + " to " + target
                    );
                    break;
                }
            }
        }
        catch (IOException e)
        {
            System.err.println(
                    "Could not migrate NetWatch data to " + target
                            + ": " + e.getMessage()
            );
        }

        return target;
    }
}