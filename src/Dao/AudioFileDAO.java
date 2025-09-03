package Dao;
import java.sql.SQLException;
import java.util.Optional;

import model.AudioFile;
public interface AudioFileDAO extends DataAccessObject<AudioFile, Integer> {
    Optional<AudioFile> findByQuestionId(int questionId) throws SQLException;
    boolean deleteByQuestionId(int questionId) throws SQLException;
	boolean update(AudioFile audioFile) throws SQLException;
}
