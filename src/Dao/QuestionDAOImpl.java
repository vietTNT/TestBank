

package Dao;

import Dao.AnswerDAO;
import Dao.AudioFileDAO;
import Dao.ImageFileDAO;
import Dao.QuestionDAO;
import database.JDBCUtil; // Lớp tiện ích kết nối CSDL của bạn
import model.Answer;
import model.AudioFile;
import model.ImageFile;
import model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class QuestionDAOImpl implements QuestionDAO {

    private AnswerDAO answerDAO;
    private AudioFileDAO audioFileDAO;
    private ImageFileDAO imageFileDAO;

    // Constructor: Khởi tạo các DAO phụ thuộc
    public QuestionDAOImpl() {
        // Giả sử bạn có các lớp Impl này và chúng có các phương thức internal
        // chấp nhận Connection làm tham số cho các thao tác trong transaction.
        this.answerDAO = new AnswerDAOImpl();
        this.audioFileDAO = new AudioFileDAOImpl();
        this.imageFileDAO = new ImageFileDAOImpl();
    }

    @Override
    public Question getQuestionWithDetails(int questionId) throws SQLException {
        Optional<Question> questionOpt = findById(questionId);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            // Các DAO này cần phương thức không quản lý transaction riêng khi được gọi từ đây
            question.setAnswers(answerDAO.findByQuestionId(questionId)); // Giả sử findByQuestionId không tự commit/close conn
            question.setAudioFile(audioFileDAO.findByQuestionId(questionId).orElse(null));
            question.setImageFile(imageFileDAO.findByQuestionId(questionId).orElse(null));
            return question;
        }
        return null;
    }
    
    @Override
    public Optional<Question> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM Questions WHERE question_id = ?";
        Question question = null;
        // Sử dụng try-with-resources cho Connection, PreparedStatement, ResultSet
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (conn == null) {
                System.err.println("Không thể kết nối đến CSDL trong QuestionDAOImpl.findById()");
                return Optional.empty();
            }
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    question = mapResultSetToQuestion(rs);
                }
            }
        } // Connection, PreparedStatement, ResultSet sẽ tự động được đóng
        return Optional.ofNullable(question);
    }
    
    // Phương thức internal findById để dùng trong transaction
    private Optional<Question> findByIdInternal(Integer id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM Questions WHERE question_id = ?";
        Question question = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    question = mapResultSetToQuestion(rs);
                }
            }
        }
        return Optional.ofNullable(question);
    }


    @Override
    public List<Question> findAll() throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM Questions ORDER BY question_id DESC";
        try (Connection conn = JDBCUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (conn == null) {
                 System.err.println("Không thể kết nối đến CSDL trong QuestionDAOImpl.findAll()");
                return questions;
            }
            while (rs.next()) {
                questions.add(mapResultSetToQuestion(rs));
            }
        }
        return questions;
    }

    @Override
    public boolean save(Question question) throws SQLException {
        Connection conn = null;
        boolean success = false;
        try {
            conn = JDBCUtil.getConnection();
            if (conn == null) {
                System.err.println("Không thể kết nối đến CSDL trong QuestionDAOImpl.save()");
                return false;
            }
            conn.setAutoCommit(false); // Bắt đầu transaction

            // Kiểm tra xem đây là INSERT hay UPDATE
            // Nếu questionId = 0 hoặc không tìm thấy questionId đó trong DB, thì là INSERT
            boolean isNewQuestion = (question.getQuestionId() == 0 || 
                                     !findByIdInternal(question.getQuestionId(), conn).isPresent());

            if (isNewQuestion) {
                success = insertInternal(question, conn); // Lưu Question chính
            } else {
                success = updateInternal(question, conn); // Cập nhật Question chính
            }

            if (success) {
                // Xử lý lưu các đối tượng liên quan (Answers, AudioFile, ImageFile)
                // Các phương thức này cũng phải sử dụng `conn` đã có để đảm bảo transaction
                saveOrUpdateAnswersInternal(question, conn);
                saveOrUpdateAudioFileInternal(question, conn);
                saveOrUpdateImageFileInternal(question, conn);
                
                conn.commit(); // Commit transaction nếu tất cả thành công
            } else {
                conn.rollback(); // Rollback nếu lưu Question chính thất bại
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); 
                } catch (SQLException ex) {
                    System.err.println("Lỗi khi rollback transaction: " + ex.getMessage());
                }
            }
            System.err.println("SQL Exception trong QuestionDAOImpl.save(): " + e.getMessage());
            e.printStackTrace();
            throw e; 
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                } catch (SQLException ex) {
                    System.err.println("Lỗi khi reset auto-commit: " + ex.getMessage());
                }
                JDBCUtil.closeConnection(conn); // Đóng kết nối
            }
        }
        return success;
    }
    
    private boolean insertInternal(Question question, Connection conn) throws SQLException {
        String sql = "INSERT INTO Questions (question_text, question_type, difficulty_level, ai_suggested_answer, notes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, question.getQuestionText());
            pstmt.setString(2, question.getQuestionType());
            pstmt.setString(3, question.getDifficultyLevel());
            pstmt.setString(4, question.getAiSuggestedAnswer());
            pstmt.setString(5, question.getNotes());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        question.setQuestionId(generatedKeys.getInt(1)); // Lấy ID tự tăng
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean updateInternal(Question question, Connection conn) throws SQLException {
        String sql = "UPDATE Questions SET question_text = ?, question_type = ?, difficulty_level = ?, ai_suggested_answer = ?, notes = ?, updated_at = CURRENT_TIMESTAMP WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, question.getQuestionText());
            pstmt.setString(2, question.getQuestionType());
            pstmt.setString(3, question.getDifficultyLevel());
            pstmt.setString(4, question.getAiSuggestedAnswer());
            pstmt.setString(5, question.getNotes());
            pstmt.setInt(6, question.getQuestionId());
            return pstmt.executeUpdate() > 0;
        }
    }
    
    private void saveOrUpdateAnswersInternal(Question question, Connection conn) throws SQLException {
        // Xóa tất cả các đáp án cũ của câu hỏi này trước khi thêm mới (đơn giản hóa logic update)
        // Giả sử AnswerDAOImpl có phương thức deleteByQuestionIdInternal(int questionId, Connection conn)
        ((AnswerDAOImpl)answerDAO).deleteByQuestionIdInternal(question.getQuestionId(), conn); 
        
        if (question.getAnswers() != null) { // Chỉ lưu đáp án cho câu trắc nghiệm
            for (Answer answer : question.getAnswers()) {
                answer.setQuestionId(question.getQuestionId()); // Đảm bảo questionId được set
                // Giả sử AnswerDAOImpl có phương thức saveInternal(Answer answer, Connection conn)
                ((AnswerDAOImpl)answerDAO).saveInternal(answer, conn); 
            }
        }
    }

    private void saveOrUpdateAudioFileInternal(Question question, Connection conn) throws SQLException {
        // Lấy AudioFile hiện tại từ CSDL (nếu có) để so sánh
        Optional<AudioFile> existingAudioOpt = ((AudioFileDAOImpl)audioFileDAO).findByQuestionIdInternal(question.getQuestionId(), conn);
        AudioFile audioToSave = question.getAudioFile(); // File mới từ đối tượng Question

        if (audioToSave != null && audioToSave.getFilePath() != null && !audioToSave.getFilePath().isEmpty()) {
            audioToSave.setQuestionId(question.getQuestionId()); // Đảm bảo questionId được set
            if (existingAudioOpt.isPresent()) { // Đã có audio file cũ
                AudioFile oldAudio = existingAudioOpt.get();
                // Nếu file path thay đổi hoặc là một đối tượng AudioFile mới hoàn toàn (chưa có ID)
                if (audioToSave.getAudioFileId() == 0 || !oldAudio.getFilePath().equals(audioToSave.getFilePath())) {
                    // Xóa bản ghi cũ (DAO không xóa file vật lý)
                    ((AudioFileDAOImpl)audioFileDAO).deleteByIdInternal(oldAudio.getAudioFileId(), conn);
                    // Lưu bản ghi mới
                    ((AudioFileDAOImpl)audioFileDAO).saveInternal(audioToSave, conn); // saveInternal sẽ là INSERT
                } else {
                    // File path không đổi, có thể cập nhật các thông tin khác nếu cần
                    audioToSave.setAudioFileId(oldAudio.getAudioFileId()); // Giữ lại ID cũ
                    ((AudioFileDAOImpl)audioFileDAO).updateInternal(audioToSave, conn); // Giả sử có updateInternal
                }
            } else { // Chưa có audio file cũ, thêm mới
                ((AudioFileDAOImpl)audioFileDAO).saveInternal(audioToSave, conn);
            }
        } else { // Người dùng muốn xóa audio file (audioToSave là null hoặc filePath rỗng)
            if (existingAudioOpt.isPresent()) {
                ((AudioFileDAOImpl)audioFileDAO).deleteByIdInternal(existingAudioOpt.get().getAudioFileId(), conn);
            }
        }
    }

    private void saveOrUpdateImageFileInternal(Question question, Connection conn) throws SQLException {
        Optional<ImageFile> existingImageOpt = ((ImageFileDAOImpl)imageFileDAO).findByQuestionIdInternal(question.getQuestionId(), conn);
        ImageFile imageToSave = question.getImageFile();

        if (imageToSave != null && imageToSave.getFilePath() != null && !imageToSave.getFilePath().isEmpty()) {
            imageToSave.setQuestionId(question.getQuestionId());
            if (existingImageOpt.isPresent()) {
                ImageFile oldImage = existingImageOpt.get();
                if (imageToSave.getImageFileId() == 0 || !oldImage.getFilePath().equals(imageToSave.getFilePath())) {
                    ((ImageFileDAOImpl)imageFileDAO).deleteByIdInternal(oldImage.getImageFileId(), conn);
                    ((ImageFileDAOImpl)imageFileDAO).saveInternal(imageToSave, conn);
                } else {
                    imageToSave.setImageFileId(oldImage.getImageFileId());
                    ((ImageFileDAOImpl)imageFileDAO).updateInternal(imageToSave, conn);
                }
            } else {
                ((ImageFileDAOImpl)imageFileDAO).saveInternal(imageToSave, conn);
            }
        } else {
            if (existingImageOpt.isPresent()) {
                ((ImageFileDAOImpl)imageFileDAO).deleteByIdInternal(existingImageOpt.get().getImageFileId(), conn);
            }
        }
    }
    
    // Phương thức update() này có thể không cần thiết nếu save() đã xử lý cả insert và update.
    // Tuy nhiên, nếu interface DataAccessObject yêu cầu, bạn cần triển khai nó.
    @Override
    public boolean update(Question question) throws SQLException {
        if (question.getQuestionId() == 0) {
            // Không thể update nếu không có ID, có thể gọi save() để thực hiện insert
            // hoặc ném lỗi tùy theo logic bạn muốn.
            // return save(question); // Hoặc:
            throw new SQLException("Cannot update Question with ID 0. Use save() for new questions.");
        }
        // Gọi save để xử lý logic update trong transaction
        return save(question);
    }


    @Override
    public boolean deleteById(Integer id) throws SQLException {
        Connection conn = null;
        boolean success = false;
        try {
            conn = JDBCUtil.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false);

            // Xóa các bản ghi phụ thuộc trước (nếu không dùng ON DELETE CASCADE trong DB cho chúng)
            // QuestionDAOImpl của bạn đã có logic này, nên không cần lặp lại ở đây nếu nó đúng.
            // Tuy nhiên, để chắc chắn, đảm bảo các DAO con được gọi với `conn`
            ((AnswerDAOImpl)answerDAO).deleteByQuestionIdInternal(id, conn); // Xóa tất cả answers của question này
            
            Optional<AudioFile> audioOpt = ((AudioFileDAOImpl)audioFileDAO).findByQuestionIdInternal(id, conn);
            if(audioOpt.isPresent()){
                ((AudioFileDAOImpl)audioFileDAO).deleteByIdInternal(audioOpt.get().getAudioFileId(), conn);
            }

            Optional<ImageFile> imageOpt = ((ImageFileDAOImpl)imageFileDAO).findByQuestionIdInternal(id, conn);
            if(imageOpt.isPresent()){
                ((ImageFileDAOImpl)imageFileDAO).deleteByIdInternal(imageOpt.get().getImageFileId(), conn);
            }

            // Sau đó xóa bản ghi Question chính
            String sql = "DELETE FROM Questions WHERE question_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                success = pstmt.executeUpdate() > 0;
            }

            if (success) {
                conn.commit();
            } else {
                conn.rollback(); // Nếu xóa question chính thất bại
            }
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
                JDBCUtil.closeConnection(conn);
            }
        }
        return success;
    }

    @Override
    public boolean delete(Question question) throws SQLException {
        if (question == null || question.getQuestionId() == 0) return false;
        return deleteById(question.getQuestionId());
    }

    @Override
    public List<Question> findByDifficulty(String difficultyLevel) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM Questions WHERE difficulty_level = ? ORDER BY question_id DESC";
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return questions;
            pstmt.setString(1, difficultyLevel);
            try(ResultSet rs = pstmt.executeQuery()){
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return questions;
    }

    @Override
    public List<Question> findByType(String questionType) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM Questions WHERE question_type = ? ORDER BY question_id DESC";
         try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return questions;
            pstmt.setString(1, questionType);
            try(ResultSet rs = pstmt.executeQuery()){
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return questions;
    }

    @Override
    public List<Question> searchByText(String searchText) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM Questions WHERE " +
                "LOWER(question_text) LIKE LOWER(?) OR " +
                "LOWER(question_type) LIKE LOWER(?) OR " +
                "LOWER(notes) LIKE LOWER(?) OR " +
                "LOWER(difficulty_level) LIKE LOWER(?) " +
                "ORDER BY question_id DESC";
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return questions;
            String searchTerm = "%" + searchText + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            pstmt.setString(3, searchTerm);
            pstmt.setString(4, searchTerm);
            try(ResultSet rs = pstmt.executeQuery()){
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }
        return questions;
    }

    @Override
    public List<Question> findRandomQuestionsByCriteria(Map<String, Object> criteria, int limit) throws SQLException {
        List<Question> questions = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Questions WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (criteria != null) {
            String difficultyLevel = (String) criteria.get("difficultyLevel");
            // Chỉ thêm điều kiện difficulty_level nếu nó không phải là "Tất cả" và không rỗng
            if (difficultyLevel != null && !difficultyLevel.equalsIgnoreCase("Tất cả") && !difficultyLevel.isEmpty()) {
                sqlBuilder.append(" AND difficulty_level = ?");
                params.add(difficultyLevel);
            }

            String questionType = (String) criteria.get("questionType");
            if (questionType != null && !questionType.isEmpty()) {
                sqlBuilder.append(" AND question_type = ?");
                params.add(questionType);
            }
            // Bạn có thể thêm các tiêu chí khác ở đây nếu cần
        }

        if (limit <= 0) {
            // Nếu không có giới hạn hoặc giới hạn không hợp lệ, không nên dùng ORDER BY RAND()
            // vì nó có thể rất chậm trên bảng lớn mà không có LIMIT.
            // Trong trường hợp này, có thể bỏ qua việc lấy ngẫu nhiên hoặc chỉ lấy không giới hạn.
            // Để đơn giản, nếu limit <=0, ta không thêm LIMIT và RAND.
        } else {
            // Cảnh báo: ORDER BY RAND() có thể chậm trên bảng lớn.
            // Xem xét các giải pháp thay thế cho CSDL cụ thể của bạn nếu hiệu năng là vấn đề.
            sqlBuilder.append(" ORDER BY RAND() LIMIT ?");
            params.add(limit);
        }

        try (Connection conn = JDBCUtil.getConnection(); //
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            if (conn == null) {
                System.err.println("Không thể kết nối CSDL trong findRandomQuestionsByCriteria");
                return questions;
            }

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs)); // Sử dụng mapResultSetToQuestion đã có trong QuestionDAOImpl
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception trong QuestionDAOImpl.findRandomQuestionsByCriteria(): " + e.getMessage());
            e.printStackTrace(); // In chi tiết lỗi
            throw e; // Ném lại ngoại lệ
        }
        return questions;
    }
    
    public Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setQuestionId(rs.getInt("question_id"));
        question.setQuestionText(rs.getString("question_text"));
        question.setQuestionType(rs.getString("question_type"));
        question.setDifficultyLevel(rs.getString("difficulty_level"));
        question.setAiSuggestedAnswer(rs.getString("ai_suggested_answer"));
        question.setNotes(rs.getString("notes"));
        question.setCreatedAt(rs.getTimestamp("created_at"));
        question.setUpdatedAt(rs.getTimestamp("updated_at"));
        return question;
    }
}