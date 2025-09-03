package Main;

import java.awt.Window;
import java.sql.Connection;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import Controller.AppController;
import View.MainFrame;
import database.JDBCUtil;

public class Test {

	private static boolean isCurrentlyDarkTheme = false;
	public static void main(String[] args) {
	
		
		applyTheme(false); 
		
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            AppController appController = new AppController(mainFrame);
            mainFrame.setAppController(appController); 
            mainFrame.setVisible(true);
        
        });
	}
	public static void applyTheme(boolean useDarkTheme) {
        try {
            if (useDarkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                isCurrentlyDarkTheme = true;
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                isCurrentlyDarkTheme = false;
            }
            // Cập nhật UI cho tất cả các cửa sổ đang mở (nếu có)
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
        } catch (UnsupportedLookAndFeelException e) {
            System.err.println("Failed to initialize FlatLaf theme: " + e.getMessage());
        }
    }
	 public static boolean isDarkThemeApplied() {
	        return isCurrentlyDarkTheme;
	    }
}
