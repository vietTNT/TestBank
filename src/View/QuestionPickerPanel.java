package View;
import model.Question; // Đảm bảo import đúng

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuestionPickerPanel extends JPanel {
    private JList<Question> availableQuestionsList;
    private DefaultListModel<Question> availableQuestionsModel;
    private JList<Question> selectedQuestionsList;
    private DefaultListModel<Question> selectedQuestionsModel;

    private JButton btnAddQuestion, btnRemoveQuestion, btnMoveUp, btnMoveDown;
    private JTextField txtSearchAvailable;

    private List<Question> allQuestionsMasterList; // Danh sách đầy đủ tất cả câu hỏi

    public QuestionPickerPanel(List<Question> allAvailableQuestions, List<Question> initiallySelectedQuestions) {
        this.allQuestionsMasterList = new ArrayList<>(allAvailableQuestions);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10,10,10,10));

        // Panel câu hỏi có sẵn
        JPanel availablePanel = new JPanel(new BorderLayout(5,5));
        txtSearchAvailable = new JTextField();
        txtSearchAvailable.putClientProperty("JTextField.placeholderText", "Tìm câu hỏi trong ngân hàng...");
        txtSearchAvailable.addActionListener(this::filterAvailableQuestions);
        JButton btnFilter = new JButton(IconUtils.createImageIcon("Search.png", "Filter", 16,16));
        btnFilter.addActionListener(this::filterAvailableQuestions);
        JPanel searchBarPanel = new JPanel(new BorderLayout(5,0));
        searchBarPanel.add(txtSearchAvailable, BorderLayout.CENTER);
        searchBarPanel.add(btnFilter, BorderLayout.EAST);
        availablePanel.add(searchBarPanel, BorderLayout.NORTH);
        
        availableQuestionsModel = new DefaultListModel<>();
        updateAvailableQuestionsModel(allQuestionsMasterList, initiallySelectedQuestions);
        availableQuestionsList = new JList<>(availableQuestionsModel);
        availableQuestionsList.setCellRenderer(new QuestionListRenderer());
        availableQuestionsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availablePanel.add(new JScrollPane(availableQuestionsList), BorderLayout.CENTER);
        availablePanel.setPreferredSize(new Dimension(350, 300));


        // Panel các nút điều khiển ở giữa
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
        controlsPanel.setBorder(new EmptyBorder(0,10,0,10));
        btnAddQuestion = new JButton(IconUtils.createImageIcon("add.png", "Thêm", 16,16));
        btnRemoveQuestion = new JButton(IconUtils.createImageIcon("icon_X.png", "Gỡ bỏ", 16,16));
        btnAddQuestion.setToolTipText("Thêm câu hỏi đã chọn vào đề thi");
        btnRemoveQuestion.setToolTipText("Gỡ câu hỏi đã chọn khỏi đề thi");
        
        Dimension buttonSize = new Dimension(40,30);
        btnAddQuestion.setPreferredSize(buttonSize);
        btnRemoveQuestion.setPreferredSize(buttonSize);

        controlsPanel.add(Box.createVerticalGlue());
        controlsPanel.add(btnAddQuestion);
        controlsPanel.add(Box.createRigidArea(new Dimension(0,10)));
        controlsPanel.add(btnRemoveQuestion);
        controlsPanel.add(Box.createVerticalGlue());


        // Panel câu hỏi đã chọn cho đề thi
        JPanel selectedPanel = new JPanel(new BorderLayout(5,5));
        selectedPanel.add(new JLabel("Câu hỏi trong đề thi:", SwingConstants.CENTER), BorderLayout.NORTH);
        selectedQuestionsModel = new DefaultListModel<>();
        if (initiallySelectedQuestions != null) {
            initiallySelectedQuestions.forEach(selectedQuestionsModel::addElement);
        }
        selectedQuestionsList = new JList<>(selectedQuestionsModel);
        selectedQuestionsList.setCellRenderer(new QuestionListRenderer());
        selectedPanel.add(new JScrollPane(selectedQuestionsList), BorderLayout.CENTER);
        selectedPanel.setPreferredSize(new Dimension(350, 300));

        // Nút di chuyển thứ tự câu hỏi đã chọn
        JPanel orderButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnMoveUp = new JButton(IconUtils.createImageIcon("up.png", "Lên", 16,16));
        btnMoveDown = new JButton(IconUtils.createImageIcon("down.png", "Xuống", 16,16));
        btnMoveUp.setToolTipText("Di chuyển câu hỏi lên");
        btnMoveDown.setToolTipText("Di chuyển câu hỏi xuống");
        orderButtonsPanel.add(btnMoveUp);
        orderButtonsPanel.add(btnMoveDown);
        selectedPanel.add(orderButtonsPanel, BorderLayout.SOUTH);


        // Thêm các panel vào QuestionPickerPanel
        add(availablePanel, BorderLayout.WEST);
        add(controlsPanel, BorderLayout.CENTER);
        add(selectedPanel, BorderLayout.EAST);

        // Action Listeners
        btnAddQuestion.addActionListener(this::addSelectedToTest);
        btnRemoveQuestion.addActionListener(this::removeFromTest);
        btnMoveUp.addActionListener(this::moveQuestionUp);
        btnMoveDown.addActionListener(this::moveQuestionDown);
    }
    
    private void updateAvailableQuestionsModel(List<Question> sourceList, List<Question> excludedList) {
        availableQuestionsModel.clear();
        List<Integer> excludedIds = excludedList.stream().map(Question::getQuestionId).collect(Collectors.toList());
        sourceList.stream()
                  .filter(q -> !excludedIds.contains(q.getQuestionId()))
                  .forEach(availableQuestionsModel::addElement);
    }


    private void filterAvailableQuestions(ActionEvent e) {
        String searchTerm = txtSearchAvailable.getText().toLowerCase().trim();
        List<Question> currentlySelected = getSelectedQuestions(); // Lấy các câu đã chọn để loại trừ
        
        if (searchTerm.isEmpty()) {
            updateAvailableQuestionsModel(allQuestionsMasterList, currentlySelected);
        } else {
            List<Question> filtered = allQuestionsMasterList.stream()
                .filter(q -> q.getQuestionText().toLowerCase().contains(searchTerm) ||
                             String.valueOf(q.getQuestionId()).contains(searchTerm) ||
                             (q.getQuestionType() != null && q.getQuestionType().toLowerCase().contains(searchTerm)) ||
                             (q.getDifficultyLevel() != null && q.getDifficultyLevel().toLowerCase().contains(searchTerm))
                       )
                .collect(Collectors.toList());
            updateAvailableQuestionsModel(filtered, currentlySelected);
        }
    }

    private void addSelectedToTest(ActionEvent e) {
        List<Question> toAdd = availableQuestionsList.getSelectedValuesList();
        for (Question q : toAdd) {
            if (!selectedQuestionsModel.contains(q)) { // Tránh trùng lặp
                selectedQuestionsModel.addElement(q);
            }
            availableQuestionsModel.removeElement(q); // Xóa khỏi danh sách có sẵn
        }
    }

    private void removeFromTest(ActionEvent e) {
        List<Question> toRemove = selectedQuestionsList.getSelectedValuesList();
        for (Question q : toRemove) {
            selectedQuestionsModel.removeElement(q);
            // Thêm lại vào danh sách có sẵn nếu nó vẫn khớp với bộ lọc hiện tại
            // Hoặc đơn giản là gọi lại filterAvailableQuestions để làm mới hoàn toàn
        }
        filterAvailableQuestions(null); // Làm mới danh sách có sẵn
    }

    private void moveQuestionUp(ActionEvent e) {
        int selectedIndex = selectedQuestionsList.getSelectedIndex();
        if (selectedIndex > 0) { // Không phải là phần tử đầu tiên
            Question q = selectedQuestionsModel.remove(selectedIndex);
            selectedQuestionsModel.add(selectedIndex - 1, q);
            selectedQuestionsList.setSelectedIndex(selectedIndex - 1);
        }
    }

    private void moveQuestionDown(ActionEvent e) {
        int selectedIndex = selectedQuestionsList.getSelectedIndex();
        if (selectedIndex != -1 && selectedIndex < selectedQuestionsModel.getSize() - 1) { // Không phải là phần tử cuối
            Question q = selectedQuestionsModel.remove(selectedIndex);
            selectedQuestionsModel.add(selectedIndex + 1, q);
            selectedQuestionsList.setSelectedIndex(selectedIndex + 1);
        }
    }

    public List<Question> getSelectedQuestions() {
        List<Question> selected = new ArrayList<>();
        for (int i = 0; i < selectedQuestionsModel.getSize(); i++) {
            selected.add(selectedQuestionsModel.getElementAt(i));
        }
        return selected;
    }

    // Custom renderer để hiển thị câu hỏi trong JList
    private static class QuestionListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Question) {
                Question q = (Question) value;
                String displayText = "ID: " + q.getQuestionId() + " - " + 
                                     (q.getQuestionText().length() > 50 ? q.getQuestionText().substring(0, 50) + "..." : q.getQuestionText());
                setText(displayText);
                setToolTipText(q.getQuestionText()); // Tooltip hiển thị đầy đủ
            }
            return this;
        }
    }
}
