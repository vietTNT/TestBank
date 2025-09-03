package View;

import Controller.QuestionController;
import model.Question;
// import model.Answer; // Không cần trực tiếp ở đây
import util.AiImageParserUtil; // Import lớp tiện ích mới

// Bỏ các import của Tess4J nếu không dùng nữa
// import net.sourceforge.tess4j.ITesseract;
// import net.sourceforge.tess4j.Tesseract;
// import net.sourceforge.tess4j.TesseractException;
// import net.sourceforge.tess4j.util.LoadLibs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class ImageToTestDialog extends JDialog {
    private QuestionController questionController;
    private File selectedImageFile;

    private JLabel lblImagePath;
    private JLabel lblImagePreview;
    private JTextArea txtExtractedContent; // Hiển thị văn bản có cấu trúc từ AI
    private JButton btnUploadImage;
    private JButton btnExtractTextAI; // Đổi tên nút cho rõ ràng
    private JButton btnProcessToQuestions;
  

    private final int PREVIEW_WIDTH = 450;
    private final int PREVIEW_HEIGHT = 350;

    public ImageToTestDialog(Frame owner, QuestionController questionController) {
        super(owner, "Trích Xuất Câu Hỏi từ Ảnh (Sử dụng AI)", true);
        this.questionController = questionController;

     

        setMinimumSize(new Dimension(950, 750));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        if (getContentPane() instanceof JPanel) {
            ((JPanel)getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));
        }
        initComponents();
    }

    private void initComponents() {
        // Panel Top
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        lblImagePath = new JLabel("Chưa chọn file ảnh nào.");
        lblImagePath.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        btnUploadImage = new JButton("Tải Ảnh Lên", IconUtils.createImageIcon("up_load.png", "Tải ảnh", 18,18));
        btnUploadImage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btnUploadImage.addActionListener(this::uploadImageAction);

        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,0));
        pathPanel.add(new JLabel("File ảnh:"));
        pathPanel.add(lblImagePath);

        JPanel topControlsPanel = new JPanel(new BorderLayout());
        topControlsPanel.add(pathPanel, BorderLayout.CENTER);
        

        topPanel.add(topControlsPanel, BorderLayout.CENTER);
        topPanel.add(btnUploadImage, BorderLayout.LINE_END);

        // Panel Center (Ảnh và Text) - Giữ nguyên
        JPanel centerSplitPanel = new JPanel(new GridLayout(1, 2, 10, 0));
     
        // Phần hiển thị ảnh
        JPanel imageDisplayPanel = new JPanel(new BorderLayout());
        imageDisplayPanel.setBorder(BorderFactory.createTitledBorder(null, "Xem trước Ảnh",
                                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                                    new Font("Segoe UI", Font.BOLD, 13), UIManager.getColor("Label.foreground")));
        lblImagePreview = new JLabel("Vui lòng tải ảnh lên", SwingConstants.CENTER);
        lblImagePreview.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        lblImagePreview.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));
        JScrollPane imageScrollPane = new JScrollPane(lblImagePreview);
        imageDisplayPanel.add(imageScrollPane, BorderLayout.CENTER);
        centerSplitPanel.add(imageDisplayPanel);

        // Phần hiển thị text trích xuất và cho phép chỉnh sửa
        JPanel textDisplayPanel = new JPanel(new BorderLayout(5,5));
        textDisplayPanel.setBorder(BorderFactory.createTitledBorder(null, "Nội dung AI trích xuất (có thể chỉnh sửa)",
                                    TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
                                    new Font("Segoe UI", Font.BOLD, 13), UIManager.getColor("Label.foreground")));
        txtExtractedContent = new JTextArea();
        // Chọn font phù hợp để hiển thị nhiều ngôn ngữ và ký tự xuống dòng
        txtExtractedContent.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtExtractedContent.setLineWrap(true);
        txtExtractedContent.setWrapStyleWord(true);
        txtExtractedContent.setMargin(new Insets(5,5,5,5));
        JScrollPane textScrollPane = new JScrollPane(txtExtractedContent);
        textDisplayPanel.add(textScrollPane, BorderLayout.CENTER);
        centerSplitPanel.add(textDisplayPanel);


        // Panel Bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnExtractTextAI = new JButton("Trích Xuất bằng AI", IconUtils.createImageIcon("AI.png", "AI OCR", 18,18)); // Cần icon mới
        btnProcessToQuestions = new JButton("Tạo Câu Hỏi", IconUtils.createImageIcon("add.png", "Tạo Câu Hỏi", 18,18));

        Font buttonFont = new Font("Segoe UI", Font.PLAIN, 14);
        btnExtractTextAI.setFont(buttonFont);
        btnProcessToQuestions.setFont(buttonFont);

        btnExtractTextAI.setEnabled(false); // Chỉ bật khi có ảnh
        btnProcessToQuestions.setEnabled(false); // Chỉ bật sau khi có text từ AI

        btnExtractTextAI.addActionListener(this::extractTextWithAIAction); // Đổi tên phương thức gọi
        btnProcessToQuestions.addActionListener(this::processAndSaveQuestionsAction);

        bottomPanel.add(btnExtractTextAI);
        bottomPanel.add(btnProcessToQuestions);

        add(topPanel, BorderLayout.NORTH);
        add(centerSplitPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void uploadImageAction(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file ảnh đề thi");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (JPG, PNG, WEBP)", "jpg", "jpeg", "png", "webp"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            lblImagePath.setText(selectedImageFile.getName());
            lblImagePath.setToolTipText(selectedImageFile.getAbsolutePath());
            txtExtractedContent.setText("");
            btnProcessToQuestions.setEnabled(false);
            try {
                BufferedImage img = ImageIO.read(selectedImageFile);
                if (img != null) {
                  
                    int originalWidth = img.getWidth();
                    int originalHeight = img.getHeight();
                    int scaledWidth = PREVIEW_WIDTH;
                    int scaledHeight = PREVIEW_HEIGHT;

                    if (originalWidth > 0 && originalHeight > 0) {
                        double imgAspectRatio = (double) originalWidth / originalHeight;
                        double previewAspectRatio = (double) PREVIEW_WIDTH / PREVIEW_HEIGHT;

                        if (imgAspectRatio > previewAspectRatio) {
                            scaledHeight = (int) (PREVIEW_WIDTH / imgAspectRatio);
                        } else {
                            scaledWidth = (int) (PREVIEW_HEIGHT * imgAspectRatio);
                        }
                    }
                    Image scaledImg = img.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                    lblImagePreview.setIcon(new ImageIcon(scaledImg));
                    lblImagePreview.setText("");
                    btnExtractTextAI.setEnabled(true);
                } else {
              
                }
            } catch (IOException ex) {
             
            }
        }
    }

    private void extractTextWithAIAction(ActionEvent e) {
        if (selectedImageFile == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một file ảnh trước.", "Chưa chọn ảnh", JOptionPane.WARNING_MESSAGE);
            return;
        }

   
        // Nếu AI tự phát hiện ngôn ngữ tốt, bạn có thể truyền null hoặc chuỗi rỗng
        String langHint = ""; // Ví dụ: "ja" hoặc "vi" hoặc để trống cho AI tự phát hiện

        JDialog loadingDialog = new JDialog(this, "Đang xử lý với AI...", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        loadingDialog.add(BorderLayout.CENTER, progressBar);
        loadingDialog.add(BorderLayout.NORTH, new JLabel("Vui lòng chờ, đang gửi ảnh và nhận dữ liệu từ AI..."));
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setSize(450, 120);
        loadingDialog.setLocationRelativeTo(this);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return AiImageParserUtil.scanImageAndGetStructuredText(selectedImageFile.getAbsolutePath());
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    String structuredTextResult = get();
                    if (structuredTextResult != null && !structuredTextResult.startsWith("Lỗi AI OCR:")) {
                        txtExtractedContent.setText(structuredTextResult);
                        if (!structuredTextResult.trim().isEmpty()) {
                            btnProcessToQuestions.setEnabled(true);
                        }
                        JOptionPane.showMessageDialog(ImageToTestDialog.this, "Trích xuất văn bản bằng AI hoàn tất!", "AI OCR Thành Công", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        txtExtractedContent.setText("");
                        btnProcessToQuestions.setEnabled(false);
                        JOptionPane.showMessageDialog(ImageToTestDialog.this,
                                (structuredTextResult != null ? structuredTextResult : "Không thể trích xuất văn bản từ ảnh bằng AI."),
                                "AI OCR Thất Bại", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    txtExtractedContent.setText("");
                    btnProcessToQuestions.setEnabled(false);
                    JOptionPane.showMessageDialog(ImageToTestDialog.this, "Lỗi trong quá trình AI OCR: " + ex.getMessage(), "Lỗi AI OCR", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
        loadingDialog.setVisible(true);
    }

    private void processAndSaveQuestionsAction(ActionEvent e) {
        String structuredTextFromAI = txtExtractedContent.getText();
        if (structuredTextFromAI.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có nội dung để tạo và lưu câu hỏi.", "Nội dung rỗng", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Vô hiệu hóa nút trong khi xử lý
        btnProcessToQuestions.setEnabled(false);
        btnProcessToQuestions.setText("Đang lưu...");



        SwingWorker<List<model.Question>, Void> parserWorker = new SwingWorker<List<model.Question>, Void>() {
            @Override
            protected List<model.Question> doInBackground() throws Exception {
                return AiImageParserUtil.parseStructuredTextToQuestions(structuredTextFromAI);
            }

            @Override
            protected void done() {
                try {
                    List<model.Question> parsedQuestionsFromAI = get();
                    if (parsedQuestionsFromAI == null || parsedQuestionsFromAI.isEmpty()) {
                        JOptionPane.showMessageDialog(ImageToTestDialog.this,
                                "Không thể phân tích thành câu hỏi nào từ nội dung AI trả về.\nVui lòng kiểm tra và chỉnh sửa lại văn bản trong ô.",
                                "Phân Tích Thất Bại", JOptionPane.WARNING_MESSAGE);
                        btnProcessToQuestions.setText("Phân Tích & Tạo Câu Hỏi"); // Reset tên nút
                        btnProcessToQuestions.setEnabled(true);
                        return;
                    }

                    // Hiển thị Dialog xem lại và chọn mức độ
                    ParsedQuestionsReviewDialog reviewDialog = new ParsedQuestionsReviewDialog(
                            (Frame) SwingUtilities.getWindowAncestor(ImageToTestDialog.this),
                            parsedQuestionsFromAI
                    );
                    reviewDialog.setVisible(true);

                    if (reviewDialog.isSaved()) {
                        List<Question> questionsReadyToSave = reviewDialog.getQuestionsToSave();
                        if (!questionsReadyToSave.isEmpty()) {
                            // Gọi QuestionController để lưu hàng loạt với mức độ đã chọn
                            // Cần một SwingWorker mới cho việc lưu nếu nó tốn thời gian
                            saveQuestionsWithController(questionsReadyToSave);
                        } else {
                             JOptionPane.showMessageDialog(ImageToTestDialog.this, "Không có câu hỏi nào được chọn để lưu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ImageToTestDialog.this,
                            "Lỗi trong quá trình phân tích câu hỏi: " + ex.getMessage(),
                            "Lỗi Phân Tích", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnProcessToQuestions.setText("Phân Tích & Tạo Câu Hỏi"); // Reset tên nút
                    btnProcessToQuestions.setEnabled(true); // Bật lại nút
                }
            }
        };
        parserWorker.execute();
    }
    // Phương thức helper để gọi QuestionController lưu trong một SwingWorker khác
    private void saveQuestionsWithController(List<Question> questionsToSave) {
        JDialog savingDialog = new JDialog(this, "Đang lưu câu hỏi...", true);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        savingDialog.add(BorderLayout.CENTER, progressBar);
        savingDialog.add(BorderLayout.NORTH, new JLabel("Vui lòng chờ..."));
        savingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        savingDialog.setSize(300, 75);
        savingDialog.setLocationRelativeTo(this);

        SwingWorker<Integer, Void> saveWorker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                // Truyền null hoặc một giá trị mặc định nếu không có cấp độ chung từ dialog trước
                // Tuy nhiên, ParsedQuestionsReviewDialog đã gán cấp độ cho từng câu hỏi rồi.
                return questionController.saveParsedQuestionsBatch(questionsToSave, null); // Tham số thứ 2 có thể không cần nữa
            }

            @Override
            protected void done() {
                savingDialog.dispose();
                try {
                    Integer savedCount = get();
                    if (savedCount > 0) {
                        JOptionPane.showMessageDialog(ImageToTestDialog.this,
                                "Đã lưu thành công " + savedCount + "/" + questionsToSave.size() + " câu hỏi vào cơ sở dữ liệu!",
                                "Lưu Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        if (questionController != null) {
                            questionController.loadInitialQuestions();
                        }
                    } else {
                        JOptionPane.showMessageDialog(ImageToTestDialog.this,
                                "Không có câu hỏi nào được lưu.",
                                "Lưu Không Thành Công", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                     JOptionPane.showMessageDialog(ImageToTestDialog.this,
                            "Lỗi trong quá trình lưu câu hỏi vào CSDL: " + ex.getMessage(),
                            "Lỗi Lưu Trữ", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        saveWorker.execute();
        savingDialog.setVisible(true);
    }
}

