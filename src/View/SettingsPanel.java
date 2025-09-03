package View;
import javax.swing.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import Main.Test;


public class SettingsPanel extends JPanel {
    private MainFrame mainFrame;

    public SettingsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    
        setLayout(new BorderLayout(10,10));
        setBorder(new EmptyBorder(25,30,25,30)); 

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false); 

        JLabel titleLabel = new JLabel("Cài đặt Giao diện");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22)); 
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(new EmptyBorder(0,0,15,0));
        formPanel.add(titleLabel);

        // Lựa chọn Theme
        JPanel themeSelectionPanel = new JPanel(new BorderLayout(10,5));
        themeSelectionPanel.setOpaque(false);
        themeSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeSelectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel themeLabel = new JLabel("Chủ đề ứng dụng:");
        themeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        themeSelectionPanel.add(themeLabel, BorderLayout.NORTH);

        JRadioButton radioLightTheme = new JRadioButton("Sáng");
        radioLightTheme.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        radioLightTheme.setActionCommand("light");
        radioLightTheme.setOpaque(false);

        JRadioButton radioDarkTheme = new JRadioButton("Tối");
        radioDarkTheme.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        radioDarkTheme.setActionCommand("dark");
        radioDarkTheme.setOpaque(false);

        ButtonGroup themeGroup = new ButtonGroup();
        themeGroup.add(radioLightTheme);
        themeGroup.add(radioDarkTheme);

        if (Test.isDarkThemeApplied()) {
            radioDarkTheme.setSelected(true);
        } else {
            radioLightTheme.setSelected(true);
        }
        
        ActionListener themeListener = (ActionEvent e) -> {
            if (mainFrame != null) {
                mainFrame.applyTheme("dark".equals(e.getActionCommand()));
            }
        };
        radioLightTheme.addActionListener(themeListener);
        radioDarkTheme.addActionListener(themeListener);
        
        JPanel themeRadioContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        themeRadioContainer.setOpaque(false);
 
        themeRadioContainer.add(radioLightTheme);
        themeRadioContainer.add(Box.createHorizontalStrut(20));
   
        themeRadioContainer.add(radioDarkTheme);
        themeSelectionPanel.add(themeRadioContainer, BorderLayout.CENTER);
        
        formPanel.add(themeSelectionPanel);
        formPanel.add(Box.createVerticalStrut(30));

        // Nút lưu cài đặt (ví dụ)
        JButton btnSaveChanges = new JButton("Lưu thay đổi");
        btnSaveChanges.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSaveChanges.setIcon(IconUtils.createImageIcon("icons/save_24.png", "Save Settings", 18,18));
        btnSaveChanges.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSaveChanges.addActionListener(e -> {
            // Logic lưu cài đặt (ví dụ: lưu theme đã chọn vào preferences)
            JOptionPane.showMessageDialog(this, "Cài đặt đã được lưu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
//         formPanel.add(btnSaveChanges); 

        add(formPanel, BorderLayout.NORTH);
    }
}