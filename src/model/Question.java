package model;


import java.sql.Timestamp;
import java.util.List;

public class Question {
	    private int questionId;
	    private String questionText;
	    private String questionType;
	    private String difficultyLevel;
	    private String aiSuggestedAnswer;
	    private String notes;
	    private Timestamp createdAt;
	    private Timestamp updatedAt;
	    private List<Answer> answers;
	    private AudioFile audioFile; 
	    private ImageFile imageFile;
	   
	    public Question() {
			// TODO Auto-generated constructor stub
		}
	    
	    
	    
		public int getQuestionId() {
			return questionId;
		}
		public void setQuestionId(int questionId) {
			this.questionId = questionId;
		}
		public String getQuestionText() {
			return questionText;
		}
		public void setQuestionText(String questionText) {
			this.questionText = questionText;
		}
		public String getQuestionType() {
			return questionType;
		}
		public void setQuestionType(String questionType) {
			this.questionType = questionType;
		}
		public String getDifficultyLevel() {
			return difficultyLevel;
		}
		public void setDifficultyLevel(String difficultyLevel) {
			this.difficultyLevel = difficultyLevel;
		}
		public String getAiSuggestedAnswer() {
			return aiSuggestedAnswer;
		}
		public void setAiSuggestedAnswer(String aiSuggestedAnswer) {
			this.aiSuggestedAnswer = aiSuggestedAnswer;
		}
		public String getNotes() {
			return notes;
		}
		public void setNotes(String notes) {
			this.notes = notes;
		}
		public Timestamp getCreatedAt() {
			return createdAt;
		}
		public void setCreatedAt(Timestamp createdAt) {
			this.createdAt = createdAt;
		}
		public Timestamp getUpdatedAt() {
			return updatedAt;
		}
		public void setUpdatedAt(Timestamp updatedAt) {
			this.updatedAt = updatedAt;
		}
		public List<Answer> getAnswers() {
			return answers;
		}
		public void setAnswers(List<Answer> answers) {
			this.answers = answers;
		}
		public AudioFile getAudioFile() {
			return audioFile;
		}
		public void setAudioFile(AudioFile audioFile) {
			this.audioFile = audioFile;
		}
		public ImageFile getImageFile() {
			return imageFile;
		}
		public void setImageFile(ImageFile imageFile) {
			this.imageFile = imageFile;
		} 
	    
}
