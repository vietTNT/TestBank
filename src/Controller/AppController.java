package Controller;

import View.MainFrame;
import View.QuestionManagementPanel;
import View.TestManagementPanel;
import Dao.AnswerDAO;
import Dao.AudioFileDAO;
import Dao.ImageFileDAO;
import Dao.QuestionDAO;
import Dao.TestDAO;
import Dao.AnswerDAOImpl;
import Dao.AudioFileDAOImpl;
import Dao.ImageFileDAOImpl;
import Dao.QuestionDAOImpl;
import Dao.TestDAOImpl;

public class AppController {
    private MainFrame mainFrame;
    private QuestionController questionController;
    private TestController testController;
    // private SettingsController settingsController;

    private QuestionDAO questionDAO;
    private AnswerDAO answerDAO;
    private AudioFileDAO audioFileDAO;
    private ImageFileDAO imageFileDAO;
    private TestDAO testDAO;

    public AppController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.questionDAO = new QuestionDAOImpl();
        this.answerDAO = new AnswerDAOImpl();
        this.audioFileDAO = new AudioFileDAOImpl();
        this.imageFileDAO = new ImageFileDAOImpl();
        this.testDAO = new TestDAOImpl();

        this.questionController = new QuestionController(this.mainFrame, this.questionDAO, this.answerDAO, this.audioFileDAO, this.imageFileDAO);
        this.testController = new TestController(
                this.mainFrame, 
                this.testDAO, 
                this.questionDAO      // TestController cần QuestionDAO để lấy danh sách câu hỏi cho TestEditorDialog
            );
        // this.settingsController = new SettingsController(this.mainFrame);

        // Việc liên kết View với Controller sẽ được thực hiện trong MainFrame.setAppController()
        System.out.println("AppController initialized. MainFrame will link views to controllers via setAppController().");
    }

    public QuestionController getQuestionController() {
        return questionController;
    }

    public TestController getTestController() {
        return testController;
    }
}