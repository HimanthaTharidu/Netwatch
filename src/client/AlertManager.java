package client;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AlertManager {

    private final JTextArea alertArea;

    private final DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public AlertManager(JTextArea alertArea) {
        this.alertArea = alertArea;
    }

    public void handleAlert(String deviceName, String message) {

        String time = LocalTime.now().format(timeFormatter);

        String alert = "[" + time + "] "
                + deviceName + ": " + message;

        SwingUtilities.invokeLater(() -> {
            alertArea.append(alert + "\n");
            alertArea.setCaretPosition(
                alertArea.getDocument().getLength()
            );
        });
    }
}
