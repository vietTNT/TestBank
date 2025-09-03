package model;

public class AudioFile {
	private int audioFileId;
    private int questionId;
    private String filePath;
    private String fileName;
    private String description;
    
    public AudioFile() {
		// TODO Auto-generated constructor stub
	}
    
	public AudioFile(int audioFileId, int questionId, String filePath, String fileName, String description) {
		super();
		this.audioFileId = audioFileId;
		this.questionId = questionId;
		this.filePath = filePath;
		this.fileName = fileName;
		this.description = description;
	}

	public int getAudioFileId() {
		return audioFileId;
	}

	public void setAudioFileId(int audioFileId) {
		this.audioFileId = audioFileId;
	}

	public int getQuestionId() {
		return questionId;
	}

	public void setQuestionId(int questionId) {
		this.questionId = questionId;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
}
