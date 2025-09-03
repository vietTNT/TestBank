package View;

import Controller.TestController; // Giả sử có
import model.Question; // Để hiển thị câu hỏi
import model.Test;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.List;
public class TestEditorDialog extends JDialog {
    private Test currentTest; // Hoặc model.Test
    private TestController controller;
    private boolean saved = false;

    // UI Components
    private JTextField txtTestName;
    private JTextArea txtTestDescription;
    private QuestionPickerPanel questionPickerPanel; // Panel chọn câu hỏi

    private Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
    private Font fieldFont = new Font("Segoe UI", Font.PLAIN, 14);

    public TestEditorDialog(Frame owner, Test test, TestController controller, List<Question> allAvailableQuestions) {
        super(owner, (test == null || test.getTestId() == 0 ? "Tạo Đề Thi Mới" : "Chỉnh Sửa Đề Thi"), true);
        this.currentTest = (test == null) ? new Test() : test; // Tạo mới hoặc dùng test hiện tại
        this.controller = controller;

        setMinimumSize(new Dimension(900, 700));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(15, 15));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));

        initComponents(allAvailableQuestions);
        loadTestData();
        pack();
    }

    private void initComponents(List<Question> allAvailableQuestions) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Tên Đề Thi
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createStyledLabel("Tên Đề Thi:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        txtTestName = new JTextField(30);
        txtTestName.setFont(fieldFont);
        formPanel.add(txtTestName, gbc);
        gbc.weightx = 0.0; // Reset

        // Mô Tả
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(createStyledLabel("Mô Tả:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.ipady = 40; // Cho JTextArea cao hơn
        txtTestDescription = new JTextArea(4, 30);
        txtTestDescription.setFont(fieldFont);
        txtTestDescription.setLineWrap(true);
        txtTestDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(txtTestDescription);
        formPanel.add(descScrollPane, gbc);
        gbc.ipady = 0; gbc.weightx = 0.0;

        // Panel Chọn Câu Hỏi
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH; // Cho phép mở rộng cả 2 chiều
        List<Question> selectedQuestionsForTest = (currentTest != null && currentTest.getQuestionsInTest() != null)
                                                ? currentTest.getQuestionsInTest()
                                                : new ArrayList<>();
        questionPickerPanel = new QuestionPickerPanel(allAvailableQuestions, selectedQuestionsForTest);
        questionPickerPanel.setBorder(BorderFactory.createTitledBorder(null, "Chọn Câu Hỏi Cho Đề Thi",
                TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                new Font("Segoe UI", Font.BOLD, 13), UIManager.getColor("Label.foreground")));
        formPanel.add(questionPickerPanel, gbc);
        gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL; // Reset


        // Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnSave = createStyledButton("Lưu Đề Thi", "save.png");
        JButton btnCancel = createStyledButton("Hủy Bỏ", "icon_X.png");

        btnSave.addActionListener(this::saveTestAction);
        btnCancel.addActionListener(e -> dispose());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(labelFont);
        return label;
    }
    
    private JButton createStyledButton(String text, String iconPath) {
        JButton button = new JButton(text, IconUtils.createImageIcon(iconPath, text, 18, 18));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
        button.setMargin(new Insets(8, 15, 8, 15));
        return button;
    }

    private void loadTestData() {
        if (currentTest != null && currentTest.getTestId() != 0) { // Giả sử testId != 0 là test đã tồn tại
            txtTestName.setText(currentTest.getTestName());
            txtTestDescription.setText(currentTest.getDescription());
            // questionPickerPanel đã được khởi tạo với selectedQuestions từ currentTest
        }else if (currentTest != null) { // Trường hợp tạo mới
            txtTestName.setText(currentTest.getTestName() != null ? currentTest.getTestName() : "");
            txtTestDescription.setText(currentTest.getDescription() != null ? currentTest.getDescription() : "");
        }
    }

    private void saveTestAction(ActionEvent e) {
        String testName = txtTestName.getText().trim();
        if (testName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên đề thi không được để trống.", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            txtTestName.requestFocus();
            return;
        }

        currentTest.setTestName(testName);
        currentTest.setDescription(txtTestDescription.getText().trim());
        currentTest.setQuestionsInTest(questionPickerPanel.getSelectedQuestions()); // Lấy danh sách câu hỏi đã chọn

        if (currentTest.getQuestionsInTest() == null || currentTest.getQuestionsInTest().isEmpty()) {
             int confirm = JOptionPane.showConfirmDialog(this,
                    "Đề thi hiện chưa có câu hỏi nào. Bạn có muốn tiếp tục lưu không?",
                    "Xác nhận lưu đề trống", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.NO_OPTION) {
                return;
            }
        }
        
        saved = true;
        dispose();
    }

    public boolean isSaved() { return saved; }
    public Test getTest() { return currentTest; } // Trả về TestModel (hoặc model.Test)
}
