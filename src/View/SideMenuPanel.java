package View;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
// import java.awt.event.MouseAdapter; // Không dùng trong bản sửa này
// import java.awt.event.MouseEvent;  // Không dùng trong bản sửa này
import java.util.function.Consumer;

public class SideMenuPanel extends JPanel {
    private boolean isMenuExpanded = true;
    private final int EXPANDED_WIDTH = 240;
    private final int COLLAPSED_WIDTH = 70;

    private JPanel menuItemsPanel;
    private JButton toggleMenuButton;
    private JLabel appTitleLabel;
    private JLabel appLogoLabel;
    private JButton selectedButton = null;

    private final Consumer<String> navigationCallback;

    // Animation Timer
    private Timer animationTimer;
    private final int ANIMATION_DURATION = 200; // milliseconds
    private final int ANIMATION_STEPS = 20;     // Number of steps for the animation

    public SideMenuPanel(Consumer<String> navigationCallback) {
        this.navigationCallback = navigationCallback;
        setLayout(new BorderLayout(0,0));
        updateThemeColors();
        setPreferredSize(new Dimension(EXPANDED_WIDTH, getHeight()));

        initComponents();
        updateMenuItemsUI(); 
    }

    private void initComponents() {
        JPanel topSectionPanel = new JPanel(new BorderLayout(0,0));
        topSectionPanel.setBorder(new EmptyBorder(15, 5, 15, 5));
        topSectionPanel.setOpaque(false);

        appLogoLabel = new JLabel(IconUtils.createImageIcon("logo.png", "App Logo", 36, 36));

        appTitleLabel = new JLabel(" Nhật Ngữ VN");
        appTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appTitleLabel.setForeground(UIManager.getColor("Label.foreground"));

        JPanel logoTitlePanel = new JPanel();
        logoTitlePanel.setLayout(new BoxLayout(logoTitlePanel, BoxLayout.X_AXIS));
        logoTitlePanel.setOpaque(false);
        logoTitlePanel.add(Box.createHorizontalStrut(5));
        logoTitlePanel.add(appLogoLabel);
        logoTitlePanel.add(Box.createHorizontalStrut(8));
        logoTitlePanel.add(appTitleLabel);
        logoTitlePanel.add(Box.createHorizontalGlue());
        
        topSectionPanel.add(logoTitlePanel, BorderLayout.CENTER);

        ImageIcon menuToggleIcon = IconUtils.createImageIcon(isMenuExpanded ? "menu.png" : "menu.png", "Toggle Menu", 22, 22);
        toggleMenuButton = new JButton(menuToggleIcon);
        toggleMenuButton.setToolTipText(isMenuExpanded ? "Thu gọn Menu" : "Mở rộng Menu");
        toggleMenuButton.setPreferredSize(new Dimension(40, 40));
        toggleMenuButton.setBorder(BorderFactory.createEmptyBorder());
        toggleMenuButton.setContentAreaFilled(false);
        toggleMenuButton.setFocusPainted(false);
        toggleMenuButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggleMenuButton.addActionListener(e -> toggleMenuAnimation()); // Gọi phương thức animation mới
        topSectionPanel.add(toggleMenuButton, BorderLayout.EAST);

        menuItemsPanel = new JPanel();
        menuItemsPanel.setLayout(new BoxLayout(menuItemsPanel, BoxLayout.Y_AXIS));
        menuItemsPanel.setOpaque(false);
        menuItemsPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JButton btnDashboard = createMenuButton("Trang chủ", "home.png", MainFrame.DASHBOARD_PANEL);
        JButton btnQuestions = createMenuButton("Quản lý Câu hỏi", "questions.png", MainFrame.QUESTION_MANAGEMENT_PANEL);
        JButton btnTests = createMenuButton("Quản lý Đề thi", "test.png", MainFrame.TEST_MANAGEMENT_PANEL);
        JButton btnSettings = createMenuButton("Cài đặt", "setting.png", MainFrame.SETTINGS_PANEL);

        menuItemsPanel.add(btnDashboard);
        menuItemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuItemsPanel.add(btnQuestions);
        menuItemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuItemsPanel.add(btnTests);
        menuItemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        menuItemsPanel.add(Box.createVerticalGlue());
        menuItemsPanel.add(btnSettings);
        
        selectButton(btnDashboard);

        add(topSectionPanel, BorderLayout.NORTH);
        add(menuItemsPanel, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String text, String iconPath, String actionCommand) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(UIManager.getColor("Button.pressedBackground"));
                } else if (getModel().isRollover() || this.equals(selectedButton)) {
                    g2d.setColor(UIManager.getColor("MenuItem.selectionBackground"));
                } else {
                    g2d.setColor(getBackground());
                }
                if (!getModel().isRollover() && !this.equals(selectedButton) && !getModel().isPressed()) {
                     g2d.setColor(new Color(0,0,0,0));
                }
                g2d.fillRoundRect(5, 2, getWidth() - 10, getHeight() - 4, 15, 15);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        button.setIcon(IconUtils.createImageIcon(iconPath,text, 22, 22));
        button.setActionCommand(actionCommand);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(15);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBackground(new Color(0,0,0,0));

        button.addActionListener(e -> {
            selectButton(button);
            if (navigationCallback != null) {
                navigationCallback.accept(e.getActionCommand());
            }
        });
        return button;
    }
    
    private void selectButton(JButton button) {
        if (selectedButton != null) {
            selectedButton.setFont(selectedButton.getFont().deriveFont(Font.PLAIN));
            selectedButton.setForeground(UIManager.getColor("MenuItem.foreground"));
            selectedButton.repaint();
        }
        selectedButton = button;
        selectedButton.setFont(selectedButton.getFont().deriveFont(Font.BOLD));
        selectedButton.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
        selectedButton.repaint();
    }

    private void toggleMenuAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop(); // Dừng animation cũ nếu đang chạy
        }

        isMenuExpanded = !isMenuExpanded; // Đảo ngược trạng thái mong muốn

        final int startWidth = getPreferredSize().width;
        final int targetWidth = isMenuExpanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        final int totalChange = targetWidth - startWidth;
        final int delay = ANIMATION_DURATION / ANIMATION_STEPS;

        // Ẩn/hiện title và logo ngay lập tức nếu đang mở rộng ra, hoặc sau khi thu nhỏ xong
        if (isMenuExpanded) {
            appTitleLabel.setVisible(true);
            appLogoLabel.setVisible(true);
        }


        animationTimer = new Timer(delay, new ActionListener() {
            private int currentStep = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                currentStep++;
                if (currentStep >= ANIMATION_STEPS) {
                    setPreferredSize(new Dimension(targetWidth, getHeight()));
                    ((Timer) e.getSource()).stop(); // Dừng timer
                    // Cập nhật UI sau khi animation hoàn tất
                    if (!isMenuExpanded) { // Nếu vừa thu gọn xong
                        appTitleLabel.setVisible(false);
                        appLogoLabel.setVisible(false);
                    }
                    updateMenuItemsUI(); // Cập nhật text/icon của các nút menu
                    toggleMenuButton.setIcon(IconUtils.createImageIcon(isMenuExpanded ? "menu.png" : "menu.png", "Toggle Menu", 22, 22));
                    toggleMenuButton.setToolTipText(isMenuExpanded ? "Thu gọn Menu" : "Mở rộng Menu");

                } else {
                    float fraction = (float) currentStep / ANIMATION_STEPS;
                    // Có thể dùng easing function ở đây để mượt hơn
                    // Ví dụ: ease-out: fraction = 1 - (1 - fraction) * (1 - fraction);
                    int currentAnimatedWidth = startWidth + (int) (totalChange * fraction);
                    setPreferredSize(new Dimension(currentAnimatedWidth, getHeight()));
                }

                if (getParent() != null) {
                    getParent().revalidate();
                    getParent().repaint();
                } else {
                    revalidate();
                    repaint();
                }
            }
        });
        animationTimer.setInitialDelay(0); // Bắt đầu ngay
        animationTimer.start();
    }


    private void updateMenuItemsUI() {
        for (Component comp : menuItemsPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                String originalText = "";
                if (button.getActionCommand().equals(MainFrame.DASHBOARD_PANEL)) originalText = "Trang chủ";
                else if (button.getActionCommand().equals(MainFrame.QUESTION_MANAGEMENT_PANEL)) originalText = "Quản lý Câu hỏi";
                else if (button.getActionCommand().equals(MainFrame.TEST_MANAGEMENT_PANEL)) originalText = "Quản lý Đề thi";
                else if (button.getActionCommand().equals(MainFrame.SETTINGS_PANEL)) originalText = "Cài đặt";

                if (isMenuExpanded) {
                    button.setText(originalText);
                    button.setHorizontalAlignment(SwingConstants.LEFT);
                    button.setPreferredSize(new Dimension(EXPANDED_WIDTH - 20, 50));
                } else {
                    button.setText("");
                    button.setToolTipText(originalText); // Giữ tooltip để người dùng biết chức năng
                    button.setHorizontalAlignment(SwingConstants.CENTER);
                    button.setPreferredSize(new Dimension(COLLAPSED_WIDTH - 10, 50));
                }
                 button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            }
        }
        menuItemsPanel.revalidate();
        menuItemsPanel.repaint();
    }
    
    public void updateThemeColors() {
        setBackground(UIManager.getColor("List.background"));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Component.borderColor")));
        if (appTitleLabel != null) appTitleLabel.setForeground(UIManager.getColor("Label.foreground"));
        if (menuItemsPanel != null) {
            for (Component comp : menuItemsPanel.getComponents()) {
                if (comp instanceof JButton) {
                    comp.setForeground(UIManager.getColor("MenuItem.foreground"));
                    comp.repaint();
                }
            }
            if (selectedButton != null) {
                 selectedButton.setForeground(UIManager.getColor("MenuItem.selectionForeground"));
                 selectedButton.repaint();
            }
        }
        revalidate();
        repaint();
    }
}

