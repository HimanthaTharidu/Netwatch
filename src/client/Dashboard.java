package client;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Dashboard {

    private JFrame frame;
    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private JLabel onlineLabel;
    private JLabel offlineLabel;

    public Dashboard() {

        frame = new JFrame("NetWatch - Network Monitoring System");
        frame.setSize(1100, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JLabel title = new JLabel("NetWatch - Network Monitoring Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        frame.add(title, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 10));

        totalLabel = new JLabel("0", SwingConstants.CENTER);
        onlineLabel = new JLabel("0", SwingConstants.CENTER);
        offlineLabel = new JLabel("0", SwingConstants.CENTER);

        statsPanel.add(createStatCard("Total Devices", totalLabel));
        statsPanel.add(createStatCard("Online Devices", onlineLabel));
        statsPanel.add(createStatCard("Offline Devices", offlineLabel));

        contentPanel.add(statsPanel, BorderLayout.NORTH);

        String[] columns = {
            "Device Name", "IP Address", "Status", "Response Time"
        };

        tableModel = new DefaultTableModel(columns, 0) {

    
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableModel.addRow(new Object[]{
            "Google DNS", "8.8.8.8", "ONLINE", "20 ms"
        });

        tableModel.addRow(new Object[]{
            "Cloudflare DNS", "1.1.1.1", "ONLINE", "15 ms"
        });

        tableModel.addRow(new Object[]{
            "Test Server", "192.168.1.20", "OFFLINE", "N/A"
        });

        refreshStatistics();
        deviceTable = new JTable(tableModel);
        deviceTable.setRowHeight(30);

        JScrollPane scrollPane = new JScrollPane(deviceTable);

        JPanel devicePanel = new JPanel(new BorderLayout(5, 10));

        JLabel deviceTitle = new JLabel("Monitored Devices");
        deviceTitle.setFont(new Font("Arial", Font.BOLD, 18));

        devicePanel.add(deviceTitle, BorderLayout.NORTH);
        devicePanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton addButton = new JButton("Add Device");
        JButton removeButton = new JButton("Remove Device");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);

        devicePanel.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addDevice());

        removeButton.addActionListener(e -> removeDevice());

        JPanel alertPanel = new JPanel(new BorderLayout());
        alertPanel.setBorder(BorderFactory.createTitledBorder("Recent Alerts"));

        JTextArea alertArea = new JTextArea(5, 30);
        alertArea.setEditable(false);
        alertArea.setText(
            "System started successfully.\n" +
            "Test Server is currently OFFLINE."
        );

        alertPanel.add(new JScrollPane(alertArea), BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));
        centerPanel.add(devicePanel, BorderLayout.CENTER);
        centerPanel.add(alertPanel, BorderLayout.SOUTH);

        contentPanel.add(centerPanel, BorderLayout.CENTER);

        frame.add(contentPanel, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JPanel createStatCard(String heading, JLabel valueLabel) {

        JPanel panel = new JPanel(new GridLayout(2, 1));

        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        panel.setBackground(Color.WHITE);

        JLabel headingLabel = new JLabel(heading, SwingConstants.CENTER);

        headingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        valueLabel.setFont(new Font("Arial", Font.BOLD, 25));

        panel.add(headingLabel);
        panel.add(valueLabel);

        return panel;
    }

    private void addDevice() {

        String name = JOptionPane.showInputDialog(
            frame, "Enter Device Name:"
        );

        if (name == null || name.trim().isEmpty()) {
            return;
        }

        String ip = JOptionPane.showInputDialog(
            frame, "Enter IP Address:"
        );

        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        tableModel.addRow(new Object[]{
            name.trim(), ip.trim(), "PENDING", "N/A"
        });
        refreshStatistics();
    }

    private void removeDevice() {

        int selectedRow = deviceTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                frame, "Please select a device first."
            );
            return;
        }

        tableModel.removeRow(selectedRow);
        refreshStatistics();
    }
    private void refreshStatistics() {

        int total = tableModel.getRowCount();

        int online = 0;
        int offline = 0;

        for (int i = 0; i < total; i++) {

            String status = tableModel.getValueAt(i, 2).toString();

            if (status.equalsIgnoreCase("ONLINE")) {
                online++;
            }
            else if (status.equalsIgnoreCase("OFFLINE")) {
                offline++;
            }
        }

        totalLabel.setText(String.valueOf(total));
        onlineLabel.setText(String.valueOf(online));
        offlineLabel.setText(String.valueOf(offline));
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> new Dashboard());
    }
}
