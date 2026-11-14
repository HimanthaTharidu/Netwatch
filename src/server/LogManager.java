
package server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogManager
{
    private static final Path LOG_FILE = AppPaths.logFile();

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String type, String message)
    {
        String timestamp = LocalDateTime.now().format(FORMAT);

        String entry = timestamp + " | " + type + " | " + message;

        System.out.println(entry);

        try
        {
            Files.createDirectories(LOG_FILE.toAbsolutePath().getParent());

            Files.writeString(
                    LOG_FILE,
                    entry + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        }
        catch (IOException e)
        {
            System.out.println("Failed to write log: " + e.getMessage());
        }
    }

    public static synchronized List<String> getRecentLogs(int limit)
    {
        List<String> recentLogs = new ArrayList<>();

        if (limit <= 0 || !Files.exists(LOG_FILE))
        {
            return recentLogs;
        }

        try
        {
            List<String> allLogs = Files.readAllLines(
                    LOG_FILE,
                    StandardCharsets.UTF_8
            );

            int start = Math.max(0, allLogs.size() - limit);

            recentLogs.addAll(allLogs.subList(start, allLogs.size()));
        }
        catch (IOException e)
        {
            System.out.println("Failed to read logs: " + e.getMessage());
        }

        return recentLogs;
    }
}
