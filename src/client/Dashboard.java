package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Arrays;

public class Dashboard {

    private JFrame frame;
    private JTable deviceTable;
    private DefaultTableModel tableModel;

    private JLabel totalLabel;
    private JLabel onlineLabel;
    private JLabel offlineLabel;

    private JTextArea alertArea;
    private JTextArea logArea;

    private JFrame logFrame;

    private NetWatchClient client;
    private AlertManager alertManager;

    public Dashboard() {

        frame = new JFrame("NetWatch - Network Monitoring System");
        frame.setSize(1100, 700);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {

                if (client != null) {
                    client.disconnect();
                }

                frame.dispose();
            }
        });

        JLabel title = new JLabel(
            "NetWatch - Network Monitoring Dashboard"
        );

        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(
            BorderFactory.createEmptyBorder(15, 20, 10, 20)
        );

        frame.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(
            new BorderLayout(10, 10)
        );

        contentPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 20, 20, 20)
        );

        JPanel statsPanel = new JPanel(
            new GridLayout(1, 3, 15, 10)
        );

        totalLabel = new JLabel("0", SwingConstants.CENTER);
        onlineLabel = new JLabel("0", SwingConstants.CENTER);
        offlineLabel = new JLabel("0", SwingConstants.CENTER);

        statsPanel.add(
            createStatCard("Total Devices", totalLabel)
        );

        statsPanel.add(
            createStatCard("Online Devices", onlineLabel)
        );

        statsPanel.add(
            createStatCard("Offline Devices", offlineLabel)
        );

        contentPanel.add(statsPanel, BorderLayout.NORTH);

        String[] columns = {
            "Device Name",
            "IP Address",
            "Status",
            "Response Time"
        };

        tableModel = new DefaultTableModel(columns, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        deviceTable = new JTable(tableModel);
        deviceTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(deviceTable);

        JPanel devicePanel = new JPanel(
            new BorderLayout(5, 10)
        );

        JLabel deviceTitle = new JLabel("Monitored Devices");
        deviceTitle.setFont(
            new Font("Arial", Font.BOLD, 18)
        );

        devicePanel.add(deviceTitle, BorderLayout.NORTH);
        devicePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        JButton addButton = new JButton("Add Device");
        JButton removeButton = new JButton("Remove Device");
        JButton logsButton = new JButton("View Logs");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(logsButton);

        devicePanel.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addDevice());
        removeButton.addActionListener(e -> removeDevice());
        logsButton.addActionListener(e -> viewLogs());

        JPanel alertPanel = new JPanel(new BorderLayout());

        alertPanel.setBorder(
            BorderFactory.createTitledBorder("Recent Alerts")
        );

        alertArea = new JTextArea(5, 30);
        alertArea.setEditable(false);
        alertArea.setText("Connecting to NetWatch server...\n");

        alertManager = new AlertManager(alertArea);

        alertPanel.add(
            new JScrollPane(alertArea),
            BorderLayout.CENTER
        );

        JPanel centerPanel = new JPanel(
            new BorderLayout(10, 15)
        );

        centerPanel.add(devicePanel, BorderLayout.CENTER);
        centerPanel.add(alertPanel, BorderLayout.SOUTH);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        frame.add(contentPanel, BorderLayout.CENTER);

        frame.setVisible(true);

        connectToServer();
    }

    private JPanel createStatCard(
            String heading, JLabel valueLabel) {

        JPanel panel = new JPanel(new GridLayout(2, 1));

        panel.setBorder(
            BorderFactory.createLineBorder(Color.GRAY)
        );

        panel.setBackground(Color.WHITE);

        JLabel headingLabel = new JLabel(
            heading, SwingConstants.CENTER
        );

        headingLabel.setFont(
            new Font("Arial", Font.BOLD, 16)
        );

        valueLabel.setFont(
            new Font("Arial", Font.BOLD, 25)
        );

        panel.add(headingLabel);
        panel.add(valueLabel);

        return panel;
    }

    private void connectToServer() {

        Thread connectionThread = new Thread(() -> {

            try {

                client = new NetWatchClient();

                client.connect();

                SwingUtilities.invokeLater(() -> {
                    alertArea.append(
                        "Connected to NetWatch server.\n"
                    );
                });

                client.startListening(message -> {

                    SwingUtilities.invokeLater(() -> {
                        handleServerMessage(message);
                    });

                });

            } catch (IOException e) {

                SwingUtilities.invokeLater(() -> {
                    alertArea.append(
                        "Connection failed: "
                        + e.getMessage() + "\n"
                    );
                });
            }

        });

        connectionThread.setName("NetWatch-Connection");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void handleServerMessage(String message) {

        String[] parts = message.split("\\|", -1);

        switch (parts[0]) {

            case "DEVICE":

                if (parts.length == 5) {

                    updateDevice(
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4]
                    );
                }

                break;

            case "ADDED":

                if (parts.length == 4) {

                    updateDevice(
                        parts[1],
                        parts[2],
                        "PENDING",
                        "-1"
                    );

                    alertArea.append(
                        parts[1] + " was added.\n"
                    );
                }

                break;

            case "REMOVED":

                if (parts.length == 3) {

                    removeDeviceFromTable(
                        parts[1],
                        parts[2]
                    );

                    alertArea.append(
                        parts[1] + " was removed.\n"
                    );
                }

                break;

            case "ALERT":

                if (parts.length >= 3) {

                    String deviceName = parts[1];

                    String alertMessage = String.join(
                        "|",
                        Arrays.copyOfRange(parts, 2, parts.length)
                    );

                    alertManager.handleAlert(
                        deviceName,
                        alertMessage
                    );
                }

                break;

            case "LOG":

                if (logArea != null) {

                    logArea.append(
                        message.substring(4) + "\n"
                    );

                    logArea.setCaretPosition(
                        logArea.getDocument().getLength()
                    );
                }

                break;

            case "CONNECTION":

                if (parts.length == 2 &&
                        parts[1].equals("RECONNECTED")) {

                    alertArea.append(
                        "Reconnected to NetWatch server.\n"
                    );

                    tableModel.setRowCount(0);
                    refreshStatistics();

                    if (logArea != null && logFrame != null
                            && logFrame.isVisible()) {
                        logArea.setText("Connection restored.\n");
                    }
                }

                break;

            case "ERROR": {

                String errorMessage = message.substring(6);

                alertArea.append(
                    "Error: " + errorMessage + "\n"
                );

                JOptionPane.showMessageDialog(
                    frame,
                    errorMessage,
                    "Device Error",
                    JOptionPane.ERROR_MESSAGE
                );

                if (logArea != null && logFrame != null
                        && logFrame.isVisible()) {

                    logArea.append(
                        "ERROR: " + errorMessage + "\n"
                    );
                }

                break;
            }

            case "SUCCESS":

                if (message.equals("SUCCESS|Logs retrieved")) {

                    if (logArea != null) {
                        logArea.append(
                            "\nLogs retrieved successfully.\n"
                        );
                    }

                } else {

                    alertArea.append(message + "\n");
                }

                break;

            default:
                break;
        }
    }

    private void viewLogs() {

        if (client == null || !client.isConnected()) {

            JOptionPane.showMessageDialog(
                frame,
                "Not connected to server."
            );

            return;
        }

        if (logFrame == null) {

            logFrame = new JFrame("NetWatch - Log Viewer");
            logFrame.setSize(800, 500);
            logFrame.setLocationRelativeTo(frame);
            logFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

            logArea = new JTextArea();
            logArea.setEditable(false);

            logArea.setFont(
                new Font("Monospaced", Font.PLAIN, 13)
            );

            logArea.setMargin(new Insets(10, 10, 10, 10));

            JScrollPane logScrollPane = new JScrollPane(logArea);

            JButton refreshButton = new JButton("Refresh Logs");

            refreshButton.addActionListener(e -> viewLogs());

            JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
            );

            buttonPanel.add(refreshButton);

            logFrame.setLayout(new BorderLayout());
            logFrame.add(logScrollPane, BorderLayout.CENTER);
            logFrame.add(buttonPanel, BorderLayout.SOUTH);
        }

        logArea.setText("Loading logs...\n");

        logFrame.setLocationRelativeTo(frame);
        logFrame.setVisible(true);

        try {

            client.sendCommand("GET_LOGS");

        } catch (IOException e) {

            logArea.append(
                "Failed to retrieve logs: " + e.getMessage() + "\n"
            );
        }
    }

    private void updateDevice(
            String name,
            String address,
            String status,
            String time) {

        int row = findDevice(name, address);

        String responseTime;

        if (time.equals("-1")
                || status.equalsIgnoreCase("PENDING")
                || status.equalsIgnoreCase("UNKNOWN")) {

            responseTime = "N/A";

        } else {

            responseTime = time + " ms";
        }

        Object[] data = {
            name,
            address,
            status,
            responseTime
        };

        if (row == -1) {

            tableModel.addRow(data);

        } else {

            for (int column = 0; column < data.length; column++) {

                tableModel.setValueAt(
                    data[column],
                    row,
                    column
                );
            }
        }

        refreshStatistics();
    }

    private int findDevice(String name, String address) {

        for (int i = 0; i < tableModel.getRowCount(); i++) {

            String existingName =
                tableModel.getValueAt(i, 0).toString();

            String existingAddress =
                tableModel.getValueAt(i, 1).toString();

            if (existingName.equals(name)
                    && existingAddress.equals(address)) {

                return i;
            }
        }

        return -1;
    }

    private void removeDeviceFromTable(
            String name, String address) {

        int row = findDevice(name, address);

        if (row != -1) {
            tableModel.removeRow(row);
        }

        refreshStatistics();
    }

    private void addDevice() {

        if (client == null || !client.isConnected()) {

            JOptionPane.showMessageDialog(
                frame,
                "Not connected to server."
            );

            return;
        }

        String name = JOptionPane.showInputDialog(
            frame,
            "Enter Device Name:"
        );

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String ip = JOptionPane.showInputDialog(
            frame,
            "Enter IP Address:"
        );

        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        String portText = JOptionPane.showInputDialog(
            frame,
            "Enter Port Number (0 for ping):"
        );

        if (portText == null || portText.trim().isEmpty()) {
            return;
        }

        try {

            int port = Integer.parseInt(portText.trim());

            if (port < 0 || port > 65535) {
                throw new NumberFormatException();
            }

            client.sendCommand(
                "ADD_DEVICE|"
                + name.trim() + "|"
                + ip.trim() + "|"
                + port
            );

        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                frame,
                "Please enter a valid port number."
            );

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                frame,
                "Failed to send command: " + e.getMessage()
            );
        }
    }

    private void removeDevice() {

        int selectedRow = deviceTable.getSelectedRow();

        if (selectedRow == -1) {

            JOptionPane.showMessageDialog(
                frame,
                "Please select a device first."
            );

            return;
        }

        if (client == null || !client.isConnected()) {

            JOptionPane.showMessageDialog(
                frame,
                "Not connected to server."
            );

            return;
        }

        int modelRow = deviceTable.convertRowIndexToModel(
            selectedRow
        );

        String name = tableModel.getValueAt(
            modelRow, 0
        ).toString();

        String address = tableModel.getValueAt(
            modelRow, 1
        ).toString();

        try {

            client.sendCommand(
                "REMOVE_DEVICE|" + name + "|" + address
            );

        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                frame,
                "Failed to send command: " + e.getMessage()
            );
        }
    }

    private void refreshStatistics() {

        int total = tableModel.getRowCount();

        int online = 0;
        int offline = 0;

        for (int i = 0; i < total; i++) {

            String status =
                tableModel.getValueAt(i, 2).toString();

            if (status.equalsIgnoreCase("ONLINE")) {

                online++;

            } else if (status.equalsIgnoreCase("OFFLINE")) {

                offline++;
            }
        }

        totalLabel.setText(String.valueOf(total));
        onlineLabel.setText(String.valueOf(online));
        offlineLabel.setText(String.valueOf(offline));
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
            () -> new Dashboard()
        );
    }
}
