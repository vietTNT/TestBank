package View;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class IconUtils {

    public static ImageIcon createImageIcon(String path, String description, int width, int height) {
        URL imgURL = IconUtils.class.getResource(path); // Tìm resource từ classpath
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL, description);
            if (width > 0 && height > 0) {
                Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage, description);
            }
            return originalIcon; // Trả về icon gốc nếu không cần scale
        } else {
            System.err.println("Couldn't find file: " + path);
            // Tạo một placeholder icon nếu không tìm thấy file
            BufferedImage placeholder = new BufferedImage(width > 0 ? width : 16, height > 0 ? height : 16, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = placeholder.createGraphics();
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawString("?", placeholder.getWidth() / 2 - 4, placeholder.getHeight() / 2 + 4);
            g2d.dispose();
            return new ImageIcon(placeholder, description);
        }
    }
     public static ImageIcon createImageIcon(String path, String description) {
        return createImageIcon(path, description, -1, -1); // -1 để không scale
    }
}