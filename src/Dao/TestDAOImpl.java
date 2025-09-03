package Dao;

import Dao.TestDAO;
import Dao.QuestionDAO;
import database.JDBCUtil; // Đảm bảo package database là đúng
import model.Answer;
import model.Question;
import model.Test; // THAY ĐỔI: Sử dụng model.Test

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestDAOImpl implements TestDAO {
    private QuestionDAO questionDAO;

    public TestDAOImpl() {
        this.questionDAO = new QuestionDAOImpl();
    }

    private Test mapResultSetToTest(ResultSet rs) throws SQLException {
        Test test = new Test(); // THAY ĐỔI
        test.setTestId(rs.getInt("test_id"));
        test.setTestName(rs.getString("test_name"));
        test.setDescription(rs.getString("description"));
        test.setCreatedAt(rs.getTimestamp("created_at"));
        return test;
    }

    @Override
    public Optional<Test> findById(int id) throws SQLException { // THAY ĐỔI
        String sql = "SELECT * FROM Tests WHERE test_id = ?";
        Test test = null; // THAY ĐỔI
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return Optional.empty();
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    test = mapResultSetToTest(rs);
                    test.setQuestionsInTest(getQuestionsForTest(id, conn));
                }
            }
        }
        return Optional.ofNullable(test);
    }

    @Override
    public List<Test> findAll() throws SQLException { // THAY ĐỔI
        List<Test> tests = new ArrayList<>(); // THAY ĐỔI
        String sql = "SELECT * FROM Tests ORDER BY test_id DESC";
        try (Connection conn = JDBCUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (conn == null) return tests;
            while (rs.next()) {
                Test test = mapResultSetToTest(rs); // THAY ĐỔI
                test.setQuestionsInTest(getQuestionsForTest(test.getTestId(), conn));
                tests.add(test);
            }
        }
        return tests;
    }

    @Override
    public boolean save(Test test) throws SQLException { // THAY ĐỔI
        Connection conn = null;
        boolean success = false;
        try {
            conn = JDBCUtil.getConnection();
            if (conn == null) return false;
            conn.setAutoCommit(false); 

            boolean isNew = (test.getTestId() == 0);
            String sqlTest;
            if (isNew) {
                sqlTest = "INSERT INTO Tests (test_name, description) VALUES (?, ?)";
            } else {
                sqlTest = "UPDATE Tests SET test_name = ?, description = ?, created_at = CURRENT_TIMESTAMP WHERE test_id = ?";
            }

            try (PreparedStatement pstmtTest = conn.prepareStatement(sqlTest, Statement.RETURN_GENERATED_KEYS)) {
                pstmtTest.setString(1, test.getTestName());
                pstmtTest.setString(2, test.getDescription());
                if (!isNew) {
                    pstmtTest.setInt(3, test.getTestId());
                }
                int affectedRows = pstmtTest.executeUpdate();
                success = affectedRows > 0;

                if (success && isNew) {
                    try (ResultSet generatedKeys = pstmtTest.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            test.setTestId(generatedKeys.getInt(1));
                        }
                    }
                }
            }

            if (success) {
                removeAllQuestionsFromTest(test.getTestId(), conn); 
                if (test.getQuestionsInTest() != null) {
                    int order = 1;
                    for (Question q : test.getQuestionsInTest()) {
                        addQuestionToTest(test.getTestId(), q.getQuestionId(), order++, conn);
                    }
                }
                conn.commit();
            } else {
                conn.rollback();
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
    public boolean deleteById(int id) throws SQLException {
        String sql = "DELETE FROM Tests WHERE test_id = ?";
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return false;
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public List<Test> searchByName(String name) throws SQLException { // THAY ĐỔI
        List<Test> tests = new ArrayList<>(); // THAY ĐỔI
        String sql = "SELECT * FROM Tests WHERE LOWER(test_name) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?) ORDER BY test_id DESC";
        try (Connection conn = JDBCUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (conn == null) return tests;
            String searchTerm = "%" + name + "%";
            pstmt.setString(1, searchTerm);
            pstmt.setString(2, searchTerm);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Test test = mapResultSetToTest(rs); // THAY ĐỔI
                    test.setQuestionsInTest(getQuestionsForTest(test.getTestId(), conn));
                    tests.add(test);
                }
            }
        }
        return tests;
    }

    @Override
    public List<Question> getQuestionsForTest(int testId, Connection conn) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.* FROM Questions q " +
                     "JOIN TestQuestions tq ON q.question_id = tq.question_id " +
                     "WHERE tq.test_id = ? ORDER BY tq.question_order ASC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Giả sử QuestionDAOImpl có mapResultSetToQuestion là public hoặc protected
                    // Hoặc bạn có thể định nghĩa một mapResultSetToQuestion riêng trong TestDAOImpl nếu cần
                    Question question = ((QuestionDAOImpl)questionDAO).mapResultSetToQuestion(rs); 
                    
                    Question questionWithDetails = questionDAO.getQuestionWithDetails(question.getQuestionId());
                    if (questionWithDetails != null) {
                        question.setImageFile(questionWithDetails.getImageFile());
                        // Cũng nên nạp cả answers và audioFile nếu getQuestionWithDetails làm điều đó
                        // question.setAnswers(questionWithDetails.getAnswers());
                        // question.setAudioFile(questionWithDetails.getAudioFile());
                    }
                    
                    List<Answer> answers = ((AnswerDAOImpl)new AnswerDAOImpl()).findByQuestionIdInternal(question.getQuestionId(), conn); // Tạo mới AnswerDAOImpl để gọi phương thức internal
                    question.setAnswers(answers);
                    questions.add(question);
                }
            }
        }
        return questions;
    }

    @Override
    public boolean addQuestionToTest(int testId, int questionId, int order, Connection conn) throws SQLException {
        String sql = "INSERT INTO TestQuestions (test_id, question_id, question_order) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testId);
            pstmt.setInt(2, questionId);
            pstmt.setInt(3, order);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean removeAllQuestionsFromTest(int testId, Connection conn) throws SQLException {
        String sql = "DELETE FROM TestQuestions WHERE test_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, testId);
            return pstmt.executeUpdate() >= 0;
        }
    }

	@Override
	public Optional<Test> findById(Integer id) throws SQLException {
		 if (id == null) {
	            return Optional.empty();
	        }
	        // Gọi phiên bản sử dụng int id đã được triển khai đầy đủ
	        return this.findById(id.intValue());
	}

	@Override
	public boolean deleteById(Integer id) throws SQLException {
		 if (id == null) {
	            return false;
	        }
	        // Gọi phiên bản sử dụng int id đã được triển khai đầy đủ
	        return this.deleteById(id.intValue());
	}

	@Override
	public boolean delete(Test entity) throws SQLException {
		  if (entity == null || entity.getTestId() == 0) {
	            return false;
	        }
	        return this.deleteById(entity.getTestId());
	}

	@Override
	public List<Question> findRandomQuestionsByCriteria(Map<String, Object> criteria, int limit) throws SQLException {
		// Đây là một triển khai ví dụ đơn giản.
        // Bạn cần xây dựng câu SQL động dựa trên 'criteria'.
        List<Question> questions = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Questions WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (criteria != null) {
            if (criteria.containsKey("difficultyLevel") && criteria.get("difficultyLevel") != null) {
                sqlBuilder.append(" AND difficulty_level = ?");
                params.add(criteria.get("difficultyLevel"));
            }
            if (criteria.containsKey("questionType") && criteria.get("questionType") != null) {
                sqlBuilder.append(" AND question_type = ?");
                params.add(criteria.get("questionType"));
            }
            // Thêm các tiêu chí khác nếu cần
        }

        sqlBuilder.append(" ORDER BY RAND() LIMIT ?"); // Hoặc DBMS_RANDOM.VALUE trên Oracle, NEWID() trên SQL Server
        params.add(limit);

        try (Connection conn = JDBCUtil.getConnection(); //
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            if (conn == null) return questions;

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                QuestionDAOImpl questionDAOImpl = (QuestionDAOImpl) this.questionDAO; //
                while (rs.next()) {
                    questions.add(questionDAOImpl.mapResultSetToQuestion(rs)); //
                }
            }
        }
        return questions;
    }
}