import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import javax.swing.*;

public class LaneReservationFrame extends JFrame {

    private static final String[] DURATION_OPTIONS = {"-","00:30:00", "01:00:00", "01:30:00"};
    private static final double[] PRICES = {0, 300, 600, 900};

    private int laneId;
    private Connection connection;

    public LaneReservationFrame(int laneId) {
        this.laneId = laneId;
        initializeDatabaseConnection();
        initializeUI();
    }

    private void initializeDatabaseConnection() {
        try {
            connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bowling_schema", "root", "Ymuh090138");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Database connection error!", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void initializeUI() {
        setTitle("Lane " + laneId + " - New Reservation");
        setSize(550, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setContentPane(new BackgroundPanel("C:\\Users\\HP\\Desktop\\bowling_database\\pattern.jpg"));
        setLayout(new BorderLayout(10, 10));

        add(createHeaderLabel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createReserveButton(), BorderLayout.SOUTH);
    }

    private JLabel createHeaderLabel() {
        JLabel headerLabel = new JLabel("Reserve Lane " + laneId, SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 20));
        headerLabel.setForeground(new Color(50, 50, 50));
        return headerLabel;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        JPanel formWrapper = new JPanel(new GridBagLayout());
        formWrapper.setBorder(BorderFactory.createRaisedBevelBorder());
        formWrapper.setOpaque(true);
        formWrapper.setBackground(new Color(255, 255, 255, 180));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField groupMembersField = new JTextField(15);
        JTextField customerNameField = new JTextField(15);

        String exampleDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        JTextField startTimeField = new JTextField(exampleDateTime, 15);
        startTimeField.setToolTipText("Format: yyyy-MM-dd HH:mm (e.g., " + exampleDateTime + ")");

        JComboBox<String> durationCombo = new JComboBox<>(DURATION_OPTIONS);
        JCheckBox cashCheck = new JCheckBox("Cash");
        JCheckBox cardCheck = new JCheckBox("Credit Card");
        JLabel calculatedPriceLabel = new JLabel("₺0.00");
        calculatedPriceLabel.setFont(new Font("Arial", Font.BOLD, 16));

        durationCombo.addActionListener(e -> {
            int selectedIndex = durationCombo.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < PRICES.length) {
                calculatedPriceLabel.setText(String.format("₺%.0f", PRICES[selectedIndex]));
            }
        });

        int row = 0;
        addFormRow(formWrapper, gbc, row++, "Group Members (comma-separated):", groupMembersField);
        addFormRow(formWrapper, gbc, row++, "Customer Name:", customerNameField);
        addFormRow(formWrapper, gbc, row++, "Start Time (yyyy-MM-dd HH:mm):", startTimeField);
        addFormRow(formWrapper, gbc, row++, "Game Duration:", durationCombo);

        gbc.gridx = 0;
        gbc.gridy = row;
        formWrapper.add(new JLabel("Payment Method:"), gbc);
        gbc.gridx = 1;
        JPanel paymentPanel = new JPanel();
        paymentPanel.setOpaque(false);
        paymentPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        paymentPanel.add(cashCheck);
        paymentPanel.add(cardCheck);
        formWrapper.add(paymentPanel, gbc);
        row++;

        addFormRow(formWrapper, gbc, row++, "Calculated Price:", calculatedPriceLabel);

        JButton laneInfoButton = new JButton("Get Lane Info");
        laneInfoButton.setBorder(BorderFactory.createRaisedBevelBorder());
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        formWrapper.add(laneInfoButton, gbc);
        laneInfoButton.addActionListener(e -> new LaneInfoFrame(laneId).setVisible(true));

        mainPanel.add(formWrapper);
        return mainPanel;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(field, gbc);
    }

    private JButton createReserveButton() {
        JButton reserveButton = new JButton("Save Reservation");
        reserveButton.setBorder(BorderFactory.createRaisedBevelBorder());
        reserveButton.addActionListener(e -> saveReservation());
        return reserveButton;
    }

    private void saveReservation() {
        JPanel formWrapper = (JPanel)((JPanel)getContentPane().getComponent(1)).getComponent(0);

        JTextField groupMembersField = (JTextField)formWrapper.getComponent(1);
        JTextField customerNameField = (JTextField)formWrapper.getComponent(3);
        JTextField startTimeField = (JTextField)formWrapper.getComponent(5);
        JComboBox<String> durationCombo = (JComboBox<String>)formWrapper.getComponent(7);
        JCheckBox cashCheck = (JCheckBox)((JPanel)formWrapper.getComponent(9)).getComponent(0);
        JCheckBox cardCheck = (JCheckBox)((JPanel)formWrapper.getComponent(9)).getComponent(1);
        JLabel calculatedPriceLabel = (JLabel)formWrapper.getComponent(11);

        if (groupMembersField.getText().trim().isEmpty() || 
            customerNameField.getText().trim().isEmpty() || 
            startTimeField.getText().trim().isEmpty() || 
            (!cashCheck.isSelected() && !cardCheck.isSelected())) {

            JOptionPane.showMessageDialog(this, 
                "Please fill all fields and select at least one payment method!", 
                "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            LocalDateTime start;
            try {
                start = LocalDateTime.parse(
                    startTimeField.getText().trim(), 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, 
                    "Invalid date/time format! Please use yyyy-MM-dd HH:mm format.", 
                    "Invalid Format", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String selectedDuration = (String) durationCombo.getSelectedItem();
            String[] parts = Objects.requireNonNull(selectedDuration).split(":");
            int totalMinutes = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            LocalDateTime end = start.plusMinutes(totalMinutes);

            String checkSql = "SELECT COUNT(*) FROM reservations r " +
                            "JOIN dates d ON r.date_id = d.date_id " +
                            "WHERE r.lane_id = ? AND (d.start_time < ? AND d.end_time > ?)";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, laneId);
                checkStmt.setTimestamp(2, Timestamp.valueOf(end));
                checkStmt.setTimestamp(3, Timestamp.valueOf(start));
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, 
                        "This lane is already reserved in the selected time range.",
                        "Lane Busy", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            double price = PRICES[durationCombo.getSelectedIndex()];
            String paymentType = cashCheck.isSelected() && cardCheck.isSelected() ? "Cash + Card" :
                            cashCheck.isSelected() ? "Cash" : "Credit Card";

            saveReservationToDatabase(
                groupMembersField.getText().trim(),
                customerNameField.getText().trim(),
                start, end, totalMinutes, paymentType, price);

            JOptionPane.showMessageDialog(this,
                String.format("Reservation saved!\nCustomer: %s\nGroup: %s\nStart: %s\nEnd: %s\nPrice: ₺%.0f\nPaid with: %s",
                    customerNameField.getText().trim(),
                    groupMembersField.getText().trim(),
                    start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    price, paymentType),
                "Success", JOptionPane.INFORMATION_MESSAGE);

            dispose();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, 
                "Error processing reservation: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }


    private void saveReservationToDatabase(String groupMembers, String customerName, 
            LocalDateTime start, LocalDateTime end, int durationMinutes, 
            String paymentType, double amount) throws SQLException {

        int groupId, gameId, dateId;

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO gamegroups (group_members) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, groupMembers);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            groupId = rs.getInt(1);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO games (score_result) VALUES ('')", Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            gameId = rs.getInt(1);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO dates (start_time) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            dateId = rs.getInt(1);
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO reservations (customer_name, payment_type, amount, duration, group_id, game_id, lane_id, date_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, customerName);
            ps.setString(2, paymentType);
            ps.setDouble(3, amount);
            ps.setTime(4, Time.valueOf(String.format("%02d:%02d:00", durationMinutes / 60, durationMinutes % 60)));
            ps.setInt(5, groupId);
            ps.setInt(6, gameId);
            ps.setInt(7, laneId);
            ps.setInt(8, dateId);
            ps.executeUpdate();
        }
    }

    static class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel(String imagePath) {
            try {
                backgroundImage = new ImageIcon(imagePath).getImage();
            } catch (Exception e) {
                System.err.println("Background image not found: " + imagePath);
                backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}