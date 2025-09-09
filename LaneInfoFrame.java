import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

public class LaneInfoFrame extends JFrame {

    private static final Color BACKGROUND_COLOR = new Color(255, 255, 255, 200);

    private int laneId;

    public LaneInfoFrame(int laneId) {
        this.laneId = laneId;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Lane " + laneId + " Schedule");
        setSize(500, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setContentPane(new BackgroundPanel("C:\\Users\\HP\\Desktop\\bowling_database\\pattern.jpg"));
        setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("Schedule for Lane " + laneId, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));
        add(titleLabel, BorderLayout.NORTH);

        JPanel schedulePanel = createSchedulePanel();
        add(schedulePanel, BorderLayout.CENTER);
    }

    private JPanel createSchedulePanel() {
        JPanel schedulePanel = new JPanel(new GridLayout(3, 1, 10, 10));
        schedulePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        schedulePanel.setBorder(BorderFactory.createLoweredBevelBorder());
        schedulePanel.setOpaque(false);

        schedulePanel.add(createDayPanel("Yesterday", LocalDate.now().minusDays(1)));
        schedulePanel.add(createDayPanel("Today", LocalDate.now()));
        schedulePanel.add(createDayPanel("Tomorrow", LocalDate.now().plusDays(1)));

        return schedulePanel;
    }

    private JPanel createDayPanel(String dayLabel, LocalDate date) {
        JPanel dayPanel = new JPanel(new BorderLayout());
        dayPanel.setBorder(BorderFactory.createTitledBorder(dayLabel));
        dayPanel.setBorder(BorderFactory.createRaisedBevelBorder());
        dayPanel.setOpaque(false);

        String[] columnNames = {"Start Time", "End Time", "Group Size", "Amount", "Payment", "Res. ID"};
        List<String[]> data = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bowling_schema", "root", "Ymuh090138");
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT d.start_time, d.end_time, r.payment_type, r.amount, g.group_size, r.reservation_id " +
                    "FROM reservations r " +
                    "JOIN dates d ON r.date_id = d.date_id " +
                    "JOIN gamegroups g ON r.group_id = g.group_id " +
                    "WHERE r.lane_id = ? AND DATE(d.start_time) = ? " +
                    "ORDER BY d.start_time")) {

            stmt.setInt(1, laneId);
            stmt.setDate(2, Date.valueOf(date));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                data.add(new String[]{
                    rs.getTime("start_time").toString(),
                    rs.getTime("end_time").toString(),
                    String.valueOf(rs.getInt("group_size")),
                    String.format("₺%d", (int) rs.getDouble("amount")),
                    rs.getString("payment_type"),
                    rs.getString("reservation_id")
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
            data.add(new String[]{"Error", "", "", "", "", ""});
        }

        String[][] tableData = data.toArray(new String[0][]);
        JTable table = new JTable(tableData, columnNames);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, null); 
        table.setFont(new Font("Monospaced", Font.PLAIN, 14));
        table.setRowHeight(25);
        table.setBackground(BACKGROUND_COLOR);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton deleteButton = new JButton("Delete Selected Reservation");
        deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
        deleteButton.addActionListener(e -> {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow >= 0) {
                    String reservationId = (String) table.getValueAt(selectedRow, 5);

                    int confirm = JOptionPane.showConfirmDialog(
                            this,
                            "Delete reservation ID " + reservationId + "?",
                            "Confirm Deletion",
                            JOptionPane.YES_NO_OPTION);

                    if (confirm == JOptionPane.YES_OPTION) {
                        try (Connection conn = DriverManager.getConnection(
                                    "jdbc:mysql://localhost:3306/bowling_schema", "root", "Ymuh090138");
                            PreparedStatement getInfo = conn.prepareStatement(
                                "SELECT lane_id, group_id, game_id, date_id FROM reservations WHERE reservation_id = ?");
                            PreparedStatement delReservation = conn.prepareStatement(
                                "DELETE FROM reservations WHERE reservation_id = ?");
                            PreparedStatement delGroup = conn.prepareStatement(
                                "DELETE FROM gamegroups WHERE group_id = ?");
                            PreparedStatement delGame = conn.prepareStatement(
                                "DELETE FROM games WHERE game_id = ?");
                            PreparedStatement delDate = conn.prepareStatement(
                                "DELETE FROM dates WHERE date_id = ?");
                            PreparedStatement check = conn.prepareStatement(
                                "SELECT COUNT(*) FROM reservations r JOIN dates d ON r.date_id = d.date_id " +
                                "WHERE r.lane_id = ? AND ? BETWEEN d.start_time AND d.end_time");
                            PreparedStatement update = conn.prepareStatement(
                                "UPDATE lanes SET status = 'free' WHERE lane_id = ?")) {

                            int resId = Integer.parseInt(reservationId);
                            int laneToCheck = -1, groupId = -1, gameId = -1, dateId = -1;

                            getInfo.setInt(1, resId);
                            ResultSet rs = getInfo.executeQuery();
                            if (rs.next()) {
                                laneToCheck = rs.getInt("lane_id");
                                groupId = rs.getInt("group_id");
                                gameId = rs.getInt("game_id");
                                dateId = rs.getInt("date_id");
                            }

                            delReservation.setInt(1, resId);
                            int affected = delReservation.executeUpdate();

                            if (affected > 0) {
                                delGroup.setInt(1, groupId);
                                delGroup.executeUpdate();

                                delGame.setInt(1, gameId);
                                delGame.executeUpdate();

                                delDate.setInt(1, dateId);
                                delDate.executeUpdate();

                                check.setInt(1, laneToCheck);
                                check.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                ResultSet checkRs = check.executeQuery();
                                if (checkRs.next() && checkRs.getInt(1) == 0) {
                                    update.setInt(1, laneToCheck);
                                    update.executeUpdate();
                                }

                                JOptionPane.showMessageDialog(this, "Reservation deleted.");
                                dispose();
                                new LaneInfoFrame(laneId).setVisible(true);
                            } else {
                                JOptionPane.showMessageDialog(this, "Reservation not found.");
                            }

                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "Error deleting reservation.");
                            ex.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a row to delete.");
                }
            });


    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    buttonPanel.setOpaque(false);
    buttonPanel.add(deleteButton);

    dayPanel.add(scrollPane, BorderLayout.CENTER);
    dayPanel.add(buttonPanel, BorderLayout.SOUTH);
    return dayPanel;
}


    private List<String> getReservationsFromDatabase(LocalDate date) {
        List<String> reservations = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bowling_schema", "root", "Ymuh090138");
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT d.start_time, d.end_time, r.payment_type, r.amount, g.group_size, r.reservation_id " +
                     "FROM reservations r " +
                     "JOIN dates d ON r.date_id = d.date_id " +
                     "JOIN gamegroups g ON r.group_id = g.group_id " +
                     "WHERE r.lane_id = ? AND DATE(d.start_time) = ?" 
                     + "ORDER BY d.start_time" )) {

            stmt.setInt(1, laneId);
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String entry = String.format("%s - %s | Group Size: %d| ₺%d | %s | Res. ID: %s",
                            rs.getTime("start_time"),
                            rs.getTime("end_time"),
                            rs.getInt("group_size"),
                            (int) rs.getDouble("amount"),
                            rs.getString("payment_type"),
                            rs.getString("reservation_id"));
                    reservations.add(entry);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            reservations.add("Error connecting to database");
        }

        return reservations;
    }

    static class BackgroundPanel extends JPanel {
        private BufferedImage backgroundImage;

        public BackgroundPanel(String imagePath) {
            try {
                backgroundImage = ImageIO.read(new File(imagePath));
            } catch (Exception e) {
                System.err.println("Error loading background image: " + imagePath);
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
