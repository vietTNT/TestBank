package View;

import model.Answer;
import model.AudioFile;
import model.ImageFile;
import model.Question;
import Controller.QuestionController;
import View.AnswersPanel.AnswerEntryPanel;
import AiService.AIAnswerSuggester; // Đảm bảo import đúng lớp AI của bạn

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.sound.sampled.*; // Cho chức năng phát audio

public class QuestionEditorDialog extends JDialog {
    private Question currentQuestion;
    private QuestionController controller;
    private boolean saved = false;

    private JTextArea txtQuestionText;
    private JComboBox<String> cmbQuestionType;
    private JComboBox<String> cmbDifficultyLevel;
    private AnswersPanel answersPanel;
    private JTextArea txtAiSuggestion;
    private JTextArea txtNotes;

    private JTextField txtAudioFilePath;
    private JButton btnChooseAudio, btnPlayAudio, btnClearAudio, btnPauseResumeAudio;
    private File selectedAudioFile;
    private Clip audioClip;
    private boolean isAudioPaused = false;
    private long audioClipPosition = 0;

    private JLabel lblImagePreview;
    private JTextField txtImageFilePath;
    private JButton btnChooseImage, btnClearImage;
    private File selectedImageFile;
    private final int PREVIEW_SIZE = 180;

    private Font labelFont = new Font("Segoe UI", Font.BOLD, 14);
    private Font fieldFont = new Font("Segoe UI", Font.PLAIN, 14);
    private Font titleBorderFont = new Font("Segoe UI", Font.BOLD, 13);

    private JComboBox<String> cmbQuestionFontFamily;
    private JComboBox<Integer> cmbQuestionFontSize;
    private JComboBox<String> cmbAnswerFontFamily;
    private JComboBox<Integer> cmbAnswerFontSize;
    private final Integer[] FONT_SIZES = {10, 12, 14, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72};
    private JPanel answerFontPanelGlobal;

    private JButton btnSuggestAnswerAI;

    public QuestionEditorDialog(Frame owner, Question question, QuestionController controller) {
        super(owner, (question == null || question.getQuestionId() == 0 ? "Thêm Câu hỏi Mới" : "Chỉnh sửa Câu hỏi"), true);
        this.currentQuestion = (question == null) ? new Question() : question;
        this.controller = controller;

        setMinimumSize(new Dimension(880, 820)); // Điều chỉnh kích thước nếu cần
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(15, 15));
        if (getContentPane() instanceof JPanel) {
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(15, 15, 15, 15));
        }

        initComponents();
        loadQuestionData();
        initFontControls();
    }

    private void initComponents() {
        JPanel mainFormPanel = new JPanel(new GridBagLayout());
        mainFormPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;

        // === Section 1: Thông tin chung ===
        JPanel generalInfoPanel = createSectionPanel("Thông tin chung");
        GridBagConstraints gbcGeneral = new GridBagConstraints();
        gbcGeneral.insets = new Insets(5, 5, 5, 5);
        gbcGeneral.fill = GridBagConstraints.HORIZONTAL;
        gbcGeneral.anchor = GridBagConstraints.WEST;

        gbcGeneral.gridx = 0; gbcGeneral.gridy = 0;
        generalInfoPanel.add(createStyledLabel("Loại câu hỏi:"), gbcGeneral);

        gbcGeneral.gridx = 1; gbcGeneral.weightx = 0.4;
        String[] questionTypes = {"Trắc nghiệm", "Nghe hiểu", "Đọc hiểu", "Từ vựng", "Ngữ pháp", "Kanji", "Bảng Chữ Cái"};
        cmbQuestionType = new JComboBox<>(questionTypes);
        cmbQuestionType.setFont(fieldFont);
        generalInfoPanel.add(cmbQuestionType, gbcGeneral);

        gbcGeneral.gridx = 2; gbcGeneral.weightx = 0.1;
        generalInfoPanel.add(Box.createHorizontalStrut(20), gbcGeneral);

        gbcGeneral.gridx = 3; gbcGeneral.weightx = 0.0;
        generalInfoPanel.add(createStyledLabel("Mức độ:"), gbcGeneral);
        gbcGeneral.gridx = 4; gbcGeneral.weightx = 0.4;
        cmbDifficultyLevel = new JComboBox<>(new String[]{"Bảng Chữ Cái", "N5", "N4", "N3", "N2", "N1", "Khác"});
        cmbDifficultyLevel.setFont(fieldFont);
        generalInfoPanel.add(cmbDifficultyLevel, gbcGeneral);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainFormPanel.add(generalInfoPanel, gbc);
        gbc.gridwidth = 1; // Reset

        // === Section 2: Nội dung câu hỏi ===
        JPanel contentPanel = createSectionPanel("Nội dung câu hỏi");
        txtQuestionText = new JTextArea(6, 0);
        txtQuestionText.setFont(determineInitialFont()); // Sử dụng hàm xác định font
        txtQuestionText.setLineWrap(true);
        txtQuestionText.setWrapStyleWord(true);
        JScrollPane questionScrollPane = new JScrollPane(txtQuestionText);
        // Để JScrollPane mở rộng
        GridBagConstraints gbcContent = new GridBagConstraints();
        gbcContent.gridx = 0; gbcContent.gridy = 0;
        gbcContent.weightx = 1.0; gbcContent.weighty = 1.0;
        gbcContent.fill = GridBagConstraints.BOTH;
        contentPanel.add(questionScrollPane, gbcContent);

        gbc.gridy++; gbc.gridwidth = 2; gbc.weighty = 0.3; gbc.fill = GridBagConstraints.BOTH;
        mainFormPanel.add(contentPanel, gbc);
        gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL; // Reset
        gbc.gridwidth = 1;

        // === Section Font cho Câu hỏi ===
        JPanel questionFontPanel = createSectionPanel("Định dạng Font Câu hỏi");
        addFontControlsToPanel(questionFontPanel, "Câu hỏi", true);
        gbc.gridy++; gbc.gridwidth = 2;
        mainFormPanel.add(questionFontPanel, gbc);
        gbc.gridwidth = 1;

        // === Section 3: Đáp án (cho trắc nghiệm và các loại tương tự) ===
        answersPanel = new AnswersPanel();
        answersPanel.setBorder(createTitledBorder("Các lựa chọn đáp án"));
        gbc.gridy++; gbc.gridwidth = 2; gbc.weighty = 0.4; gbc.fill = GridBagConstraints.BOTH;
        mainFormPanel.add(answersPanel, gbc);
        gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;

        // === Section Font cho Câu trả lời ===
        answerFontPanelGlobal = createSectionPanel("Định dạng Font Câu trả lời");
        addFontControlsToPanel(answerFontPanelGlobal, "Câu trả lời", false);
        gbc.gridy++; gbc.gridwidth = 2;
        mainFormPanel.add(answerFontPanelGlobal, gbc);
        gbc.gridwidth = 1;

        // Logic hiển thị/ẩn AnswersPanel và AnswerFontPanel
        cmbQuestionType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedType = (String) e.getItem();
                updatePanelsVisibility(selectedType);
                updateAISuggestButtonState(selectedType);
            }
        });
        updatePanelsVisibility((String) cmbQuestionType.getSelectedItem()); // Gọi lần đầu


        // === Section 4: File đính kèm ===
        JPanel attachmentsPanel = createSectionPanel("File đính kèm");
        attachmentsPanel.setLayout(new GridLayout(1, 2, 15, 0));
        attachmentsPanel.add(createAudioFilePanel());
        attachmentsPanel.add(createImageFilePanel());
        gbc.gridy++; gbc.gridwidth = 2;
        mainFormPanel.add(attachmentsPanel, gbc);
        gbc.gridwidth = 1;

        // === Section 5: Thông tin bổ sung (Gợi ý AI, Ghi chú) ===
        JPanel additionalInfoPanel = createSectionPanel("Thông tin bổ sung");
        additionalInfoPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcAdd = new GridBagConstraints();
        gbcAdd.insets = new Insets(5,5,5,5);
        gbcAdd.fill = GridBagConstraints.HORIZONTAL;
        gbcAdd.anchor = GridBagConstraints.WEST;

        // Gợi ý AI
        gbcAdd.gridx = 0; gbcAdd.gridy = 0; gbcAdd.weightx = 0.0;
        additionalInfoPanel.add(createStyledLabel("Gợi ý AI:"), gbcAdd);

        gbcAdd.gridy = 1; gbcAdd.weightx = 1.0; gbcAdd.gridwidth = 2; gbcAdd.ipady = 40; // Tăng chiều cao
        txtAiSuggestion = new JTextArea(3,0);
        Font multiLanguageFont;
        String[] preferredFonts = {
            "Arial Unicode MS", // Ưu tiên hàng đầu nếu có
            "Segoe UI",        // Tốt cho Windows hiện đại
            "MS Gothic",        // Tốt cho tiếng Nhật, kiểm tra tiếng Việt
            "Yu Gothic",        // Tương tự MS Gothic
            Font.SANS_SERIF     // Font logic, để hệ thống tự chọn
        };

        multiLanguageFont = findAvailableFont(preferredFonts, 14); // Tìm font phù hợp với cỡ 14

        if (multiLanguageFont == null) { // Nếu không tìm thấy font nào trong danh sách ưu tiên
            multiLanguageFont = new Font(Font.SANS_SERIF, Font.PLAIN, 14); // Dùng font logic mặc định
            System.err.println("Không tìm thấy font ưu tiên, sử dụng font SANS_SERIF mặc định cho AI Suggestion.");
        }
        
        System.out.println("Font được chọn cho AI Suggestion: " + multiLanguageFont.getFontName());
        txtAiSuggestion.setFont(multiLanguageFont);
        txtAiSuggestion.setLineWrap(true);
        txtAiSuggestion.setWrapStyleWord(true);
        txtAiSuggestion.setEditable(false); // AI suggestion chỉ để đọc
        additionalInfoPanel.add(new JScrollPane(txtAiSuggestion), gbcAdd);
        gbcAdd.ipady = 0;

        // Nút Gợi ý Đáp án từ AI
        btnSuggestAnswerAI = new JButton("Gợi ý Đáp án từ AI");
        btnSuggestAnswerAI.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSuggestAnswerAI.setIcon(IconUtils.createImageIcon("icons/ai_lightbulb_16.png", "Gợi ý AI", 16, 16)); // Cần icon
        btnSuggestAnswerAI.addActionListener(this::suggestAnswerAIAction);
        updateAISuggestButtonState((String) cmbQuestionType.getSelectedItem()); // Đặt trạng thái ban đầu

        gbcAdd.gridy = 2; gbcAdd.gridx = 0; gbcAdd.gridwidth = 2;
        gbcAdd.fill = GridBagConstraints.NONE; gbcAdd.anchor = GridBagConstraints.EAST; // Căn phải nút
        additionalInfoPanel.add(btnSuggestAnswerAI, gbcAdd);
        gbcAdd.fill = GridBagConstraints.HORIZONTAL; gbcAdd.anchor = GridBagConstraints.WEST; // Reset


        // Ghi chú
        gbcAdd.gridy = 3; gbcAdd.gridx = 0; gbcAdd.gridwidth = 1; gbcAdd.weightx = 0.0;
        additionalInfoPanel.add(createStyledLabel("Ghi chú:"), gbcAdd);

        gbcAdd.gridy = 4; gbcAdd.gridx = 0; gbcAdd.gridwidth = 2; gbcAdd.weightx = 1.0; gbcAdd.ipady = 40;
        txtNotes = new JTextArea(3,0);
        txtNotes.setFont(fieldFont);
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        additionalInfoPanel.add(new JScrollPane(txtNotes), gbcAdd);
        gbcAdd.ipady = 0;

        gbc.gridy++; gbc.gridwidth = 2;
        mainFormPanel.add(additionalInfoPanel, gbc);
        gbc.gridwidth = 1;

        // --- Panel chứa các nút chính (Lưu, Hủy) ---
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10)); // Thêm padding trên dưới
        bottomButtonPanel.setOpaque(false);
        JButton btnSave = createStyledButton("Lưu câu hỏi", "save.png");
        JButton btnCancel = createStyledButton("Hủy bỏ", "icon_X.png");

        btnSave.addActionListener(this::saveAction);
        btnCancel.addActionListener(e -> {
            stopCurrentAudio(); 
            dispose();
        });

        bottomButtonPanel.add(btnCancel);
        bottomButtonPanel.add(btnSave);

        // Thêm mainFormPanel vào JScrollPane
        JScrollPane formScrollPane = new JScrollPane(mainFormPanel);
        formScrollPane.setBorder(null);
        formScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Tăng tốc độ cuộn
        if (UIManager.get("Panel.background") != null) {
             formScrollPane.getViewport().setBackground(UIManager.getColor("Panel.background"));
        }

        add(formScrollPane, BorderLayout.CENTER);
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private Font determineInitialFont() {
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 14); // Font mặc định của bạn
        String[] preferredJapaneseFonts = {
            "MS Mincho", "MS Gothic", "Yu Mincho", "Yu Gothic", "Meiryo",
            "Hiragino Sans", "Arial Unicode MS", "TakaoPGothic", "Noto Sans JP"
        };
        for (String fontName : preferredJapaneseFonts) {
            Font testFont = new Font(fontName, Font.PLAIN, 14);
            if (canDisplayJapanese(testFont, fontName)) {
                System.out.println("INFO: Using font '" + fontName + "' for question text area.");
                return testFont;
            }
        }
        System.err.println("WARNING: No preferred Japanese font found or suitable. Using default.");
        return defaultFont;
    }

    private boolean canDisplayJapanese(Font font, String targetFontName) {
        if (font == null) return false;
        // Kiểm tra font có thực sự là font mong muốn không (tránh trường hợp trả về font fallback)
        if (!font.getFamily().equalsIgnoreCase(targetFontName)) {
            // Kiểm tra trong danh sách font hệ thống
            boolean actuallyAvailable = false;
            String[] availableFontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            for(String availableName : availableFontNames) {
                if (availableName.equalsIgnoreCase(targetFontName)) {
                    actuallyAvailable = true;
                    break;
                }
            }
            if (!actuallyAvailable) return false;
        }
        return font.canDisplay('\u3042') && font.canDisplay('\u65E5'); // Test 'あ' và '日'
    }


    private void updatePanelsVisibility(String selectedType) {
        boolean isMcqType = "Trắc nghiệm".equals(selectedType) ||
                            "Ngữ pháp".equals(selectedType) ||
                            "Từ vựng".equals(selectedType) ||
                            "Kanji".equals(selectedType); // Các loại dùng AnswersPanel
        answersPanel.setVisible(isMcqType);
        answerFontPanelGlobal.setVisible(isMcqType);
    }

    private void updateAISuggestButtonState(String questionType) {
        boolean enableAISuggest = "Trắc nghiệm".equals(questionType) ||
                                  "Ngữ pháp".equals(questionType) ||
                                  "Từ vựng".equals(questionType) ||
                                  "Kanji".equals(questionType);
        btnSuggestAnswerAI.setEnabled(enableAISuggest);
    }

    private void addFontControlsToPanel(JPanel targetPanel, String labelPrefix, boolean isForQuestion) {
       
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3,5,3,5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        targetPanel.add(createStyledLabel("Font chữ " + labelPrefix + ":"), c);

        String[] fontFamilies = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        JComboBox<String> cmbFontFamily = new JComboBox<>(new Vector<>(Arrays.asList(fontFamilies)));
       
        cmbFontFamily.setFont(fieldFont);
        cmbFontFamily.setPreferredSize(new Dimension(200, cmbFontFamily.getPreferredSize().height));
        c.gridx = 1; c.weightx = 0.7; c.fill = GridBagConstraints.HORIZONTAL;
        targetPanel.add(cmbFontFamily, c);
        c.weightx = 0.0; c.fill = GridBagConstraints.NONE;

        c.gridx = 2;
        targetPanel.add(Box.createHorizontalStrut(10),c);

        c.gridx = 3;
        targetPanel.add(createStyledLabel("Cỡ chữ:"), c);

        JComboBox<Integer> cmbFontSize = new JComboBox<>(FONT_SIZES);
        cmbFontSize.setFont(fieldFont);
        cmbFontSize.setPreferredSize(new Dimension(80, cmbFontSize.getPreferredSize().height));
        c.gridx = 4;
        targetPanel.add(cmbFontSize, c);

        c.gridx = 5; c.weightx = 1.0; 
        targetPanel.add(Box.createHorizontalGlue(), c);


        if (isForQuestion) {
            this.cmbQuestionFontFamily = cmbFontFamily;
            this.cmbQuestionFontSize = cmbFontSize;
            ItemListener questionFontListener = e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateQuestionTextFont();
                }
            };
            cmbFontFamily.addItemListener(questionFontListener);
            cmbFontSize.addItemListener(questionFontListener);
        } else {
            this.cmbAnswerFontFamily = cmbFontFamily;
            this.cmbAnswerFontSize = cmbFontSize;
            ItemListener answerFontListener = e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateAnswerTextFont();
                }
            };
            cmbFontFamily.addItemListener(answerFontListener);
            cmbFontSize.addItemListener(answerFontListener);
        }
    }

    private void initFontControls() {
        if (txtQuestionText == null) return;

        Font currentQuestionFont = txtQuestionText.getFont();
        if (cmbQuestionFontFamily != null) cmbQuestionFontFamily.setSelectedItem(currentQuestionFont.getFamily());
        if (cmbQuestionFontSize != null) cmbQuestionFontSize.setSelectedItem(currentQuestionFont.getSize());

        if (answersPanel != null && cmbAnswerFontFamily != null && cmbAnswerFontSize != null) {
            Font currentAnswerFont = answersPanel.getCurrentAnswerFont();
            cmbAnswerFontFamily.setSelectedItem(currentAnswerFont.getFamily());
            cmbAnswerFontSize.setSelectedItem(currentAnswerFont.getSize());
        }
    }

    private void updateQuestionTextFont() {
        // ... (Giữ nguyên code của bạn)
        if (txtQuestionText == null || cmbQuestionFontFamily == null || cmbQuestionFontSize == null ||
            cmbQuestionFontFamily.getSelectedItem() == null || cmbQuestionFontSize.getSelectedItem() == null) return;

        String family = (String) cmbQuestionFontFamily.getSelectedItem();
        int size = (Integer) cmbQuestionFontSize.getSelectedItem();
        Font newFont = new Font(family, Font.PLAIN, size);
        txtQuestionText.setFont(newFont);
    }

    private void updateAnswerTextFont() {
        // ... (Giữ nguyên code của bạn)
         if (answersPanel == null || cmbAnswerFontFamily == null || cmbAnswerFontSize == null ||
            cmbAnswerFontFamily.getSelectedItem() == null || cmbAnswerFontSize.getSelectedItem() == null) return;

        String family = (String) cmbAnswerFontFamily.getSelectedItem();
        int size = (Integer) cmbAnswerFontSize.getSelectedItem();
        Font newFont = new Font(family, Font.PLAIN, size);
        answersPanel.setAnswerFontForAllEntries(newFont);
    }

    private JPanel createSectionPanel(String title) {
        // ... (Giữ nguyên code của bạn)
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(createTitledBorder(title));
        return panel;
    }

    private TitledBorder createTitledBorder(String title) {
        // ... (Giữ nguyên code của bạn)
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, UIManager.getColor("Component.borderColor")),
            " " + title + " "
        );
        titledBorder.setTitleFont(titleBorderFont);
        if (UIManager.get("Label.foreground") != null) { // Thêm kiểm tra null
            titledBorder.setTitleColor(UIManager.getColor("Label.foreground"));
        }
        return titledBorder;
    }

    private JLabel createStyledLabel(String text) {
        // ... (Giữ nguyên code của bạn)
        JLabel label = new JLabel(text);
        label.setFont(labelFont);
        return label;
    }

    private JButton createStyledButton(String text, String iconPath) {
        // ... (Giữ nguyên code của bạn)
        ImageIcon icon = IconUtils.createImageIcon(iconPath, text, 18, 18);
        JButton button = new JButton(text, icon);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
        button.setMargin(new Insets(8, 15, 8, 15));
        return button;
    }

    private JPanel createAudioFilePanel() {
        // ... (Sửa đổi để bao gồm btnPauseResumeAudio nếu bạn đã làm theo hướng dẫn trước)
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3,3,3,3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4; gbc.weightx = 1.0;
        txtAudioFilePath = new JTextField();
        txtAudioFilePath.setFont(fieldFont.deriveFont(Font.ITALIC, 13f));
        txtAudioFilePath.setEditable(false);
        txtAudioFilePath.putClientProperty("JTextField.placeholderText", "Chưa chọn file âm thanh");
        panel.add(txtAudioFilePath, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.weightx = 0.0; // Reset weightx

        gbc.gridx = 0;
        btnChooseAudio = createMiniButton("Chọn File", "add.png"); // Cập nhật icon path nếu cần
        panel.add(btnChooseAudio, gbc);

        gbc.gridx = 1;
        btnPlayAudio = createMiniButton("Phát", "play.png"); // Cập nhật icon
        panel.add(btnPlayAudio, gbc);

        gbc.gridx = 2;
        btnPauseResumeAudio = createMiniButton("Tạm dừng", "pause.png"); // Nút mới
        btnPauseResumeAudio.setEnabled(false);
        panel.add(btnPauseResumeAudio, gbc);

        gbc.gridx = 3;
        btnClearAudio = createMiniButton("Xóa", "icon_X.png"); // Cập nhật icon
        panel.add(btnClearAudio, gbc);

        btnChooseAudio.addActionListener(this::chooseAudioFileAction);
        btnPlayAudio.addActionListener(this::playAudioAction);
        btnPauseResumeAudio.addActionListener(this::pauseResumeAudioAction);
        btnClearAudio.addActionListener(this::clearAudioFileAction);

        btnPlayAudio.setEnabled(selectedAudioFile != null && selectedAudioFile.exists());
        return panel;
    }

    private JPanel createImageFilePanel() {
        // ... (Giữ nguyên code của bạn)
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3,3,3,3);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        lblImagePreview = new JLabel("Xem trước", SwingConstants.CENTER);
        lblImagePreview.setFont(fieldFont.deriveFont(Font.ITALIC));
        lblImagePreview.setPreferredSize(new Dimension(PREVIEW_SIZE, PREVIEW_SIZE - 40));
        lblImagePreview.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));
        lblImagePreview.setOpaque(true);
        if (UIManager.get("TextField.background") != null) {
            lblImagePreview.setBackground(UIManager.getColor("TextField.background"));
        }
        panel.add(lblImagePreview, gbc);

        gbc.gridy++;
        txtImageFilePath = new JTextField();
        txtImageFilePath.setFont(fieldFont.deriveFont(Font.ITALIC, 13f));
        txtImageFilePath.setEditable(false);
        txtImageFilePath.putClientProperty("JTextField.placeholderText", "Chưa chọn file hình ảnh");
        panel.add(txtImageFilePath, gbc);

        gbc.gridy++; gbc.gridwidth = 1; gbc.weightx = 0.5;
        btnChooseImage = createMiniButton("Chọn File", "add.png");
        panel.add(btnChooseImage, gbc);

        gbc.gridx = 1;
        btnClearImage = createMiniButton("Xóa", "icon_X.png");
        panel.add(btnClearImage, gbc);

        btnChooseImage.addActionListener(this::chooseImageFileAction);
        btnClearImage.addActionListener(this::clearImageFileAction);
        return panel;
    }

    private JButton createMiniButton(String text, String iconPath) {
        // ... (Giữ nguyên code của bạn, đảm bảo IconUtils được gọi đúng)
        ImageIcon icon = IconUtils.createImageIcon(iconPath, text, 16, 16); // Giả sử icon 16x16
        JButton button = new JButton(text, icon);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(5);
        button.setMargin(new Insets(5, 8, 5, 8));
        return button;
    }

    private void loadQuestionData() {
        // ... (Giữ nguyên code của bạn, nhưng thêm reset cho audio)
        stopCurrentAudio(); // Dừng audio cũ trước khi load
        if (currentQuestion != null && currentQuestion.getQuestionId() != 0) {
            txtQuestionText.setText(currentQuestion.getQuestionText());
            cmbQuestionType.setSelectedItem(currentQuestion.getQuestionType());
            cmbDifficultyLevel.setSelectedItem(currentQuestion.getDifficultyLevel());
            answersPanel.setAnswers(currentQuestion.getAnswers() != null ? currentQuestion.getAnswers() : new ArrayList<>());
            txtAiSuggestion.setText(currentQuestion.getAiSuggestedAnswer() != null ? currentQuestion.getAiSuggestedAnswer() : "");
            txtNotes.setText(currentQuestion.getNotes());

            if (currentQuestion.getAudioFile() != null && currentQuestion.getAudioFile().getFilePath() != null) {
                selectedAudioFile = new File(currentQuestion.getAudioFile().getFilePath());
                txtAudioFilePath.setText(selectedAudioFile.getName());
                btnPlayAudio.setEnabled(selectedAudioFile.exists());
                btnPauseResumeAudio.setEnabled(false);
            } else {
                clearAudioFileAction(null); // Gọi hàm clear để reset UI và trạng thái
            }
            if (currentQuestion.getImageFile() != null && currentQuestion.getImageFile().getFilePath() != null) {
                selectedImageFile = new File(currentQuestion.getImageFile().getFilePath());
                txtImageFilePath.setText(selectedImageFile.getName());
                displayImagePreview(selectedImageFile);
            } else {
                clearImageFileAction(null);
            }
        } else { // Câu hỏi mới
             answersPanel.setAnswers(new ArrayList<>()); // Khởi tạo với 2 lựa chọn mặc định
             txtQuestionText.setText("");
             cmbQuestionType.setSelectedIndex(0);
             cmbDifficultyLevel.setSelectedIndex(0);
             txtAiSuggestion.setText("");
             txtNotes.setText("");
             clearAudioFileAction(null);
             clearImageFileAction(null);
        }
        updatePanelsVisibility((String) cmbQuestionType.getSelectedItem());
        updateAISuggestButtonState((String) cmbQuestionType.getSelectedItem());
    }

    private void saveAction(ActionEvent e) {
     
        if (txtQuestionText.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nội dung câu hỏi không được để trống.", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            txtQuestionText.requestFocus();
            return;
        }
     

        currentQuestion.setQuestionText(txtQuestionText.getText().trim());
        currentQuestion.setQuestionType((String) cmbQuestionType.getSelectedItem());
        currentQuestion.setDifficultyLevel((String) cmbDifficultyLevel.getSelectedItem());

        boolean isMcqType = "Trắc nghiệm".equals(currentQuestion.getQuestionType()) ||
                            "Ngữ pháp".equals(currentQuestion.getQuestionType()) ||
                            "Từ vựng".equals(currentQuestion.getQuestionType()) ||
                            "Kanji".equals(currentQuestion.getQuestionType());

        if (isMcqType) {
            List<Answer> collectedAnswers = answersPanel.getAnswers();
            if (collectedAnswers.isEmpty() || collectedAnswers.stream().noneMatch(Answer::isCorrect)) {
                JOptionPane.showMessageDialog(this, "Câu hỏi loại này phải có ít nhất một lựa chọn và một đáp án đúng.", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }
            currentQuestion.setAnswers(collectedAnswers);
        } else {
            currentQuestion.setAnswers(new ArrayList<>()); // Xóa/đặt rỗng danh sách đáp án nếu không phải loại dùng AnswersPanel
        }

  
        currentQuestion.setNotes(txtNotes.getText().trim());

       

        saved = true;
        stopCurrentAudio(); // Dừng nhạc trước khi đóng
        dispose();
    }

    // --- Các phương thức xử lý file âm thanh ---
    private void chooseAudioFileAction(ActionEvent e) {
        // ... (Giữ nguyên code của bạn, nhưng gọi stopCurrentAudio())
        stopCurrentAudio();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file âm thanh (MP3, WAV)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Audio Files (*.mp3, *.wav)", "mp3", "wav"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedAudioFile = fileChooser.getSelectedFile();
            txtAudioFilePath.setText(selectedAudioFile.getName());
            btnPlayAudio.setEnabled(true);
            btnPauseResumeAudio.setEnabled(false); // Reset nút pause
            audioClipPosition = 0; // Reset vị trí
        }
    }

    private void playAudioAction(ActionEvent e) {
        // ... (Sử dụng phiên bản đã sửa lỗi và có thể phát MP3 nếu thư viện SPI được thêm)
        if (audioClip != null && audioClip.isRunning()) { // Nếu đang phát -> Dừng
            stopCurrentAudio();
            return;
        }
        // Nếu đang pause và nhấn Play -> phát lại từ đầu (hoặc từ vị trí pause tùy logic bạn muốn)
        if (isAudioPaused && audioClip != null && audioClip.isOpen()) {
            audioClipPosition = 0; // Phát lại từ đầu
            // Hoặc giữ nguyên audioClipPosition để tiếp tục từ chỗ pause
        }


        File audioFileToPlay = null;
        if (selectedAudioFile != null && selectedAudioFile.exists()) {
            audioFileToPlay = selectedAudioFile;
        } else if (currentQuestion != null && currentQuestion.getAudioFile() != null &&
                   currentQuestion.getAudioFile().getFilePath() != null &&
                   !currentQuestion.getAudioFile().getFilePath().isEmpty()) {
            audioFileToPlay = new File(currentQuestion.getAudioFile().getFilePath());
        }

        if (audioFileToPlay != null && audioFileToPlay.exists() && audioFileToPlay.isFile()) {
            stopCurrentAudio(); // Dừng clip cũ trước khi phát clip mới hoặc phát lại
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFileToPlay);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);

                if (!AudioSystem.isLineSupported(info)) {
                    System.err.println("Định dạng audio không được hỗ trợ trực tiếp: " + format);
                    JOptionPane.showMessageDialog(this,
                            "Định dạng file âm thanh không được hỗ trợ trực tiếp.\n" +
                            "Hãy thử file WAV hoặc đảm bảo thư viện giải mã MP3 (MP3 SPI) đã được thêm vào.",
                            "Lỗi Phát Âm Thanh", JOptionPane.ERROR_MESSAGE);
                    audioStream.close();
                    return;
                }
                audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        if (!isAudioPaused) {
                            audioClipPosition = 0;
                            btnPlayAudio.setText("Phát");
                            btnPlayAudio.setIcon(IconUtils.createImageIcon("play.png", "Phát", 16, 16));
                            btnPauseResumeAudio.setText("Tạm dừng");
                            btnPauseResumeAudio.setIcon(IconUtils.createImageIcon("pause.png", "Tạm dừng", 16,16));
                            btnPauseResumeAudio.setEnabled(false);
                        }
                    } else if (event.getType() == LineEvent.Type.START) {
                        btnPlayAudio.setText("Dừng");
                        btnPlayAudio.setIcon(IconUtils.createImageIcon("stop.png", "Dừng", 16, 16)); // Cần icon stop
                        btnPauseResumeAudio.setEnabled(true);
                        isAudioPaused = false;
                        btnPauseResumeAudio.setText("Tạm dừng");
                         btnPauseResumeAudio.setIcon(IconUtils.createImageIcon("pause.png", "Tạm dừng", 16,16));
                    }
                });
                audioClip.open(audioStream);
                audioClip.setMicrosecondPosition(audioClipPosition);
                audioClip.start();
            } catch (UnsupportedAudioFileException uafe) {
                uafe.printStackTrace(); JOptionPane.showMessageDialog(this, "Không hỗ trợ định dạng file âm thanh này: " + uafe.getMessage() + "\nVui lòng thử file WAV hoặc cấu hình MP3 SPI.", "Lỗi Định Dạng", JOptionPane.ERROR_MESSAGE);
            } catch (LineUnavailableException lue) {
                lue.printStackTrace(); JOptionPane.showMessageDialog(this, "Không thể mở line âm thanh: " + lue.getMessage(), "Lỗi Âm Thanh", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ioe) {
                ioe.printStackTrace(); JOptionPane.showMessageDialog(this, "Lỗi I/O khi đọc file âm thanh: " + ioe.getMessage(), "Lỗi File", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "File âm thanh không tồn tại hoặc chưa được chọn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            btnPlayAudio.setEnabled(false);
            btnPauseResumeAudio.setEnabled(false);
        }
    }

    private void pauseResumeAudioAction(ActionEvent e) {
      
        if (audioClip == null) return;
        if (isAudioPaused) {
            if (audioClip.isOpen()) {
                audioClip.setMicrosecondPosition(audioClipPosition);
                audioClip.start();
                isAudioPaused = false;
                btnPauseResumeAudio.setText("Tạm dừng");
                btnPauseResumeAudio.setIcon(IconUtils.createImageIcon("pause.png", "Tạm dừng", 16,16));
                btnPlayAudio.setText("Dừng");
                btnPlayAudio.setIcon(IconUtils.createImageIcon("stop.png", "Dừng", 16,16));
            }
        } else {
            if (audioClip.isRunning()) {
                audioClipPosition = audioClip.getMicrosecondPosition();
                audioClip.stop();
                isAudioPaused = true;
                btnPauseResumeAudio.setText("Tiếp tục");
                btnPauseResumeAudio.setIcon(IconUtils.createImageIcon("play.png", "Tiếp tục", 16,16)); // Cần icon resume
                btnPlayAudio.setText("Phát");
                btnPlayAudio.setIcon(IconUtils.createImageIcon("play.png", "Phát", 16,16));
            }
        }
    }

    private void stopCurrentAudio() {
        // ... (Giữ nguyên code từ phản hồi trước)
        if (audioClip != null) {
            if (audioClip.isRunning() || audioClip.isOpen()) {
                audioClip.stop();
                audioClip.close();
            }
            audioClip = null;
            isAudioPaused = false;
            audioClipPosition = 0;
            btnPlayAudio.setText("Phát");
            if (IconUtils.class != null) { // Thêm kiểm tra null cho an toàn
                btnPlayAudio.setIcon(IconUtils.createImageIcon("play.png", "Phát", 16, 16));
                btnPauseResumeAudio.setText("Tạm dừng");
                btnPauseResumeAudio.setIcon(IconUtils.createImageIcon("pause.png", "Tạm dừng", 16,16));
            }
            btnPauseResumeAudio.setEnabled(false);
        }
    }

    private void clearAudioFileAction(ActionEvent e) {
        // ... (Giữ nguyên code của bạn, nhưng gọi stopCurrentAudio())
        stopCurrentAudio();
        selectedAudioFile = null;
        if (txtAudioFilePath != null) txtAudioFilePath.setText("");
        if (btnPlayAudio != null) btnPlayAudio.setEnabled(false);
        if (btnPauseResumeAudio != null) btnPauseResumeAudio.setEnabled(false);
        if (currentQuestion != null) {
            currentQuestion.setAudioFile(null);
        }
    }

    // --- Các phương thức xử lý file hình ảnh ---
    private void chooseImageFileAction(ActionEvent e) {
        // ... (Giữ nguyên code của bạn)
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file hình ảnh (JPG, PNG, GIF)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            txtImageFilePath.setText(selectedImageFile.getName());
            displayImagePreview(selectedImageFile);
        }
    }

    private void displayImagePreview(File imageFile) {
        // ... (Giữ nguyên code của bạn)
        if (lblImagePreview == null) return;
        if (imageFile != null && imageFile.exists()) {
            try {
                ImageIcon imageIcon = new ImageIcon(imageFile.toURI().toURL());
                int previewHeight = lblImagePreview.getHeight();
                if (previewHeight <= 0) previewHeight = PREVIEW_SIZE - 40;

                int originalWidth = imageIcon.getIconWidth();
                int originalHeight = imageIcon.getIconHeight();
                int newWidth = PREVIEW_SIZE; // Mặc định là chiều rộng của preview area
                int newHeight = previewHeight; // Mặc định là chiều cao của preview area

                if (originalWidth > 0 && originalHeight > 0) {
                    double aspectRatio = (double) originalWidth / originalHeight;
                    // Tính toán newWidth và newHeight dựa trên aspectRatio để vừa PREVIEW_SIZE và previewHeight
                    if (originalWidth > PREVIEW_SIZE || originalHeight > previewHeight) { // Nếu ảnh lớn hơn khu vực preview
                        if ((double)PREVIEW_SIZE / aspectRatio <= previewHeight) { // Scale theo chiều rộng
                            newWidth = PREVIEW_SIZE;
                            newHeight = (int) (newWidth / aspectRatio);
                        } else { // Scale theo chiều cao
                            newHeight = previewHeight;
                            newWidth = (int) (newHeight * aspectRatio);
                        }
                    } else { // Nếu ảnh nhỏ hơn hoặc bằng khu vực preview, giữ nguyên kích thước
                        newWidth = originalWidth;
                        newHeight = originalHeight;
                    }
                }
                Image image = imageIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
                lblImagePreview.setIcon(new ImageIcon(image));
                lblImagePreview.setText("");
            } catch (MalformedURLException ex) {
                ex.printStackTrace(); lblImagePreview.setIcon(null); lblImagePreview.setText("Lỗi URL ảnh");
            } catch (Exception e) {
                e.printStackTrace(); lblImagePreview.setIcon(null); lblImagePreview.setText("Lỗi ảnh");
            }
        } else {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Xem trước");
        }
    }

    private void clearImageFileAction(ActionEvent e) {
        // ... (Giữ nguyên code của bạn)
        selectedImageFile = null;
        if (txtImageFilePath != null) txtImageFilePath.setText("");
        if (lblImagePreview != null) {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("Xem trước");
        }
        if (currentQuestion != null) {
            currentQuestion.setImageFile(null);
        }
    }

    // --- Gợi ý AI ---
    private void suggestAnswerAIAction(ActionEvent e) {
        
        String questionContent = txtQuestionText.getText().trim();
        if (questionContent.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập nội dung câu hỏi.", "Thiếu Nội Dung", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Answer> currentAnswerObjects = answersPanel.getAnswers();
        List<String> answerTexts = currentAnswerObjects.stream()
                                     .map(Answer::getAnswerText)
                                     .filter(text -> text != null && !text.trim().isEmpty())
                                     .collect(Collectors.toList());

        if (answerTexts.size() < 2) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ít nhất 2 lựa chọn đáp án.", "Thiếu Lựa Chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSuggestAnswerAI.setEnabled(false);
        btnSuggestAnswerAI.setText("Đang xử lý AI...");
        txtAiSuggestion.setText("Đang lấy gợi ý từ AI, vui lòng chờ...");

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Đảm bảo AIAnswerSuggester và lớp gọi AI của bạn (ví dụ GeminiConnect) hoạt động đúng
                return AIAnswerSuggester.suggestAnswer(questionContent, answerTexts);
            }

            @Override
            protected void done() {
                try {
                    String aiResult = get(); // Lấy kết quả từ doInBackground

                    // Lấy danh sách các đối tượng AnswerEntryPanel từ AnswersPanel
                    // và danh sách nội dung text của các lựa chọn hiện tại trên UI
                    List<AnswerEntryPanel> uiEntryPanels = answersPanel.getAnswerEntryPanels();
                    List<String> uiAnswerTexts = new ArrayList<>();
                    if (uiEntryPanels != null) {
                        for (AnswerEntryPanel entryPanel : uiEntryPanels) {
                            if (entryPanel != null && entryPanel.getAnswerTextField() != null) {
                                uiAnswerTexts.add(entryPanel.getAnswerTextField().getText().trim());
                            } else {
                                uiAnswerTexts.add(""); // Thêm chuỗi rỗng nếu có panel null hoặc textfield null
                            }
                        }
                    }

                    if (aiResult != null && !aiResult.toLowerCase().startsWith("lỗi:")) {
                        String[] lines = aiResult.split("\\n", 2);
                        String suggestedCorrectAnswerContent = (lines.length > 0) ? lines[0].trim() : "";
                  

                        txtAiSuggestion.setText("Đáp án AI gợi ý: " + suggestedCorrectAnswerContent);

                        int suggestedIndex = -1;

                        // Log để gỡ lỗi
                        System.out.println( suggestedCorrectAnswerContent + "'");
                        System.out.println("[DEBUG AI] Số lượng lựa chọn trên UI (uiAnswerTexts): " + uiAnswerTexts.size());
                        for (int i = 0; i < uiAnswerTexts.size(); i++) {
                            System.out.println("[DEBUG AI] Lựa chọn UI " + i + ": '" + uiAnswerTexts.get(i) + "'");
                        }

                        // Ưu tiên 1: Thử khớp trực tiếp nội dung text của đáp án
                        if (!suggestedCorrectAnswerContent.isEmpty()) {
                            for (int i = 0; i < uiAnswerTexts.size(); i++) {
                                if (uiAnswerTexts.get(i).equalsIgnoreCase(suggestedCorrectAnswerContent)) {
                                    suggestedIndex = i;
                                    System.out.println("[DEBUG AI] Khớp trực tiếp nội dung tại index: " + suggestedIndex);
                                    break;
                                }
                            }
                        }

                        // Ưu tiên 2: Nếu không khớp trực tiếp, thử khớp với "選択肢 X" hoặc "Lựa chọn X"
                        if (suggestedIndex == -1 && suggestedCorrectAnswerContent.matches("(?i)(|Lựa chọn|Option)\\s*\\d+")) {
                             try {
                                String numberPart = suggestedCorrectAnswerContent.replaceAll("[^0-9]", "");
                                if (!numberPart.isEmpty()) {
                                    int choiceNumber = Integer.parseInt(numberPart);
                                    suggestedIndex = choiceNumber - 1;
                                    System.out.println("[DEBUG AI] Khớp dạng 'Lựa chọn X' tại index: " + suggestedIndex);
                                }
                            } catch (NumberFormatException ignored) {
                                System.out.println("[DEBUG AI] Lỗi khi parse số từ: '" + suggestedCorrectAnswerContent + "'");
                            }
                        }

                        // Ưu tiên 3: Nếu AI trả về chỉ một chữ cái A, B, C, D
                        if (suggestedIndex == -1 && suggestedCorrectAnswerContent.matches("^[A-Da-d]$")) {
                            char choiceChar = suggestedCorrectAnswerContent.toUpperCase().charAt(0);
                            suggestedIndex = choiceChar - 'A';
                            System.out.println("[DEBUG AI] Khớp dạng chữ cái A/B/C/D tại index: " + suggestedIndex);
                        }

                      

                        // Sau khi đã có suggestedIndex, tiến hành chọn RadioButton
                        if (uiEntryPanels != null && suggestedIndex >= 0 && suggestedIndex < uiEntryPanels.size()) {
                            // Bỏ chọn tất cả các RadioButton khác trước khi chọn cái mới
                            for (AnswerEntryPanel entryPanel : uiEntryPanels) {
                                if (entryPanel != null && entryPanel.getCorrectRadioButton() != null) {
                                    entryPanel.getCorrectRadioButton().setSelected(false);
                                }
                            }
                            // Chọn RadioButton được gợi ý
                            AnswerEntryPanel targetPanel = uiEntryPanels.get(suggestedIndex);
                            if (targetPanel != null && targetPanel.getCorrectRadioButton() != null) {
                                targetPanel.getCorrectRadioButton().setSelected(true);
                                JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                                        "AI đã gợi ý đáp án và tự động chọn. Vui lòng kiểm tra lại!",
                                        "AI Gợi Ý Thành Công", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                 System.err.println("[DEBUG AI ERROR] TargetPanel hoặc RadioButton bị null tại index: " + suggestedIndex);
                                 JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                                        "AI đã cung cấp gợi ý nhưng có lỗi khi tự động chọn. Vui lòng kiểm tra và tự chọn đáp án đúng.",
                                        "AI Gợi Ý", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else if (!suggestedCorrectAnswerContent.isEmpty()) {
                            JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                                    "AI đã cung cấp gợi ý nhưng không thể tự động khớp với lựa chọn nào trên giao diện. Vui lòng kiểm tra và tự chọn đáp án đúng.",
                                    "AI Gợi Ý", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                             JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                                    "AI không đưa ra được gợi ý đáp án cụ thể từ phản hồi nhận được.",
                                    "AI Gợi Ý", JOptionPane.INFORMATION_MESSAGE);
                        }

                    } else { // aiResult là null hoặc bắt đầu bằng "Lỗi:"
                        txtAiSuggestion.setText(aiResult != null ? aiResult : "Lỗi: Không nhận được phản hồi từ AI.");
                        JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                                (aiResult != null ? aiResult : "Lỗi: Không nhận được phản hồi từ AI."),
                                "Lỗi Gợi Ý AI", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    txtAiSuggestion.setText("Lỗi khi xử lý phản hồi từ AI: " + ex.getMessage());
                    JOptionPane.showMessageDialog(QuestionEditorDialog.this,
                            "Lỗi khi xử lý phản hồi từ AI: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSuggestAnswerAI.setText("Gợi ý Đáp án từ AI");
                    updateAISuggestButtonState((String)cmbQuestionType.getSelectedItem());
                }
            }
        };
        worker.execute();
    }


    // --- Getters cho Controller ---
    public boolean isSaved() { return saved; }
    public Question getQuestion() { return currentQuestion; }
    public File getSelectedAudioFile() { return selectedAudioFile; }
    public File getSelectedImageFile() { return selectedImageFile; }

    // --- Setters 
    public void setSelectedAudioFile(File file) {
        this.selectedAudioFile = file;
        if (txtAudioFilePath != null && btnPlayAudio != null) {
            if (file != null && file.exists()) {
                txtAudioFilePath.setText(file.getName());
                btnPlayAudio.setEnabled(true);
            } else {
                txtAudioFilePath.setText("");
                btnPlayAudio.setEnabled(false);
            }
            if(btnPauseResumeAudio != null) btnPauseResumeAudio.setEnabled(false);
            isAudioPaused = false;
            audioClipPosition = 0;
        }
    }

    public void setSelectedImageFile(File file) {
        this.selectedImageFile = file;
         if (txtImageFilePath != null && lblImagePreview != null) {
            if (file != null) {
                txtImageFilePath.setText(file.getName());
                displayImagePreview(file);
            } else {
                txtImageFilePath.setText("");
                lblImagePreview.setIcon(null);
                lblImagePreview.setText("Xem trước");
            }
        }
    }

    private Font findAvailableFont(String[] fontNames, int defaultSize) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] availableFontFamilyNames = ge.getAvailableFontFamilyNames();
        List<String> systemFonts = Arrays.asList(availableFontFamilyNames);

        for (String preferredFont : fontNames) {
            if (systemFonts.contains(preferredFont)) {
                Font testFont = new Font(preferredFont, Font.PLAIN, defaultSize);
                // Kiểm tra sơ bộ khả năng hiển thị một vài ký tự đại diện
                if (testFont.canDisplay('語') && testFont.canDisplay('ệ') && testFont.canDisplay('A')) {
                    return testFont;
                }
            }
        }
        // Nếu là font logic, trả về trực tiếp
        if (Arrays.asList(fontNames).contains(Font.SANS_SERIF) || 
            Arrays.asList(fontNames).contains(Font.SERIF) ||
            Arrays.asList(fontNames).contains(Font.MONOSPACED)) {
            // Lấy font logic cuối cùng trong danh sách ưu tiên nếu có
            for (int i = fontNames.length - 1; i >= 0; i--) {
                if (fontNames[i].equals(Font.SANS_SERIF) || fontNames[i].equals(Font.SERIF) || fontNames[i].equals(Font.MONOSPACED)) {
                    return new Font(fontNames[i], Font.PLAIN, defaultSize);
                }
            }
        }
        return null; // Trả về null nếu không có font nào phù hợp trong danh sách ưu tiên (trừ font logic)
    }
    
    @Override
    public void dispose() {
        stopCurrentAudio(); // Dừng nhạc khi dialog đóng
        super.dispose();
    }
}