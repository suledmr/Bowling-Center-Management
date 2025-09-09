import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;

public class BowlingMainFrame extends JFrame {

    private static final Color BUSY_LANE_COLOR = new Color(216, 112, 112); // Red
    private static final Color AVAILABLE_LANE_COLOR = new Color(136, 201, 153); // Green

    private final JButton[] pinButtons = new JButton[5];
    private final JLabel[] pinLabels = new JLabel[5];

    public BowlingMainFrame() {
        setTitle("Welcome to Bowling Center - Lane Status");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        int FRAME_WIDTH = 800;
        int FRAME_HEIGHT = 800;

        BackgroundPanel backgroundPanel = new BackgroundPanel(
                "C:\\Users\\HP\\Desktop\\bowling_database\\mainPageBackground.jpg",
                FRAME_WIDTH,
                FRAME_HEIGHT
        );
        backgroundPanel.setLayout(new GridBagLayout());

        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        buttonPanel.setOpaque(false);

       
        Set<Integer> busyLanes = getBusyLanesAt(LocalDateTime.now());

        for (int i = 0; i < 5; i++) {
            int laneId = i + 1;
            boolean isBusy = busyLanes.contains(laneId);

            JPanel lanePanel = createLanePanel(laneId, isBusy);
            buttonPanel.add(lanePanel);

            updateButtonImage(i, "C:\\Users\\HP\\Desktop\\bowling_database\\bowlingPins.png");
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("BOWLING CENTER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(40, 10, 30, 10));

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        backgroundPanel.add(mainPanel, gbc);
        setContentPane(backgroundPanel);
    }

    private JPanel createLanePanel(int laneId, boolean isBusy) {
        JPanel lanePanel = new JPanel(new BorderLayout());
        lanePanel.setBorder(BorderFactory.createLoweredBevelBorder());
        lanePanel.setOpaque(false);

        JButton btn = new JButton();
        btn.setPreferredSize(new Dimension(125, 100));
        btn.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 150), 2));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setRolloverEnabled(false);
        btn.setPressedIcon(null);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        btn.addActionListener(e -> {
        LaneReservationFrame reservationFrame = new LaneReservationFrame(laneId) {
                @Override
                public void dispose() {
                    super.dispose();
                    BowlingMainFrame.this.dispose();
                    new BowlingMainFrame().setVisible(true);
                }
            };
            reservationFrame.setVisible(true);

            if (isBusy) {
                new LaneInfoFrame(laneId).setVisible(true);
            }
        });


        pinButtons[laneId-1] = btn;

        JLabel label = new JLabel(String.valueOf(laneId), SwingConstants.CENTER);
        label.setForeground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 18));
        label.setOpaque(true);
        label.setBackground(isBusy ? BUSY_LANE_COLOR : AVAILABLE_LANE_COLOR);

        pinLabels[laneId-1] = label;

        lanePanel.add(btn, BorderLayout.CENTER);
        lanePanel.add(label, BorderLayout.SOUTH);

        return lanePanel;
    }

    public void updateButtonImage(int index, String imagePath) {
        if (index >= 0 && index < pinButtons.length) {
            JButton button = pinButtons[index];
            ImageIcon icon = new ImageIcon(imagePath);
            int btnWidth = button.getPreferredSize().width;
            int btnHeight = button.getPreferredSize().height;
            Image scaled = getScaledImage(icon.getImage(), btnWidth, btnHeight);
            button.setIcon(new ImageIcon(scaled));
        }
    }

    private Image getScaledImage(Image srcImg, int maxWidth, int maxHeight) {
        int imgWidth = srcImg.getWidth(null);
        int imgHeight = srcImg.getHeight(null);
        double widthRatio = (double) maxWidth / imgWidth;
        double heightRatio = (double) maxHeight / imgHeight;
        double scale = Math.min(widthRatio, heightRatio);
        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);
        return srcImg.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private Set<Integer> getBusyLanesAt(LocalDateTime currentTime) {
        Set<Integer> busyLanes = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bowling_schema", "root", "Ymuh090138");
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT DISTINCT r.lane_id FROM reservations r " +
                     "JOIN dates d ON r.date_id = d.date_id " +
                     "WHERE ? BETWEEN d.start_time AND d.end_time")) {

            stmt.setTimestamp(1, Timestamp.valueOf(currentTime));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    busyLanes.add(rs.getInt("lane_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error checking lane status: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        return busyLanes;
    }

    class BackgroundPanel extends JPanel {
        private final Image scaledImage;
        private final int panelWidth;
        private final int panelHeight;

        public BackgroundPanel(String imagePath, int panelWidth, int panelHeight) {
            this.panelWidth = panelWidth;
            this.panelHeight = panelHeight;
            ImageIcon icon = new ImageIcon(imagePath);
            Image original = icon.getImage();
            this.scaledImage = getScaledImage(original, panelWidth, panelHeight);
            setPreferredSize(new Dimension(panelWidth, panelHeight));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (scaledImage != null) {
                int x = (panelWidth - scaledImage.getWidth(null)) / 2;
                int y = (panelHeight - scaledImage.getHeight(null)) / 2;
                g.drawImage(scaledImage, x, y, this);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BowlingMainFrame frame = new BowlingMainFrame();
            frame.setVisible(true);
        });
    }
}
