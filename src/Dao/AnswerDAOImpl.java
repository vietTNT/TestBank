package Dao;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.JDBCUtil;
import model.Answer;

public class AnswerDAOImpl implements AnswerDAO {

    // Public method using its own connection
    @Override
    public Optional<Answer> findById(Integer id) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return findByIdInternal(id, conn);
        }
    }

    // Internal method for transactions
    protected Optional<Answer> findByIdInternal(Integer id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM Answers WHERE answer_id = ?";
        Answer answer = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                answer = mapResultSetToAnswer(rs);
            }
        }
        return Optional.ofNullable(answer);
    }
    
    @Override
    public List<Answer> findAll() throws SQLException {
        List<Answer> answers = new ArrayList<>();
        String sql = "SELECT * FROM Answers";
        try (Connection conn = JDBCUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                answers.add(mapResultSetToAnswer(rs));
            }
        }
        return answers;
    }
    
    @Override
    public List<Answer> findByQuestionId(int questionId) throws SQLException {
         try (Connection conn = JDBCUtil.getConnection()) {
            return findByQuestionIdInternal(questionId, conn);
        }
    }

    protected List<Answer> findByQuestionIdInternal(int questionId, Connection conn) throws SQLException {
        List<Answer> answers = new ArrayList<>();
        String sql = "SELECT * FROM Answers WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                answers.add(mapResultSetToAnswer(rs));
            }
        }
        return answers;
    }


    @Override
    public boolean save(Answer answer) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            // For non-transactional save, or if AnswerDAO is simple and doesn't need to participate in QuestionDAO's transaction
            return saveInternal(answer, conn);
        }
    }

    // Internal save for transactional use by QuestionDAO
    protected boolean saveInternal(Answer answer, Connection conn) throws SQLException {
        if (answer.getAnswerId() == 0 || !findByIdInternal(answer.getAnswerId(), conn).isPresent()) {
            return insertInternal(answer, conn);
        } else {
            return updateInternal(answer, conn);
        }
    }

    protected boolean insertInternal(Answer answer, Connection conn) throws SQLException {
        String sql = "INSERT INTO Answers (question_id, answer_text, is_correct, explanation) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, answer.getQuestionId());
            pstmt.setString(2, answer.getAnswerText());
            pstmt.setBoolean(3, answer.isCorrect());
            pstmt.setString(4, answer.getExplanation());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        answer.setAnswerId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    // Not directly exposed by DataAccessObject
    @Override
    public boolean update(Answer answer) throws SQLException {
         try (Connection conn = JDBCUtil.getConnection()) {
            return updateInternal(answer, conn);
        }
    }

    protected boolean updateInternal(Answer answer, Connection conn) throws SQLException {
        String sql = "UPDATE Answers SET question_id = ?, answer_text = ?, is_correct = ?, explanation = ? WHERE answer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, answer.getQuestionId());
            pstmt.setString(2, answer.getAnswerText());
            pstmt.setBoolean(3, answer.isCorrect());
            pstmt.setString(4, answer.getExplanation());
            pstmt.setInt(5, answer.getAnswerId());
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return deleteByIdInternal(id, conn);
        }
    }
    
    protected boolean deleteByIdInternal(Integer id, Connection conn) throws SQLException {
        String sql = "DELETE FROM Answers WHERE answer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
    
    @Override
    public boolean deleteByQuestionId(int questionId) throws SQLException {
         try (Connection conn = JDBCUtil.getConnection()) {
            return deleteByQuestionIdInternal(questionId, conn);
        }
    }

    protected boolean deleteByQuestionIdInternal(int questionId, Connection conn) throws SQLException {
        String sql = "DELETE FROM Answers WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(Answer answer) throws SQLException {
        if (answer == null || answer.getAnswerId() == 0) return false;
        return deleteById(answer.getAnswerId());
    }

    private Answer mapResultSetToAnswer(ResultSet rs) throws SQLException {
        Answer answer = new Answer();
        answer.setAnswerId(rs.getInt("answer_id"));
        answer.setQuestionId(rs.getInt("question_id"));
        answer.setAnswerText(rs.getString("answer_text"));
        answer.setCorrect(rs.getBoolean("is_correct"));
        answer.setExplanation(rs.getString("explanation"));
        return answer;
    }
}
