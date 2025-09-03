package Controller;

import javax.swing.*;

import Dao.AnswerDAO;
import Dao.AudioFileDAO;
import Dao.ImageFileDAO;
import Dao.QuestionDAO;
import Dao.QuestionDAOImpl;
import View.MainFrame;
import View.QuestionEditorDialog;
import View.QuestionManagementPanel;
import model.AudioFile;
import model.ImageFile;
import model.Question;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // Để tạo tên file duy nhất

public class QuestionController {
    private MainFrame mainFrame;
    private QuestionManagementPanel view;
    private QuestionDAO questionDAO;
    private QuestionDAOImpl questionDAOImpl;
    private AnswerDAO answerDAO; // Cần để quản lý Answer khi lưu Question
    private AudioFileDAO audioFileDAO;
    private ImageFileDAO imageFileDAO;
    private long Max = 99999999;
    // Thư mục lưu trữ file của ứng dụng (cần được tạo nếu chưa có)
    private static final String APP_FILES_DIRECTORY = "app_data";
    private static final String AUDIO_SUBDIRECTORY = "audio";
    private static final String IMAGE_SUBDIRECTORY = "images";


    public QuestionController(MainFrame mainFrame, QuestionDAO questionDAO, AnswerDAO answerDAO, AudioFileDAO audioFileDAO, ImageFileDAO imageFileDAO) {
        this.mainFrame = mainFrame;
        this.questionDAO = questionDAO;
        this.answerDAO = answerDAO;
        this.audioFileDAO = audioFileDAO;
        this.imageFileDAO = imageFileDAO;

        // Tạo các thư mục lưu trữ nếu chưa tồn tại
        createAppDirectories();

        System.out.println("QuestionController initialized with DAOs.");
    }
    
    private void createAppDirectories() {
        try {
            Files.createDirectories(Paths.get(APP_FILES_DIRECTORY, AUDIO_SUBDIRECTORY));
            Files.createDirectories(Paths.get(APP_FILES_DIRECTORY, IMAGE_SUBDIRECTORY));
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo thư mục ứng dụng: " + e.getMessage());
            // Có thể hiển thị lỗi cho người dùng hoặc thoát ứng dụng nếu thư mục là thiết yếu
        }
    }

    public void setView(QuestionManagementPanel view) {
        this.view = view;
        if (this.view != null) {
            loadInitialQuestions();
        }
    }

    public void loadInitialQuestions() {
        if (view == null) {
            System.err.println("Lỗi: QuestionManagementPanel (view) chưa được thiết lập cho QuestionController.");
            return;
        }
        try {
            List<Question> questions = questionDAO.findAll();
            // Để hiển thị đúng trạng thái có audio/image, chúng ta cần load chúng
            for (Question q : questions) {
                q.setAudioFile(audioFileDAO.findByQuestionId(q.getQuestionId()).orElse(null));
                q.setImageFile(imageFileDAO.findByQuestionId(q.getQuestionId()).orElse(null));
            }
            view.displayQuestions(questions);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, "Lỗi tải danh sách câu hỏi từ CSDL: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openAddQuestionDialog() {
        if (mainFrame == null) {
            System.err.println("MainFrame is null, cannot open dialog.");
            return;
        }
        QuestionEditorDialog editorDialog = new QuestionEditorDialog(mainFrame, null, this);
        editorDialog.setVisible(true);

        if (editorDialog.isSaved()) {
            Question newQuestion = editorDialog.getQuestion();
            try {
                // Xử lý file trước khi lưu Question object chính
                handleAudioFileUpload(newQuestion, editorDialog.getSelectedAudioFile());
                handleImageFileUpload(newQuestion, editorDialog.getSelectedImageFile());

                boolean success = questionDAO.save(newQuestion); // DAO.save sẽ xử lý cả Answer, AudioFile, ImageFile
                
                if (success) {
                    loadInitialQuestions();
                    JOptionPane.showMessageDialog(mainFrame, "Thêm câu hỏi mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Thêm câu hỏi mới thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "Lỗi CSDL khi lưu câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "Lỗi xử lý file: " + e.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public int saveParsedQuestionsBatch(List<Question> questionsToSave, String ignoredDefaultDifficultyLevel) {
        if (questionsToSave == null || questionsToSave.isEmpty()) {
            System.out.println("[QuestionController LOG] Không có câu hỏi nào để lưu hàng loạt.");
            return 0;
        }
        int successfullySavedCount = 0;
        for (Question question : questionsToSave) {
            try {
                // Mức độ khó đã được set từ ParsedQuestionsReviewDialog
                // Loại câu hỏi cũng đã được set từ AI hoặc ParsedQuestionsReviewDialog
                if (question.getDifficultyLevel() == null || question.getDifficultyLevel().isEmpty() || question.getDifficultyLevel().equals("N/A")) {
                    question.setDifficultyLevel("N/A"); // Đảm bảo có giá trị
                }
                if (question.getQuestionType() == null || question.getQuestionType().isEmpty() || question.getQuestionType().equals("Chưa xác định") || question.getQuestionType().equals("Chưa phân loại")) {
                    if (question.getAnswers() != null && !question.getAnswers().isEmpty()) {
                        question.setQuestionType("Trắc nghiệm");
                    } else {
                        question.setQuestionType("Chưa phân loại");
                    }
                }
                question.setAudioFile(null); // AI không trích xuất audio/image
                question.setImageFile(null);

                boolean success = questionDAO.save(question);
                if (success) {
                  successfullySavedCount++;
                  System.out.println("[QuestionController LOG] Đã lưu câu hỏi ID (mới hoặc cập nhật): " + question.getQuestionId() + " - Nội dung: " + question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)) + "...");
              } else {
                  System.err.println("[QuestionController ERROR] Lưu thất bại cho câu hỏi: " + question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)) + "...");
              }
          } catch (SQLException e) {
              System.err.println("[QuestionController ERROR] Lỗi SQL khi lưu câu hỏi: " + question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)) + "... - " + e.getMessage());
              e.printStackTrace();
              // Có thể dừng lại hoặc tiếp tục với các câu hỏi khác tùy theo yêu cầu
          } catch (Exception ex) { // Bắt các lỗi không mong muốn khác
               System.err.println("[QuestionController ERROR] Lỗi không xác định khi lưu câu hỏi: " + question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)) + "... - " + ex.getMessage());
              ex.printStackTrace();
          }
      }
      System.out.println("[QuestionController LOG] Hoàn tất lưu hàng loạt, đã lưu thành công: " + successfullySavedCount + "/" + questionsToSave.size());
      return successfullySavedCount;
    }
    
    public void openEditQuestionDialog(int questionId) {
        if (mainFrame == null) return;
        try {
            Question questionToEdit = questionDAO.getQuestionWithDetails(questionId); // Lấy đầy đủ thông tin

            if (questionToEdit != null) {
                QuestionEditorDialog editorDialog = new QuestionEditorDialog(mainFrame, questionToEdit, this);
                // Truyền file hiện tại vào dialog để nó có thể hiển thị và so sánh
                if (questionToEdit.getAudioFile() != null && questionToEdit.getAudioFile().getFilePath() != null) {
                    editorDialog.setSelectedAudioFile(new File(questionToEdit.getAudioFile().getFilePath()));
                }
                if (questionToEdit.getImageFile() != null && questionToEdit.getImageFile().getFilePath() != null) {
                    editorDialog.setSelectedImageFile(new File(questionToEdit.getImageFile().getFilePath()));
                }
                editorDialog.setVisible(true);

                if (editorDialog.isSaved()) {
                    Question updatedQuestion = editorDialog.getQuestion();
                    
                    // Xử lý file: nếu file mới được chọn, xóa file cũ (nếu có) và lưu file mới
                    // Nếu file được clear, xóa file cũ.
                    handleAudioFileUpload(updatedQuestion, editorDialog.getSelectedAudioFile());
                    handleImageFileUpload(updatedQuestion, editorDialog.getSelectedImageFile());
                    
                    boolean success = questionDAO.save(updatedQuestion); // DAO.save sẽ tự biết là update
                    
                    if (success) {
                        loadInitialQuestions();
                        JOptionPane.showMessageDialog(mainFrame, "Cập nhật câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "Cập nhật câu hỏi thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy câu hỏi với ID: " + questionId, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi CSDL khi tải câu hỏi để sửa: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame, "Lỗi xử lý file khi mở form sửa: " + e.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteQuestion(int questionId) {
        if (view == null || mainFrame == null) return;
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Bạn có chắc chắn muốn xóa câu hỏi này (ID: " + questionId + ")?\nTất cả dữ liệu liên quan (đáp án, file) cũng sẽ bị xóa.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // Lấy thông tin file trước khi xóa record khỏi DB để có thể xóa file vật lý
                Question questionToDelete = questionDAO.getQuestionWithDetails(questionId);

                boolean success = questionDAO.deleteById(questionId); // DAO nên xử lý cascade delete cho answers, audio, image records

                if (success) {
                    // Xóa file vật lý sau khi xóa thành công khỏi DB
                    if (questionToDelete != null) {
                        if (questionToDelete.getAudioFile() != null) {
                            deletePhysicalFile(questionToDelete.getAudioFile().getFilePath());
                        }
                        if (questionToDelete.getImageFile() != null) {
                            deletePhysicalFile(questionToDelete.getImageFile().getFilePath());
                        }
                    }
                    loadInitialQuestions();
                    JOptionPane.showMessageDialog(mainFrame, "Xóa câu hỏi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainFrame, "Xóa câu hỏi thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame, "Lỗi CSDL khi xóa câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void searchQuestions(String searchTerm) {
        if (view == null) return;
        try {
            List<Question> searchResults;
				searchResults = questionDAO.searchByText(searchTerm);
             for (Question q : searchResults) { // Load media info cho kết quả tìm kiếm
                q.setAudioFile(audioFileDAO.findByQuestionId(q.getQuestionId()).orElse(null));
                q.setImageFile(imageFileDAO.findByQuestionId(q.getQuestionId()).orElse(null));
            }
            view.displayQuestions(searchResults);
            if (searchResults.isEmpty()) {
                 JOptionPane.showMessageDialog(view, "Không tìm thấy kết quả nào cho: '" + searchTerm + "'", "Kết quả tìm kiếm", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(view, "Lỗi CSDL khi tìm kiếm câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // --- Xử lý File ---
    private void handleAudioFileUpload(Question question, File selectedFileFromDialog) throws IOException {
    	 if (question == null) {
    	        System.err.println("Lỗi: Đối tượng Question là null trong handleAudioFileUpload.");
    	        return;
    	    }
    		
    	AudioFile existingAudio = question.getAudioFile(); // AudioFile object từ DB (nếu là edit)
    	if (selectedFileFromDialog != null && selectedFileFromDialog.exists()) {
            // Người dùng đã chọn một file mới hoặc giữ file cũ (nếu đường dẫn tuyệt đối giống hệt)
            String newSelectedFilePathAbsolute = selectedFileFromDialog.getAbsolutePath();
            String newFileNameInApp = selectedFileFromDialog.getName(); // Tên gốc, copyFileToAppDirectory sẽ tạo tên duy nhất

            // Kiểm tra xem file được chọn có phải là file hiện tại đã lưu trong app_data không
            // (nếu existingAudio có đường dẫn và đường dẫn đó khớp với file được chọn)
            boolean isSameAsExistingInApp = existingAudio != null &&
                                            existingAudio.getFilePath() != null &&
                                            new File(existingAudio.getFilePath()).getAbsolutePath().equals(newSelectedFilePathAbsolute);


            if (isSameAsExistingInApp) {
                // File không thay đổi, không cần làm gì cả
                System.out.println("File âm thanh không thay đổi, giữ nguyên: " + existingAudio.getFilePath());
                // Đảm bảo đối tượng AudioFile vẫn được gắn vào question nếu nó là file cũ
                question.setAudioFile(existingAudio);
            } else {
                // File mới được chọn hoặc file hiện tại được thay thế
                // Xóa file cũ trong thư mục app nếu có và khác file mới
                if (existingAudio != null && existingAudio.getFilePath() != null) {
                    deletePhysicalFile(existingAudio.getFilePath());
                }

                // Sao chép file mới vào thư mục app (copyFileToAppDirectory sẽ tạo tên duy nhất nếu cần)
                String finalPathInApp = copyFileToAppDirectory(selectedFileFromDialog, AUDIO_SUBDIRECTORY);

                AudioFile audioToSave = new AudioFile(); 
                if(existingAudio != null && existingAudio.getAudioFileId() != 0 && !isSameAsExistingInApp){
        
                }

                audioToSave.setFilePath(finalPathInApp); // Đường dẫn trong thư mục app_data
                audioToSave.setFileName(selectedFileFromDialog.getName()); // Tên file gốc
                audioToSave.setDescription("Audio cho câu hỏi " + question.getQuestionId()); // Ví dụ mô tả
                // questionId sẽ được DAO gán khi lưu Question tổng thể
                question.setAudioFile(audioToSave);
                System.out.println("Đã chuẩn bị lưu file âm thanh mới: " + finalPathInApp);
            }
        } else { // Người dùng đã xóa lựa chọn file (selectedFileFromDialog là null)
            if (existingAudio != null && existingAudio.getFilePath() != null) {
                deletePhysicalFile(existingAudio.getFilePath()); // Xóa file vật lý
                System.out.println("Đã xóa file âm thanh vật lý: " + existingAudio.getFilePath());
            }
            question.setAudioFile(null); // Đánh dấu để xóa record AudioFile trong DB (DAO sẽ xử lý)
            System.out.println("Đã xóa lựa chọn file âm thanh cho câu hỏi.");
        }
    }

    private void handleImageFileUpload(Question question, File selectedFileFromDialog) throws IOException {
        ImageFile existingImage = question.getImageFile();

        if (selectedFileFromDialog != null) {
            String newFilePathInApp = Paths.get(APP_FILES_DIRECTORY, IMAGE_SUBDIRECTORY, selectedFileFromDialog.getName()).toString();

            if (existingImage == null || !selectedFileFromDialog.getAbsolutePath().equals(existingImage.getFilePath())) {
                if (existingImage != null && existingImage.getFilePath() != null && !existingImage.getFilePath().equals(newFilePathInApp)) {
                    deletePhysicalFile(existingImage.getFilePath());
                }
                String finalPath = copyFileToAppDirectory(selectedFileFromDialog, IMAGE_SUBDIRECTORY);
                
                ImageFile imageToSave = (existingImage == null) ? new ImageFile() : existingImage;
                imageToSave.setFilePath(finalPath);
                imageToSave.setFileName(selectedFileFromDialog.getName());
                question.setImageFile(imageToSave);
            }
        } else {
            if (existingImage != null && existingImage.getFilePath() != null) {
                deletePhysicalFile(existingImage.getFilePath());
            }
            question.setImageFile(null);
        }
    }

    private String copyFileToAppDirectory(File sourceFile, String subDirectory) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("File nguồn không tồn tại.");
        }
        // Tạo tên file duy nhất để tránh trùng lặp (tùy chọn)
        String originalFileName = sourceFile.getName();
        String extension = "";
        int i = originalFileName.lastIndexOf('.');
        if (i > 0) {
            extension = originalFileName.substring(i); // .png, .mp3
            originalFileName = originalFileName.substring(0, i);
        }
        String uniqueFileName = originalFileName + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path targetDirectory = Paths.get(APP_FILES_DIRECTORY, subDirectory);
        Files.createDirectories(targetDirectory); // Đảm bảo thư mục đích tồn tại
        Path targetPath = targetDirectory.resolve(uniqueFileName);
        
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Đã sao chép file vào: " + targetPath.toAbsolutePath().toString());
        return targetPath.toAbsolutePath().toString(); // Trả về đường dẫn tuyệt đối đã lưu
    }

    private void deletePhysicalFile(String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            File fileToDelete = new File(filePath);
            if (fileToDelete.exists() && fileToDelete.isFile()) { // Chỉ xóa nếu là file
                try {
                    if (Files.deleteIfExists(fileToDelete.toPath())) {
                        System.out.println("Đã xóa file vật lý: " + filePath);
                    } else {
                        System.err.println("Không thể xóa file vật lý (có thể đang được sử dụng): " + filePath);
                    }
                } catch (IOException e) {
                     System.err.println("Lỗi I/O khi xóa file vật lý: " + filePath + " - " + e.getMessage());
                }
            } else {
                // System.err.println("File vật lý không tồn tại hoặc không phải là file: " + filePath);
            }
        }
    }

    // Getter cho MainFrame (nếu cần cho các dialog con không được tạo từ đây)
    public Frame getMainFrame() {
        return getMainFrame();
    }
}
