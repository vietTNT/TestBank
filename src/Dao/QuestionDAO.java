package Dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import model.Question;
public interface QuestionDAO extends DataAccessObject<Question, Integer> {
    List<Question> findByDifficulty(String difficultyLevel) throws SQLException;
    List<Question> findByType(String questionType) throws SQLException;
    List<Question> searchByText(String searchText) throws SQLException;
    Question getQuestionWithDetails(int questionId) throws SQLException;
	boolean update(Question question) throws SQLException;
	List<Question> findRandomQuestionsByCriteria(Map<String, Object> criteria, int limit) throws SQLException;
}
