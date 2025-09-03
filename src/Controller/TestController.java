package Controller;

import View.MainFrame;
import View.TestManagementPanel;
import View.TestEditorDialog;
import Dao.TestDAO;
import Dao.QuestionDAO;
import model.Test;
import model.Answer;
import model.Question;
import util.FileExporter; // Import lớp tiện ích xuất file

//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.poi.xwpf.usermodel.XWPFDocument;


import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.awt.Insets; // Thêm import này
import java.awt.Font; 
import View.RandomTestCriteriaDialog; // Thêm import này
import java.util.Map;   
// import java.util.Map; // Nếu dùng Map cho criteria

public class TestController {
    private MainFrame mainFrame;
    private TestDAO testDAO;
    private QuestionDAO questionDAO;
    private TestManagementPanel view;

    public TestController(MainFrame mainFrame, TestDAO testDAO, QuestionDAO questionDAO) {
        this.mainFrame = mainFrame;
        this.testDAO = testDAO;
        this.questionDAO = questionDAO;
        System.out.println("TestController initialized with DAOs.");
    }

    public void setView(TestManagementPanel view) {
        this.view = view;
        if (this.view != null) {
            loadInitialTests();
        } else {
            System.err.println("TestController: setView called with null view.");
        }
    }

    public void loadInitialTests() {
        if (view == null) {
            System.err.println("TestController: Cannot load tests, view is null.");
            return;
        }
        try {
            List<Test> tests = testDAO.findAll(); // THAY ĐỔI
            view.displayTests(tests); // View cần nhận List<Test>
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, "Lỗi tải danh sách đề thi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openCreateTestDialog() {
        if (mainFrame == null) return;
        try {
            List<Question> allQuestions = questionDAO.findAll();
            TestEditorDialog editorDialog = new TestEditorDialog(mainFrame, null, this, allQuestions); 
            editorDialog.setVisible(true);

            if (editorDialog.isSaved()) {
                Test newTest = editorDialog.getTest(); 
                saveOrUpdateTest(newTest);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi tải danh sách câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openEditTestDialog(int testId) {
        if (mainFrame == null) return;
        try {
            Test testToEdit = testDAO.findById(testId).orElse(null); // THAY ĐỔI
            List<Question> allQuestions = questionDAO.findAll();

            if (testToEdit != null) {
                TestEditorDialog editorDialog = new TestEditorDialog(mainFrame, testToEdit, this, allQuestions);
                editorDialog.setVisible(true);

                if (editorDialog.isSaved()) {
                    Test updatedTest = editorDialog.getTest(); // THAY ĐỔI
                    saveOrUpdateTest(updatedTest);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy đề thi với ID: " + testId, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi tải đề thi để sửa: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void openCreateRandomizedTestDialog() {
        if (mainFrame == null) {
            System.err.println("MainFrame is null in TestController, cannot open dialog.");
            return;
        }

        RandomTestCriteriaDialog criteriaDialog = new RandomTestCriteriaDialog(mainFrame);
        criteriaDialog.setVisible(true);

        if (criteriaDialog.isConfirmed()) {
            String testName = criteriaDialog.getTestName();
            Map<String, Object> baseCriteria = criteriaDialog.getCriteria(); // Lấy difficultyLevel
            Map<String, Integer> questionCountsPerType = criteriaDialog.getQuestionCounts();

            List<Question> finalSelectedQuestions = new ArrayList<>();
            String selectedDifficulty = baseCriteria.get("difficultyLevel").toString();
            String testDescription = "Đề thi ngẫu nhiên - Cấp độ: " + selectedDifficulty;

            try {
                for (Map.Entry<String, Integer> entry : questionCountsPerType.entrySet()) {
                    String questionType = entry.getKey();
                    int countNeeded = entry.getValue();

                    if (countNeeded > 0) {
                        Map<String, Object> specificCriteria = new HashMap<>(baseCriteria);
                        specificCriteria.put("questionType", questionType);

                        List<Question> fetchedQuestions = questionDAO.findRandomQuestionsByCriteria(specificCriteria, countNeeded);

                        if (fetchedQuestions.size() < countNeeded) {
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Không đủ câu hỏi loại '" + questionType + "' (" + fetchedQuestions.size() +
                                    " câu) cho mức độ '" + selectedDifficulty + "'. Sẽ lấy tất cả câu hỏi tìm được.",
                                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                        }
                        finalSelectedQuestions.addAll(fetchedQuestions);
                    }
                }

                if (finalSelectedQuestions.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame, "Không có câu hỏi nào được chọn dựa trên tiêu chí. Không thể tạo đề.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                // Xáo trộn thứ tự chung của các câu hỏi trong đề
                Collections.shuffle(finalSelectedQuestions);
                testDescription += " - Tổng số: " + finalSelectedQuestions.size() + " câu.";

                Test randomTest = new Test(testName, testDescription);
                randomTest.setQuestionsInTest(finalSelectedQuestions);

                // Mở TestEditorDialog để người dùng xem lại và có thể chỉnh sửa trước khi lưu
                List<Question> allQuestionsForPicker = questionDAO.findAll();
                if (allQuestionsForPicker == null) allQuestionsForPicker = new ArrayList<>();

                TestEditorDialog editorDialog = new TestEditorDialog(mainFrame, randomTest, this, allQuestionsForPicker);
                editorDialog.setVisible(true);
                if (editorDialog.isSaved()) {
                    saveOrUpdateTest(editorDialog.getTest());
                }

            } catch (SQLException e) {
                handleSQLException(e, "Lỗi khi tạo đề thi ngẫu nhiên từ CSDL");
            }
        }
    }
    private void saveOrUpdateTest(Test test) { // THAY ĐỔI
    	 if (test == null) {
             JOptionPane.showMessageDialog(mainFrame, "Dữ liệu đề thi không hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
             return;
         }
    	try {
            boolean success = testDAO.save(test);
            if (success) {
                loadInitialTests();
                JOptionPane.showMessageDialog(mainFrame, 
                    (test.getTestId() == 0 || testDAO.findById(test.getTestId()).isEmpty() ? "Tạo đề thi thành công!" : "Cập nhật đề thi thành công!"), 
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Lưu đề thi thất bại.", "Lỗi Lưu Trữ", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi CSDL khi lưu đề thi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteTest(int testId) {
        if (view == null || mainFrame == null) return;
        String testName = "";
        try {
            Test testToDelete = testDAO.findById(testId).orElse(null);
            if (testToDelete != null) {
                testName = testToDelete.getTestName();
            }
        } catch (SQLException e) {
            // Bỏ qua lỗi ở đây, chỉ là để lấy tên
        }
        
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Bạn có chắc chắn muốn xóa đề thi này (ID: " + testId + ")?\nCác câu hỏi liên kết cũng sẽ bị gỡ khỏi đề thi này.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = testDAO.deleteById(testId);
                if (success) {
                    loadInitialTests();
                    JOptionPane.showMessageDialog(mainFrame, "Xóa đề thi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Xóa đề thi thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "Lỗi CSDL khi xóa đề thi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void searchTests(String searchTerm) {
        if (view == null) return;
        try {
            List<Test> searchResults = testDAO.searchByName(searchTerm); // THAY ĐỔI
            view.displayTests(searchResults); // View cần nhận List<Test>
            if (searchResults.isEmpty() && !searchTerm.trim().isEmpty()) {
                 JOptionPane.showMessageDialog(view, "Không tìm thấy đề thi nào cho: '" + searchTerm + "'", "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, "Lỗi CSDL khi tìm kiếm đề thi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void viewTestDetails(int testId) {
        try {
            Test test = testDAO.findById(testId).orElse(null); // THAY ĐỔI
            if (test != null) {
                 StringBuilder details = new StringBuilder("Chi tiết Đề thi:\n");
                 details.append("ID: ").append(test.getTestId()).append("\n");
                 details.append("Tên: ").append(test.getTestName()).append("\n");
                 details.append("Mô tả: ").append(test.getDescription()).append("\n");
                 if (test.getCreatedAt() != null) {
                    details.append("Ngày tạo: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(test.getCreatedAt())).append("\n");
                 }
                 details.append("Số câu hỏi: ").append(test.getQuestionsInTest() != null ? test.getQuestionsInTest().size() : 0).append("\n\n");
                 if (test.getQuestionsInTest() != null && !test.getQuestionsInTest().isEmpty()) {
                     details.append("Danh sách câu hỏi:\n");
                     for (Question q : test.getQuestionsInTest()) {
                         details.append(" - ID: ").append(q.getQuestionId()).append(", Nội dung: ")
                                .append(q.getQuestionText().length() > 50 ? q.getQuestionText().substring(0,50)+"..." : q.getQuestionText())
                                .append("\n");
                     }
                 } else {
                     details.append("Đề thi này chưa có câu hỏi nào.\n");
                 }
                JTextArea textArea = new JTextArea(details.toString());
                textArea.setWrapStyleWord(true);
                textArea.setLineWrap(true);
                textArea.setEditable(false);
                
                textArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
                textArea.setMargin(new Insets(10,10,10,10));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(500, 400));
                JOptionPane.showMessageDialog(mainFrame, scrollPane, "Chi tiết Đề thi: " + test.getTestName(), JOptionPane.INFORMATION_MESSAGE);

            } else {
                 JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy đề thi ID: " + testId, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
             JOptionPane.showMessageDialog(mainFrame, "Lỗi khi lấy chi tiết đề thi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void handleSQLException(SQLException e, String messagePrefix) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(mainFrame, messagePrefix + ": " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
    }
    
   
 // Thêm phương thức helper này vào TestController.java
    private Test createShuffledTestVersion(Test originalTest, boolean shuffleAnswersAlso) {
        Test shuffledTest = new Test();
        shuffledTest.setTestId(originalTest.getTestId()); // Giữ lại ID cho ngữ cảnh, nhưng đây là bản copy
        shuffledTest.setTestName(originalTest.getTestName() + (shuffleAnswersAlso ? " (Đã xáo trộn câu hỏi & đáp án)" : " (Đã xáo trộn câu hỏi)"));
        shuffledTest.setDescription(originalTest.getDescription());
        shuffledTest.setCreatedAt(originalTest.getCreatedAt());

        if (originalTest.getQuestionsInTest() != null) {
            List<Question> originalQuestions = new ArrayList<>(originalTest.getQuestionsInTest());
            Collections.shuffle(originalQuestions); // Xáo trộn danh sách câu hỏi

            List<Question> newShuffledQuestionList = new ArrayList<>();
            for (Question oq : originalQuestions) {
                Question newQ = new Question();
                newQ.setQuestionId(oq.getQuestionId());
                newQ.setQuestionText(oq.getQuestionText());
                newQ.setQuestionType(oq.getQuestionType());
                newQ.setDifficultyLevel(oq.getDifficultyLevel());
                newQ.setAiSuggestedAnswer(oq.getAiSuggestedAnswer());
                newQ.setNotes(oq.getNotes());
                // Sao chép AudioFile và ImageFile nếu có
                if (oq.getAudioFile() != null) {
                    newQ.setAudioFile(oq.getAudioFile());
                }
                if (oq.getImageFile() != null) {
                    newQ.setImageFile(oq.getImageFile());
                }

                if (shuffleAnswersAlso && "Trắc nghiệm".equals(oq.getQuestionType()) && oq.getAnswers() != null) {
                    List<Answer> originalAnswers = new ArrayList<>(oq.getAnswers());
                    Collections.shuffle(originalAnswers); // Xáo trộn các đáp án của câu hỏi này
                    newQ.setAnswers(originalAnswers);
                } else {
                    newQ.setAnswers(oq.getAnswers()); // Giữ nguyên thứ tự đáp án nếu không yêu cầu
                }
                newShuffledQuestionList.add(newQ);
            }
            shuffledTest.setQuestionsInTest(newShuffledQuestionList);
        }
        return shuffledTest;
    }
    
 // Sửa đổi các phương thức export, ví dụ exportTestToPdfWithOptions:
    public void exportTestToPdfWithOptions(int testId, boolean includeAnswers) {
        if (mainFrame == null) return;
        try {
            Test originalTest = testDAO.findById(testId).orElse(null); //
            if (originalTest == null) {
                JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy đề thi để xuất PDF.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JCheckBox shuffleCheckBox = new JCheckBox("Xáo trộn câu hỏi và đáp án?");
            Object[] params = {"Bạn có muốn bao gồm đáp án không?", includeAnswers ? "Có" : "Không (Chỉ đề)", shuffleCheckBox};
            // Thay vì dùng includeAnswers trực tiếp, có thể làm 2 nút riêng biệt trong UI
            // Ở đây giả sử includeAnswers là một trạng thái đã biết.

            // Tạo dialog tùy chỉnh hơn để thêm checkbox "Xáo trộn"
            JPanel exportOptionsPanel = new JPanel(new GridLayout(0, 1));
            JCheckBox cbIncludeAnswers = new JCheckBox("Bao gồm đáp án ở cuối file?", includeAnswers);
            JCheckBox cbShuffle = new JCheckBox("Xáo trộn câu hỏi & đáp án?");
            exportOptionsPanel.add(cbIncludeAnswers);
            exportOptionsPanel.add(cbShuffle);

            int optionResult = JOptionPane.showConfirmDialog(mainFrame, exportOptionsPanel, "Tùy chọn xuất PDF", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (optionResult == JOptionPane.OK_OPTION) {
                boolean finalIncludeAnswers = cbIncludeAnswers.isSelected();
                boolean finalShuffle = cbShuffle.isSelected();

                Test testToExport = finalShuffle ? createShuffledTestVersion(originalTest, true) : originalTest;

                JFileChooser fileChooser = new JFileChooser();
                String defaultFileName = testToExport.getTestName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".pdf";
                fileChooser.setDialogTitle("Lưu Đề Thi PDF");
                fileChooser.setSelectedFile(new File(defaultFileName));
                fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));

                if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
                        fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".pdf");
                    }
                    FileExporter.exportTestToPdf(testToExport, fileToSave, finalIncludeAnswers); //
                    JOptionPane.showMessageDialog(mainFrame, "Xuất PDF thành công!\nĐã lưu tại: " + fileToSave.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            handleSQLException(e, "Lỗi CSDL khi xuất PDF"); //
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi ghi file PDF: " + e.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void exportTestToDocxWithOptions(int testId, boolean includeAnswers) { // Đổi tên phương thức
        if (mainFrame == null) return;
         try {
            Test test = testDAO.findById(testId).orElse(null);
            if (test == null) {
                JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy đề thi để xuất DOCX.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (test != null && test.getQuestionsInTest() != null) {
                for (Question q : test.getQuestionsInTest()) {
                    // Đảm bảo mỗi question có answers của nó
                    Question questionWithDetails = questionDAO.getQuestionWithDetails(q.getQuestionId());
                    if (questionWithDetails != null) {
                        q.setAnswers(questionWithDetails.getAnswers());
                    }
                }
            }
            JFileChooser fileChooser = new JFileChooser();
            String defaultFileName = test.getTestName().replaceAll("[^a-zA-Z0-9.-]", "_") + (includeAnswers ? "_co_dap_an" : "") + ".docx";
            fileChooser.setDialogTitle("Lưu Đề Thi DOCX" + (includeAnswers ? " (Kèm Đáp Án)" : ""));
            fileChooser.setSelectedFile(new File(defaultFileName));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Word Documents (*.docx)", "docx"));

            if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                 if (!fileToSave.getName().toLowerCase().endsWith(".docx")) {
                    fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".docx");
                }
                FileExporter.exportTestToDocx(test, fileToSave, includeAnswers); // Truyền includeAnswers
                JOptionPane.showMessageDialog(mainFrame, "Xuất DOCX thành công!\nĐã lưu tại: " + fileToSave.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            handleSQLException(e, "Lỗi CSDL khi xuất DOCX");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi ghi file DOCX: " + e.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void generateAndSaveAnswerKeyFile(int testId) { // Đổi tên cho rõ ràng
        if (mainFrame == null) return;
        try {
            Test test = testDAO.findById(testId).orElse(null);
            if (test == null || test.getQuestionsInTest() == null || test.getQuestionsInTest().isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy đề thi hoặc đề thi không có câu hỏi để tạo đáp án.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Lưu File Đáp Án");
            fileChooser.setSelectedFile(new File("DapAn_" + test.getTestName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".txt"));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            // Bạn có thể thêm filter cho PDF/DOCX nếu muốn xuất đáp án ra các định dạng đó
            
            if (fileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                // String format = "txt"; // Hiện tại FileExporter.generateAndExportAnswerKey chỉ hỗ trợ txt
                FileExporter.exportTestToDocx(test, fileToSave, true); // Gọi phương thức mới
                 JOptionPane.showMessageDialog(mainFrame, "Tạo và lưu file đáp án thành công!\nĐã lưu tại: " + fileToSave.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            handleSQLException(e, "Lỗi CSDL khi tạo đáp án");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi ghi file đáp án: " + e.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
        }
    }
}