
package View;

import Main.Test; 
import Controller.AppController;
import Controller.QuestionController;
import Controller.TestController; // Bỏ comment nếu bạn có TestController

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {
    private SideMenuPanel sideMenuPanel;
    private TopHeaderPanel topHeaderPanel;
    private JPanel mainContentPanelContainer;
    private CardLayout cardLayout;

    private DashboardPanel dashboardPanel;
    private QuestionManagementPanel questionManagementPanel;
    private TestManagementPanel testManagementPanel;
    private SettingsPanel settingsPanel;
    
    private AppController appController;

    public static final String DASHBOARD_PANEL = "DashboardPanel";
    public static final String QUESTION_MANAGEMENT_PANEL = "QuestionManagementPanel";
    public static final String TEST_MANAGEMENT_PANEL = "TestManagementPanel";
    public static final String SETTINGS_PANEL = "SettingsPanel";

    public MainFrame() {
        setTitle("Quản lý Ngân hàng Đề thi Tiếng Nhật");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 768)); // Tăng kích thước tối thiểu
        setSize(1366, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0,0));
        initComponents();
    }

    private void initComponents() {
        topHeaderPanel = new TopHeaderPanel();
        add(topHeaderPanel, BorderLayout.NORTH);

        sideMenuPanel = new SideMenuPanel(this::navigateTo);
        add(sideMenuPanel, BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainContentPanelContainer = new JPanel(cardLayout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(UIManager.getColor("Panel.background"));
                int arc = 20; // Tăng độ bo góc
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2d.dispose();
            }
        };
        mainContentPanelContainer.setOpaque(false);
        mainContentPanelContainer.setBorder(new EmptyBorder(15, 15, 15, 15)); // Tăng padding

        dashboardPanel = new DashboardPanel();
        questionManagementPanel = new QuestionManagementPanel(null); 
        testManagementPanel = new TestManagementPanel(null); 
        settingsPanel = new SettingsPanel(this);

        mainContentPanelContainer.add(dashboardPanel, DASHBOARD_PANEL);
        mainContentPanelContainer.add(questionManagementPanel, QUESTION_MANAGEMENT_PANEL);
        mainContentPanelContainer.add(testManagementPanel, TEST_MANAGEMENT_PANEL);
        mainContentPanelContainer.add(settingsPanel, SETTINGS_PANEL);

        add(mainContentPanelContainer, BorderLayout.CENTER);
        cardLayout.show(mainContentPanelContainer, DASHBOARD_PANEL);
    }
    
    public QuestionManagementPanel getQuestionManagementPanelInstance() {
        return questionManagementPanel;
    }
    public TestManagementPanel getTestManagementPanelInstance() {
        return testManagementPanel;
    }
     public SettingsPanel getSettingsPanelInstance() {
        return settingsPanel;
    }

    public void navigateTo(String panelName) {
        cardLayout.show(mainContentPanelContainer, panelName);
        System.out.println("Navigating to: " + panelName);
    }

    public void applyTheme(boolean useDarkTheme) {
        Main.Test.applyTheme(useDarkTheme); 
        if (topHeaderPanel != null) topHeaderPanel.updateThemeColors();
        if (sideMenuPanel != null) sideMenuPanel.updateThemeColors();
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
        if (this.appController != null) {
            QuestionController qCtrl = this.appController.getQuestionController();
            if (qCtrl != null && this.questionManagementPanel != null) {
                this.questionManagementPanel.setController(qCtrl);
                qCtrl.setView(this.questionManagementPanel);
            }

            TestController tCtrl = this.appController.getTestController();
            if (tCtrl != null && this.testManagementPanel != null) {
                this.testManagementPanel.setController(tCtrl); // TestManagementPanel cần setController
                tCtrl.setView(this.testManagementPanel);      // TestController cần setView
            }
            // Tương tự cho SettingsController nếu cần
        }
    }
}