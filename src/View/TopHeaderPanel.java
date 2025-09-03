package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import Main.Test;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TopHeaderPanel extends JPanel {
    // Mảng màu cho các thẻ, có thể mở rộng
    private Color[][] cardColorsLight = {
        {new Color(255, 230, 204), new Color(255, 200, 153), new Color(230, 115, 0)}, // Cam
        {new Color(204, 230, 255), new Color(153, 200, 255), new Color(0, 71, 171)}, // Xanh dương
        {new Color(204, 255, 204), new Color(153, 255, 153), new Color(0, 102, 0)}, // Xanh lá
        {new Color(230, 204, 230), new Color(200, 153, 200), new Color(102, 0, 102)}  // Tím
    };
     private Color[][] cardColorsDark = {
        {new Color(102, 77, 51), new Color(153, 102, 51), new Color(255, 200, 153)}, // Cam đậm
        {new Color(51, 77, 102), new Color(51, 102, 153), new Color(153, 200, 255)}, // Xanh dương đậm
        {new Color(51, 102, 51), new Color(51, 153, 51), new Color(153, 255, 153)}, // Xanh lá đậm
        {new Color(77, 51, 77), new Color(102, 51, 102), new Color(200, 153, 200)}  // Tím đậm
    };

    private JPanel[] summaryCards; // Lưu trữ các thẻ để cập nhật màu

    public TopHeaderPanel() {
        updateThemeColors(); // Đặt màu ban đầu
        setPreferredSize(new Dimension(getWidth(), 130)); // Tăng chiều cao một chút
        setLayout(new FlowLayout(FlowLayout.CENTER, 25, 20)); // Căn giữa, tăng khoảng cách
        setBorder(BorderFactory.createMatteBorder(0,0,1,0, UIManager.getColor("Component.borderColor")));

        summaryCards = new JPanel[4]; // Số lượng thẻ
        String[] titles = {"Đề N5 Phổ biến", "Đề N4 Kanji", "Đề N3 Nghe hiểu", "Đề N2 Tổng hợp"};
        String[] values = {"1500+ lượt giải", "1250+ lượt giải", "980+ lượt giải", "760+ lượt giải"};

        for (int i = 0; i < summaryCards.length; i++) {
            summaryCards[i] = createSummaryCard(titles[i], values[i], i);
            add(summaryCards[i],new Font("Segoe UI", Font.BOLD, 22));
        }
    }

    private JPanel createSummaryCard(String title, String value, int colorIndex) {
        Color[] currentPalette = Test.isDarkThemeApplied() ? cardColorsDark[colorIndex % cardColorsDark.length] : cardColorsLight[colorIndex % cardColorsLight.length];
        Color bgColor1 = currentPalette[0];
        Color bgColor2 = currentPalette[1]; // Màu thứ hai cho gradient
        Color titleColor = currentPalette[2];

        JPanel card = new JPanel(new BorderLayout(5,8)) { // Tăng khoảng cách dọc
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
           
                GradientPaint gp = new GradientPaint(0, 0, bgColor1, 0, getHeight(), bgColor2);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25); // Bo góc nhiều hơn

             
                g2d.dispose();
            }
        };
        card.setOpaque(false);
      
        card.setPreferredSize(new Dimension(220, 90)); 
        card.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16)); 
        titleLabel.setForeground(titleColor);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        // Màu chữ của valueLabel sẽ tự động thay đổi theo theme nhờ FlatLaf
        valueLabel.setForeground(UIManager.getColor("Label.foreground"));


        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        
        card.addMouseListener(new MouseAdapter() {
            Color originalBg1 = bgColor1;
            Color originalBg2 = bgColor2;

            public void mouseEntered(MouseEvent evt) {
                // Làm sáng màu gradient khi di chuột vào
                card.setBackground(originalBg1.brighter()); // Chỉ để kích hoạt repaint, màu thật vẽ ở paintComponent
                // Để thay đổi màu gradient thực sự, bạn cần lưu trữ màu gốc và màu hover
                // và yêu cầu card vẽ lại với màu hover.
                // Ví dụ đơn giản: chỉ thay đổi màu nền (sẽ bị paintComponent ghi đè nếu không cẩn thận)
                // Hoặc tốt hơn là tạo một biến trạng thái "hover" và vẽ lại trong paintComponent
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent evt) {
                card.setBackground(originalBg1);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        return card;
    }
    
    public void updateThemeColors() {
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createMatteBorder(0,0,1,0, UIManager.getColor("Component.borderColor")));
        // Cập nhật màu cho các thẻ đã tạo
        if (summaryCards != null) {
            for (int i = 0; i < summaryCards.length; i++) {
                if (summaryCards[i] != null) {
                    // Lấy lại màu dựa trên theme hiện tại
                    Color[] currentPalette = Test.isDarkThemeApplied() ? cardColorsDark[i % cardColorsDark.length] : cardColorsLight[i % cardColorsLight.length];
                    // Cần một cách để truyền màu mới vào paintComponent của card
                    // Cách đơn giản là remove và add lại, hoặc tạo phương thức updateColors cho card
                    // Tạm thời, repaint có thể đủ nếu màu được lấy động trong paintComponent (nhưng hiện tại chưa)
                    // Để đơn giản, ta có thể remove và add lại các card
                }
            }
        }
        // Nếu các card không tự cập nhật màu gradient, bạn cần remove và add lại chúng
        // hoặc tạo một phương thức trong createSummaryCard để nó có thể nhận màu mới.
        // Cách đơn giản nhất là removeAll() và gọi lại phần khởi tạo card.
        if (summaryCards != null && summaryCards.length > 0) { // Kiểm tra để tránh lỗi khi khởi tạo lần đầu
            removeAll();
            String[] titles = {"Đề N5 Phổ biến", "Đề N4 Kanji", "Đề N3 Nghe hiểu", "Đề N2 Tổng hợp"};
            String[] values = {"1500+ lượt giải", "1250+ lượt giải", "980+ lượt giải", "760+ lượt giải"};
            for (int i = 0; i < summaryCards.length; i++) {
                summaryCards[i] = createSummaryCard(titles[i], values[i], i);
                add(summaryCards[i]);
            }
        }
        revalidate();
        repaint();
    }
}
