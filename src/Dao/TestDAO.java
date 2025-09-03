package Dao;
import java.sql.SQLException;
import model.Question;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.sql.Connection;
import model.Test;
public interface TestDAO extends DataAccessObject<Test, Integer> {
	 	Optional<Test> findById(int id) throws SQLException; // THAY ĐỔI
	    List<Test> findAll() throws SQLException; // THAY ĐỔI
	    boolean save(Test test) throws SQLException; // THAY ĐỔI
	    boolean deleteById(int id) throws SQLException;
	    List<Test> searchByName(String name) throws SQLException; // THAY ĐỔI

	    List<Question> getQuestionsForTest(int testId, Connection conn) throws SQLException;
	    boolean addQuestionToTest(int testId, int questionId, int order, Connection conn) throws SQLException;
	    boolean removeAllQuestionsFromTest(int testId, Connection conn) throws SQLException;
	    List<Question> findRandomQuestionsByCriteria(Map<String, Object> criteria, int limit) throws SQLException;
}
