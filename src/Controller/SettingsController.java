package Controller;

import View.MainFrame;

public class SettingsController {
    private MainFrame mainFrame;
    // private SettingsPanel view;
    // private UserPreferences preferences; // Ví dụ

    public SettingsController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        // this.preferences = new UserPreferences(); // Load preferences
        System.out.println("SettingsController initialized.");
    }

    // public void setView(SettingsPanel view) {
    //     this.view = view;
    //     // if (this.view != null) {
    //     //     loadCurrentSettings();
    //     // }
    // }

    // public void loadCurrentSettings() {
    //     // Ví dụ: view.setThemeSelection(preferences.getTheme());
    // }

    // public void saveThemePreference(boolean useDarkTheme) {
    //     // preferences.setTheme(useDarkTheme ? "dark" : "light");
    //     // preferences.save();
    //     mainFrame.applyTheme(useDarkTheme); // Gọi MainFrame để áp dụng theme
    // }

    // Các phương thức khác cho việc lưu/tải cài đặt
}
