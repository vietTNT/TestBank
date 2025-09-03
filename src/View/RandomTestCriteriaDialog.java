// File: View/RandomTestCriteriaDialog.java
package View;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RandomTestCriteriaDialog extends JDialog {
    private JTextField txtTestName;
    private JComboBox<String> cmbDifficultyLevel;
    private JSpinner spnNumGrammar, spnNumListening, spnNumReading;
    // Thêm các JSpinner khác nếu có thêm loại câu hỏi (ví dụ: Từ vựng, Kanji)
     private JSpinner spnNumVocabulary, spnNumKanji;


    private JButton btnOk, btnCancel;
    private boolean confirmed = false;

    private final String[] DIFFICULTY_LEVELS = {"Tất cả", "N5", "N4", "N3", "N2", "N1", "Bảng Chữ Cái", "Khác"};
    // Đảm bảo các giá trị này khớp với giá trị trong CSDL hoặc logic của bạn
    public static final String GRAMMAR_TYPE = "Ngữ pháp";
    public static final String LISTENING_TYPE = "Nghe hiểu";
    public static final String READING_TYPE = "Đọc hiểu";
     public static final String VOCABULARY_TYPE = "Từ vựng";
     public static final String KANJI_TYPE = "Kanji";


    public RandomTestCriteriaDialog(Frame owner) {
        super(owner, "Tạo Đề Thi Ngẫu Nhiên Theo Tiêu Chí", true);
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));

        // Test Name
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Tên Đề Thi:"));
        txtTestName = new JTextField(25);
        namePanel.add(txtTestName);
        formPanel.add(namePanel);

        // Difficulty Level
        JPanel difficultyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        difficultyPanel.add(new JLabel("Mức độ khó:"));
        cmbDifficultyLevel = new JComboBox<>(DIFFICULTY_LEVELS);
        difficultyPanel.add(cmbDifficultyLevel);
        formPanel.add(difficultyPanel);

        // Number of questions per type
        formPanel.add(createQuestionCountPanel("Số câu Ngữ pháp:", spnNumGrammar = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1))));
        formPanel.add(createQuestionCountPanel("Số câu Nghe hiểu:", spnNumListening = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1))));
        formPanel.add(createQuestionCountPanel("Số câu Đọc hiểu:", spnNumReading = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1))));
        formPanel.add(createQuestionCountPanel("Số câu Từ vựng:", spnNumVocabulary = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1))));
        formPanel.add(createQuestionCountPanel("Số câu Kanji:", spnNumKanji = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1))));


        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnOk = new JButton("Tạo Đề");
        btnCancel = new JButton("Hủy");

        btnOk.addActionListener(e -> {
            if (txtTestName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên đề thi không được để trống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        });
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnOk);
        buttonPanel.add(btnCancel);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createQuestionCountPanel(String labelText, JSpinner spinner) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(labelText));
        spinner.setPreferredSize(new Dimension(60, spinner.getPreferredSize().height));
        panel.add(spinner);
        return panel;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getTestName() {
        return txtTestName.getText().trim();
    }

    public Map<String, Object> getCriteria() {
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("difficultyLevel", cmbDifficultyLevel.getSelectedItem().toString());
        // Số lượng câu hỏi sẽ được lấy riêng
        return criteria;
    }

    public Map<String, Integer> getQuestionCounts() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put(GRAMMAR_TYPE, (Integer) spnNumGrammar.getValue());
        counts.put(LISTENING_TYPE, (Integer) spnNumListening.getValue());
        counts.put(READING_TYPE, (Integer) spnNumReading.getValue());
         counts.put(VOCABULARY_TYPE, (Integer) spnNumVocabulary.getValue());
         counts.put(KANJI_TYPE, (Integer) spnNumKanji.getValue());
        return counts;
    }
}