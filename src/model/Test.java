package model;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Test {
	private int testId;
    private String testName;
    private String description;
    private java.sql.Timestamp createdAt;
    private List<Question> questionsInTest;
    
    public Test() {
    	  this.questionsInTest = new ArrayList<>();
    }
    public Test(String testName, String description) {
        this(); // Gọi constructor mặc định để khởi tạo questionsInTest = new ArrayList<>();
        this.testName = testName;
        this.description = description;
        // createdAt sẽ được CSDL tự động gán khi INSERT nếu cột đó có DEFAULT CURRENT_TIMESTAMP
        // hoặc bạn có thể gán ở đây nếu muốn: this.createdAt = new Timestamp(System.currentTimeMillis());
    }
	public Test(int testId, String testName, String description, Timestamp createdAt, List<Question> questionsInTest) {
		super();
		this.testId = testId;
		this.testName = testName;
		this.description = description;
		this.createdAt = createdAt;
		this.questionsInTest = questionsInTest;
	}

	public int getTestId() {
		return testId;
	}

	public void setTestId(int testId) {
		this.testId = testId;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(java.sql.Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public List<Question> getQuestionsInTest() {
		return questionsInTest;
	}

	public void setQuestionsInTest(List<Question> questionsInTest) {
		this.questionsInTest = questionsInTest;
	}


    
    
}
