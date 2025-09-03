package Dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import database.JDBCUtil;
import model.AudioFile;

public class AudioFileDAOImpl implements AudioFileDAO {
    
    @Override
    public Optional<AudioFile> findById(Integer id) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return findByIdInternal(id, conn);
        }
    }

    protected Optional<AudioFile> findByIdInternal(Integer id, Connection conn) throws SQLException {
        String sql = "SELECT * FROM AudioFiles WHERE audio_file_id = ?";
        AudioFile audioFile = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                audioFile = mapResultSetToAudioFile(rs);
            }
        }
        return Optional.ofNullable(audioFile);
    }
    
    @Override
    public Optional<AudioFile> findByQuestionId(int questionId) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return findByQuestionIdInternal(questionId, conn);
        }
    }

    protected Optional<AudioFile> findByQuestionIdInternal(int questionId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM AudioFiles WHERE question_id = ?";
        AudioFile audioFile = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                audioFile = mapResultSetToAudioFile(rs);
            }
        }
        return Optional.ofNullable(audioFile);
    }

    @Override
    public List<AudioFile> findAll() throws SQLException {
        List<AudioFile> audioFiles = new ArrayList<>();
        String sql = "SELECT * FROM AudioFiles";
        try (Connection conn = JDBCUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                audioFiles.add(mapResultSetToAudioFile(rs));
            }
        }
        return audioFiles;
    }

    @Override
    public boolean save(AudioFile audioFile) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return saveInternal(audioFile, conn);
        }
    }
    
    protected boolean saveInternal(AudioFile audioFile, Connection conn) throws SQLException {
         if (audioFile.getAudioFileId() == 0 || !findByIdInternal(audioFile.getAudioFileId(), conn).isPresent()) {
            return insertInternal(audioFile, conn);
        } else {
            return updateInternal(audioFile, conn);
        }
    }
    
    protected boolean insertInternal(AudioFile audioFile, Connection conn) throws SQLException {
        String sql = "INSERT INTO AudioFiles (question_id, file_path, file_name, description) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Handle potential null question_id if AudioFile can exist without a question initially
            if (audioFile.getQuestionId() == 0) { // Assuming 0 means not set
                 pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, audioFile.getQuestionId());
            }
            pstmt.setString(2, audioFile.getFilePath());
            pstmt.setString(3, audioFile.getFileName());
            pstmt.setString(4, audioFile.getDescription());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        audioFile.setAudioFileId(generatedKeys.getInt(1));
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean update(AudioFile audioFile) throws SQLException {
        try (Connection conn = JDBCUtil.getConnection()) {
            return updateInternal(audioFile, conn);
        }
    }

    protected boolean updateInternal(AudioFile audioFile, Connection conn) throws SQLException {
        String sql = "UPDATE AudioFiles SET question_id = ?, file_path = ?, file_name = ?, description = ? WHERE audio_file_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
             if (audioFile.getQuestionId() == 0) {
                 pstmt.setNull(1, Types.INTEGER);
            } else {
                pstmt.setInt(1, audioFile.getQuestionId());
            }
            pstmt.setString(2, audioFile.getFilePath());
            pstmt.setString(3, audioFile.getFileName());
            pstmt.setString(4, audioFile.getDescription());
            pstmt.setInt(5, audioFile.getAudioFileId());
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
        String sql = "DELETE FROM AudioFiles WHERE audio_file_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            // Consider deleting the physical file here if needed
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
        // Optional<AudioFile> audioFileOpt = findByQuestionIdInternal(questionId, conn);
        // if (audioFileOpt.isPresent()) { /* delete physical file */ }
        String sql = "DELETE FROM AudioFiles WHERE question_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, questionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean delete(AudioFile audioFile) throws SQLException {
        if (audioFile == null || audioFile.getAudioFileId() == 0) return false;
        return deleteById(audioFile.getAudioFileId());
    }

    private AudioFile mapResultSetToAudioFile(ResultSet rs) throws SQLException {
        AudioFile audioFile = new AudioFile();
        audioFile.setAudioFileId(rs.getInt("audio_file_id"));
        audioFile.setQuestionId(rs.getInt("question_id")); // Will be 0 if NULL in DB
        if (rs.wasNull()) { // Check if the last read value (question_id) was SQL NULL
            audioFile.setQuestionId(0); // Or handle as appropriate
        }
        audioFile.setFilePath(rs.getString("file_path"));
        audioFile.setFileName(rs.getString("file_name"));
        audioFile.setDescription(rs.getString("description"));
        return audioFile;
    }
}
