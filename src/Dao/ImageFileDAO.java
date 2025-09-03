package Dao;

import java.sql.SQLException;
import java.util.Optional;

import model.ImageFile;
public interface ImageFileDAO extends DataAccessObject<ImageFile, Integer> {
    Optional<ImageFile> findByQuestionId(int questionId) throws SQLException;
    boolean deleteByQuestionId(int questionId) throws SQLException;
	boolean update(ImageFile imageFile) throws SQLException;
}
