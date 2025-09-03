package View;

import model.Question;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ParsedQuestionsReviewDialog extends JDialog {
    private JTable questionsTable;
    private DefaultTableModel tableModel;
    private JButton btnSaveSelected, btnCancel;
    private JComboBox<String> cmbApplyAllDifficulty;
    private List<Question> parsedQuestions;
    private List<Question> questionsToSave;
    private boolean saved = false;

    private final String[] DIFFICULTY_LEVELS = {"N5", "N4", "N3", "N2", "N1", "Bảng Chữ Cái", "Khác"};

    public ParsedQuestionsReviewDialog(Frame owner, List<Question> questions) {
        super(owner, "Xem Lại và Thiết Lập Câu Hỏi từ AI", true);
        this.parsedQuestions = new ArrayList<>(questions); // Tạo bản copy để làm việc
        this.questionsToSave = new ArrayList<>();

        initComponents();
        populateTable();

        setSize(800, 600);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // Panel chọn mức độ cho tất cả
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Áp dụng mức độ cho tất cả:"));
        cmbApplyAllDifficulty = new JComboBox<>(DIFFICULTY_LEVELS);
        JButton btnApplyAll = new JButton("Áp dụng");
        btnApplyAll.addActionListener(this::applyAllDifficultyAction);
        topPanel.add(cmbApplyAllDifficulty);
        topPanel.add(btnApplyAll);
        add(topPanel, BorderLayout.NORTH);

        // Bảng hiển thị câu hỏi
        String[] columnNames = {"STT", "Nội dung câu hỏi ", "Loại", "Mức độ"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; 
            }
        };
        questionsTable = new JTable(tableModel);
        questionsTable.setRowHeight(25);
        questionsTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        questionsTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        questionsTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        questionsTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Thiết lập editor ComboBox cho cột "Mức độ"
        TableColumn difficultyColumn = questionsTable.getColumnModel().getColumn(3);
        JComboBox<String> comboBoxEditor = new JComboBox<>(DIFFICULTY_LEVELS);
        difficultyColumn.setCellEditor(new DefaultCellEditor(comboBoxEditor));

        add(new JScrollPane(questionsTable), BorderLayout.CENTER);

        // Panel nút Lưu/Hủy
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnSaveSelected = new JButton("Lưu các câu hỏi này");
        btnCancel = new JButton("Hủy");

        btnSaveSelected.addActionListener(this::saveAction);
        btnCancel.addActionListener(e -> dispose());

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnSaveSelected);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void populateTable() {
        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        int stt = 1;
        for (Question q : parsedQuestions) {
            String summaryText = q.getQuestionText();
            if (summaryText.length() > 100) {
                summaryText = summaryText.substring(0, 97) + "...";
            }
            Vector<Object> row = new Vector<>();
            row.add(stt++);
            row.add(summaryText);
            row.add(q.getQuestionType() != null ? q.getQuestionType() : "N/A");
            row.add(q.getDifficultyLevel() != null ? q.getDifficultyLevel() : "N/A");
            tableModel.addRow(row);
        }
    }

    private void applyAllDifficultyAction(ActionEvent e) {
        String selectedDifficulty = (String) cmbApplyAllDifficulty.getSelectedItem();
        if (selectedDifficulty != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAt(selectedDifficulty, i, 3); // Cột "Mức độ" là cột thứ 3 (index)
            }
            // Cập nhật lại danh sách parsedQuestions nếu cần, hoặc làm khi lưu
        }
    }

    private void saveAction(ActionEvent e) {
        // Ngừng chỉnh sửa cell (nếu có) để giá trị được commit
        if (questionsTable.isEditing()) {
            questionsTable.getCellEditor().stopCellEditing();
        }

        questionsToSave.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Question originalQuestion = parsedQuestions.get(i); // Lấy câu hỏi gốc
            Question questionToSave = new Question(); // Tạo đối tượng mới hoặc clone

            // Sao chép các thuộc tính từ câu hỏi gốc đã được AI phân tích
            questionToSave.setQuestionText(originalQuestion.getQuestionText());
            questionToSave.setAnswers(originalQuestion.getAnswers()); // Quan trọng: giữ lại các đáp án AI đã phân tích
            questionToSave.setQuestionType(tableModel.getValueAt(i, 2) != null ? tableModel.getValueAt(i, 2).toString() : "N/A"); // Lấy loại từ bảng (có thể AI đã gợi ý)

            // Lấy mức độ khó từ JComboBox trong bảng
            String difficulty = tableModel.getValueAt(i, 3).toString();
            questionToSave.setDifficultyLevel(difficulty);

            // Các trường khác có thể để mặc định hoặc null nếu AI không cung cấp
            // questionToSave.setNotes(originalQuestion.getNotes());
            // questionToSave.setAiSuggestedAnswer(originalQuestion.getAiSuggestedAnswer());

            questionsToSave.add(questionToSave);
        }
        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public List<Question> getQuestionsToSave() {
        return questionsToSave;
    }
}
