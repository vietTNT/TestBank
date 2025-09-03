package model;

public class ImageFile {
	private int imageFileId;
    private int questionId;
    private String filePath;
    private String fileName;
    private String caption;
    
    public ImageFile() {}
    
	public ImageFile(int imageFileId, int questionId, String filePath, String fileName, String caption) {
		super();
		this.imageFileId = imageFileId;
		this.questionId = questionId;
		this.filePath = filePath;
		this.fileName = fileName;
		this.caption = caption;
	}

	public int getImageFileId() {
		return imageFileId;
	}

	public void setImageFileId(int imageFileId) {
		this.imageFileId = imageFileId;
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

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	@Override
    public String toString() {
        return "ImageFile ID: " + imageFileId + ", Name: " + fileName;
    }
	
    
}
