package View; // Đảm bảo package khớp với dự án của bạn

import model.Answer; // Đảm bảo model.Answer được import đúng

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class AnswersPanel extends JPanel {
    private JPanel answerEntriesPanel;
    private JButton btnAddAnswer;
    private List<AnswerEntryPanel> answerEntryPanels;
    private ButtonGroup correctAnswerGroup;
    private Font currentAnswerFont; // Lưu trữ font hiện tại cho các câu trả lời

    public AnswersPanel() {
        setLayout(new BorderLayout(5, 10));
        setOpaque(false); // Để nền của parent hiển thị (nếu QuestionEditorDialog có nền tùy chỉnh)
        answerEntryPanels = new ArrayList<>();
        correctAnswerGroup = new ButtonGroup();
        // Đặt font mặc định ban đầu, sẽ được cập nhật bởi QuestionEditorDialog nếu cần
        currentAnswerFont = new Font("Segoe UI", Font.PLAIN, 14); 

        answerEntriesPanel = new JPanel();
        answerEntriesPanel.setLayout(new BoxLayout(answerEntriesPanel, BoxLayout.Y_AXIS));
        answerEntriesPanel.setOpaque(false);
        
        JScrollPane scrollPane = new JScrollPane(answerEntriesPanel);
        scrollPane.setPreferredSize(new Dimension(400, 150)); // Có thể điều chỉnh
        scrollPane.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        scrollPane.getViewport().setBackground(UIManager.getColor("TextField.background"));

        // Sử dụng IconUtils từ package View
        btnAddAnswer = new JButton("Thêm lựa chọn", View.IconUtils.createImageIcon("add.png", "Thêm lựa chọn", 16,16));
        btnAddAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnAddAnswer.setMargin(new Insets(4,8,4,8));
        btnAddAnswer.addActionListener(e -> addAnswerEntry(false, ""));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnAddAnswer);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Khởi tạo với một vài lựa chọn nếu cần, hoặc để trống
        // setAnswers(new ArrayList<>()); // Gọi setAnswers để có ít nhất 2 lựa chọn ban đầu
    }

    private void addAnswerEntry(boolean isCorrect, String text) {
        AnswerEntryPanel newEntry = new AnswerEntryPanel(text, isCorrect);
        newEntry.setAnswerFont(currentAnswerFont); // Áp dụng font hiện tại khi thêm entry mới
        correctAnswerGroup.add(newEntry.getCorrectRadioButton());
        
        // Nếu đây là entry đầu tiên được thêm vào một danh sách rỗng, hoặc isCorrect là true, chọn nó.
        if (answerEntryPanels.isEmpty() || isCorrect) {
            // Đảm bảo không có cái nào khác được chọn trước khi chọn cái này
            boolean otherSelected = false;
            for(AnswerEntryPanel entry : answerEntryPanels){
                if(entry.getCorrectRadioButton().isSelected()){
                    otherSelected = true;
                    break;
                }
            }
            if(!otherSelected || isCorrect){ // Chỉ chọn nếu không có cái nào khác đang được chọn, HOẶC nó được chỉ định là đúng
                 newEntry.getCorrectRadioButton().setSelected(true);
            }
        }
        
        newEntry.getRemoveButton().addActionListener(e -> {
            boolean wasSelected = newEntry.getCorrectRadioButton().isSelected();
            correctAnswerGroup.remove(newEntry.getCorrectRadioButton());
            answerEntriesPanel.remove(newEntry);
            answerEntryPanels.remove(newEntry);
            
            // Nếu đáp án đúng bị xóa và vẫn còn các lựa chọn khác, chọn cái đầu tiên là đúng
            if (wasSelected && !answerEntryPanels.isEmpty()) {
                boolean anySelected = false;
                for(AnswerEntryPanel entry : answerEntryPanels){
                    if(entry.getCorrectRadioButton().isSelected()){
                        anySelected = true;
                        break;
                    }
                }
                if(!anySelected){ // Nếu không còn cái nào được chọn, chọn cái đầu tiên
                     answerEntryPanels.get(0).getCorrectRadioButton().setSelected(true);
                }
            }
            answerEntriesPanel.revalidate();
            answerEntriesPanel.repaint();
            // Không tự động thêm lại nếu xóa hết, để người dùng quyết định
        });
        answerEntryPanels.add(newEntry);
        answerEntriesPanel.add(newEntry);
        answerEntriesPanel.add(Box.createVerticalStrut(8)); // Khoảng cách giữa các entry
        answerEntriesPanel.revalidate();
        answerEntriesPanel.repaint();
    }
    
    public void setAnswers(List<Answer> answers) {
        answerEntriesPanel.removeAll(); 
        answerEntryPanels.clear();
        correctAnswerGroup = new ButtonGroup(); // Quan trọng: reset ButtonGroup

        if (answers == null || answers.isEmpty()) {
            // Thêm 2 lựa chọn mặc định nếu không có đáp án nào được cung cấp
            addAnswerEntry(true, ""); // Đáp án đầu tiên mặc định là đúng
            addAnswerEntry(false, "");
            return;
        }

        boolean oneCorrectExists = false;
        for (Answer answer : answers) {
            addAnswerEntry(answer.isCorrect(), answer.getAnswerText());
            if (answer.isCorrect()) {
                oneCorrectExists = true;
            }
        }
        // Đảm bảo có ít nhất một radio button được chọn nếu có đáp án và chưa có cái nào đúng
        if (!answers.isEmpty() && !oneCorrectExists && !answerEntryPanels.isEmpty()) {
            answerEntryPanels.get(0).getCorrectRadioButton().setSelected(true);
        }
        answerEntriesPanel.revalidate();
        answerEntriesPanel.repaint();
    }

    public List<Answer> getAnswers() {
        List<Answer> answers = new ArrayList<>();
        for (AnswerEntryPanel entryPanel : answerEntryPanels) {
            String text = entryPanel.getAnswerTextField().getText().trim();
            // Chỉ lưu đáp án có nội dung hoặc nếu nó được đánh dấu là đúng (cho phép đáp án đúng rỗng nếu cần)
            if (!text.isEmpty() || entryPanel.getCorrectRadioButton().isSelected()) {
                Answer answer = new Answer();
                answer.setAnswerText(text); 
                answer.setCorrect(entryPanel.getCorrectRadioButton().isSelected());
                answers.add(answer);
            }
        }
        return answers;
    }

    /**
     * Đặt font chữ cho tất cả các trường nhập liệu câu trả lời hiện có.
     * @param font Font mới để áp dụng.
     */
    public void setAnswerFontForAllEntries(Font font) {
        this.currentAnswerFont = font;
        for (AnswerEntryPanel entryPanel : answerEntryPanels) {
            entryPanel.setAnswerFont(font);
        }
    }
    
    /**
     * Lấy font chữ hiện tại đang được áp dụng cho các câu trả lời.
     * Nếu không có câu trả lời nào, trả về font mặc định.
     * @return Font hiện tại.
     */
    public Font getCurrentAnswerFont() {
        if (!answerEntryPanels.isEmpty() && answerEntryPanels.get(0).getAnswerTextField() != null) {
            return answerEntryPanels.get(0).getAnswerTextField().getFont();
        }
        return this.currentAnswerFont; // Trả về font đã lưu hoặc font mặc định
    }
    
    // Getter để QuestionEditorDialog có thể lấy danh sách các panel entry (nếu cần)
    public List<AnswerEntryPanel> getAnswerEntryPanels() {
        return answerEntryPanels;
    }

    /**
     * Lớp nội bộ đại diện cho một dòng nhập liệu câu trả lời.
     */
    public static class AnswerEntryPanel extends JPanel {
        private JTextField answerTextField;
        private JRadioButton correctRadioButton;
        private JButton removeButton;

        public AnswerEntryPanel(String text, boolean isCorrect) {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            setOpaque(false); 
            setBorder(new EmptyBorder(2,2,2,2));

            correctRadioButton = new JRadioButton();
            correctRadioButton.setOpaque(false);
            correctRadioButton.setSelected(isCorrect);
            correctRadioButton.setToolTipText("Đánh dấu là đáp án đúng");
            correctRadioButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            answerTextField = new JTextField(text, 35);
            answerTextField.putClientProperty("JTextField.placeholderText", "Nhập nội dung lựa chọn...");

            // Sử dụng IconUtils từ package View
            removeButton = new JButton(View.IconUtils.createImageIcon("icon_X.png", "Xóa lựa chọn", 14,14));
            removeButton.setToolTipText("Xóa lựa chọn này");
            removeButton.setMargin(new Insets(2,2,2,2));
            removeButton.setFocusPainted(false);
            removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // removeButton.setContentAreaFilled(false); // Tuỳ chọn để nút trong suốt hơn

            add(correctRadioButton);
            add(Box.createHorizontalStrut(8));
            add(answerTextField); // TextField sẽ chiếm không gian còn lại
            add(Box.createHorizontalStrut(8));
            add(removeButton);
            
            // Đảm bảo panel không bị co lại quá mức theo chiều dọc
            setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height + 5));
        }

        public JTextField getAnswerTextField() { return answerTextField; }
        public JRadioButton getCorrectRadioButton() { return correctRadioButton; }
        public JButton getRemoveButton() { return removeButton; }

        /**
         * Đặt font chữ cho trường nhập liệu câu trả lời.
         * @param font Font mới.
         */
        public void setAnswerFont(Font font) {
            if (answerTextField != null) {
                answerTextField.setFont(font);
            }
        }
    }
}

