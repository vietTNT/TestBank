package Dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import model.Question;
public interface DataAccessObject<T, K> {
    Optional<T> findById(K id) throws SQLException;
    List<T> findAll() throws SQLException;
    boolean save(T entity) throws SQLException; 
    
    boolean deleteById(K id) throws SQLException;
    boolean delete(T entity) throws SQLException;
	
}
