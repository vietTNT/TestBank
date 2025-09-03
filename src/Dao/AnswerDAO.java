package Dao;

import java.sql.SQLException;
import java.util.List;

import model.Answer;
public interface AnswerDAO extends DataAccessObject<Answer, Integer> {
    List<Answer> findByQuestionId(int questionId) throws SQLException;
    boolean deleteByQuestionId(int questionId) throws SQLException;
	boolean update(Answer answer) throws SQLException;
}