package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.JDBCUtil;
import model.ImageFile;
import model.Question;

public class ImageFileDAOImpl implements ImageFileDAO {

    @Override
    public Optional<ImageFile> findById(Integer id) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return findByIdInternal(id, conn);
        }
    }
    
    protected Optional<ImageFile> findByIdInternal(Integer id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM ImageFiles WHERE image_file_id = ?";
        ImageFile imageFile = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                imageFile = mapResultSetToImageFile(rs);
            }
        }
        return Optional.ofNullable(imageFile);
    }
    
    @Override
    public Optional<ImageFile> findByQuestionId(int questionId) throws SQLException {
         try (Connection conn = JDBCUtil.getConnection()) {
            return findByQuestionIdInternal(questionId, conn);
        }
    }

    protected Optional<ImageFile> findByQuestionIdInternal(int questionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM ImageFiles WHERE question_id = ?";
        ImageFile imageFile = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                imageFile = mapResultSetToImageFile(rs);
            }
        }
        return Optional.ofNullable(imageFile);
    }

    @Override
    public List<ImageFile> findAll() throws SQLException {
        List<ImageFile> imageFiles = new ArrayList<>();
        String sql = "SELECT * FROM ImageFiles";
        try (Connection conn = JDBCUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                imageFiles.add(mapResultSetToImageFile(rs));
            }
        }
        return imageFiles;
    }

    @Override
    public boolean save(ImageFile imageFile) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return saveInternal(imageFile, conn);
        }
    }

    protected boolean saveInternal(ImageFile imageFile, Connection conn) throws SQLException {
        if (imageFile.getImageFileId() == 0 || !findByIdInternal(imageFile.getImageFileId(), conn).isPresent()) {
            return insertInternal(imageFile, conn);
        } else {
            return updateInternal(imageFile, conn);
        }
    }
    
    protected boolean insertInternal(ImageFile imageFile, Connection conn) throws SQLException {
        String sql = "INSERT INTO ImageFiles (question_id, file_path, file_name, caption) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (imageFile.getQuestionId() == 0) {
                 pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, imageFile.getQuestionId());
            }
            pstmt.setString(2, imageFile.getFilePath());
            pstmt.setString(3, imageFile.getFileName());
            pstmt.setString(4, imageFile.getCaption());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        imageFile.setImageFileId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean update(ImageFile imageFile) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return updateInternal(imageFile, conn);
        }
    }

    protected boolean updateInternal(ImageFile imageFile, Connection conn) throws SQLException {
        String sql = "UPDATE ImageFiles SET question_id = ?, file_path = ?, file_name = ?, caption = ? WHERE image_file_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (imageFile.getQuestionId() == 0) {
                 pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, imageFile.getQuestionId());
            }
            pstmt.setString(2, imageFile.getFilePath());
            pstmt.setString(3, imageFile.getFileName());
            pstmt.setString(4, imageFile.getCaption());
            pstmt.setInt(5, imageFile.getImageFileId());
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
        String sql = "DELETE FROM ImageFiles WHERE image_file_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            // Consider deleting physical file
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
        // Optional<ImageFile> imageFileOpt = findByQuestionIdInternal(questionId, conn);
        // if (imageFileOpt.isPresent()) { /* delete physical file */ }
        String sql = "DELETE FROM ImageFiles WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(ImageFile imageFile) throws SQLException {
        if (imageFile == null || imageFile.getImageFileId() == 0) return false;
        return deleteById(imageFile.getImageFileId());
    }

    private ImageFile mapResultSetToImageFile(ResultSet rs) throws SQLException {
        ImageFile imageFile = new ImageFile();
        imageFile.setImageFileId(rs.getInt("image_file_id"));
        imageFile.setQuestionId(rs.getInt("question_id"));
         if (rs.wasNull()) {
            imageFile.setQuestionId(0);
        }
        imageFile.setFilePath(rs.getString("file_path"));
        imageFile.setFileName(rs.getString("file_name"));
        imageFile.setCaption(rs.getString("caption"));
        return imageFile;
    }


}