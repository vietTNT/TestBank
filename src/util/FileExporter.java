package util;

import model.Test;
import model.Question;
import model.Answer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.BreakType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class FileExporter {

    private static final String UNICODE_FONT_PATH = "/fonts/arialuni.ttf"; // Đảm bảo font này có trong resources/fonts
    private static final String DEFAULT_FONT_FAMILY = "Arial Unicode MS";
    /**
     * Xuất một đề thi ra file PDF, có tùy chọn bao gồm phần đáp án ở cuối.
     * @param test Đối tượng Test chứa thông tin đề thi.
     * @param file File đích để lưu PDF.
     * @param includeAnswers True nếu muốn bao gồm phần đáp án ở cuối, False nếu chỉ xuất đề.
     * @throws IOException Nếu có lỗi khi ghi file hoặc load font.
     */
    public static void exportTestToPdf(Test test, File file, boolean includeAnswers) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType0Font unicodeFont;
            try {
                InputStream fontStream = FileExporter.class.getResourceAsStream(UNICODE_FONT_PATH);
                if (fontStream == null) {
                    throw new IOException("Không tìm thấy file font Unicode tại classpath: " + UNICODE_FONT_PATH);
                }
                unicodeFont = PDType0Font.load(document, fontStream);
            } catch (IOException e) {
                System.err.println("Lỗi nghiêm trọng khi tải font cho PDF: " + e.getMessage());
                throw new IOException("Không thể tải font cần thiết để tạo PDF. " + e.getMessage(), e);
            }

            PDPageContentStream contentStream = null;
            
            try {
                contentStream = new PDPageContentStream(document, page);
                
                float margin = 50;
                float yStart = page.getMediaBox().getHeight() - margin;
                float yPosition = yStart;
                float leading = 18f; 
                float fontSize = 12f;
                float titleFontSize = 18f;
                float pageWidth = page.getMediaBox().getWidth() - 2 * margin;

                // 1. Tiêu đề Đề thi
                contentStream.beginText();
                contentStream.setFont(unicodeFont, titleFontSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Đề thi: " + test.getTestName());
                contentStream.endText();
                yPosition -= leading * 1.8f;

                // 2. Mô tả
                if (test.getDescription() != null && !test.getDescription().isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(unicodeFont, fontSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                    yPosition -= addWrappedText(contentStream, test.getDescription(), margin, yPosition, pageWidth, unicodeFont, fontSize, leading, false);
                    contentStream.endText();
                    yPosition -= leading; 
                }
                
                // Thêm dòng kẻ ngang phân cách
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(margin + pageWidth, yPosition);
                contentStream.stroke();
                yPosition -= leading;


                // 3. Danh sách câu hỏi
                if (test.getQuestionsInTest() != null) {
                    int qNum = 1;
                    for (Question q : test.getQuestionsInTest()) {
                        float estimatedHeightForQuestion = calculateEstimatedHeight(q, pageWidth, unicodeFont, fontSize, leading);
                        if (yPosition - estimatedHeightForQuestion < margin) {
                            contentStream.close(); 
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            contentStream = new PDPageContentStream(document, page);
                            yPosition = yStart;
                        }
                        
                        contentStream.beginText();
                        contentStream.setFont(unicodeFont, fontSize);
                        contentStream.newLineAtOffset(margin, yPosition);
                        String questionHeader = qNum++ + ". " + q.getQuestionText();
                        yPosition -= addWrappedText(contentStream, questionHeader, margin, yPosition, pageWidth, unicodeFont, fontSize, leading, true);
                        contentStream.endText();

                        if ("Trắc nghiệm".equals(q.getQuestionType()) && q.getAnswers() != null) {
                            char optionChar = 'A';
                            for (Answer ans : q.getAnswers()) {
                                String answerFullText = "  " + (optionChar++) + ". " + ans.getAnswerText();
                                float estimatedAnswerHeight = calculateEstimatedHeightForText(answerFullText, pageWidth - 20, unicodeFont, fontSize, leading);
                                if (yPosition - estimatedAnswerHeight < margin) { 
                                    contentStream.close();
                                    page = new PDPage(PDRectangle.A4);
                                    document.addPage(page);
                                    contentStream = new PDPageContentStream(document, page);
                                    yPosition = yStart;
                                    // Cần bắt đầu lại text block cho trang mới nếu các đáp án bị ngắt
                                    contentStream.beginText();
                                    contentStream.setFont(unicodeFont, fontSize);
                                    contentStream.newLineAtOffset(margin + 20, yPosition); // Đặt lại vị trí đầu dòng
                                } else if (optionChar > 'A' +1) { // Nếu không phải đáp án đầu tiên của câu hỏi này trên trang hiện tại
                                     contentStream.newLineAtOffset(0, -leading); // Chỉ di chuyển xuống
                                     yPosition -=leading;
                                } else { // Đáp án đầu tiên của câu hỏi này trên trang hiện tại
                                    contentStream.endText(); // Kết thúc khối text cũ (nếu có)
                                    contentStream.beginText();
                                    contentStream.setFont(unicodeFont, fontSize);
                                    contentStream.newLineAtOffset(margin + 20, yPosition);
                                }
                                
                                List<String> answerLines = new ArrayList<>();
                                splitTextToLines(answerFullText, pageWidth - 20, unicodeFont, fontSize, answerLines);
                                float heightUsedThisBlock = 0;
                                for(int i=0; i<answerLines.size(); i++){
                                    if(i>0) {
                                        contentStream.newLineAtOffset(0, -leading);
                                        yPosition -= leading;
                                    }
                                    contentStream.showText(answerLines.get(i));
                                    heightUsedThisBlock += leading;
                                }
                                // yPosition -= heightUsedThisBlock; // Đã trừ trong vòng lặp
                                contentStream.endText(); // Kết thúc khối text cho mỗi đáp án
                                // Không cần yPosition -= leading ở đây nữa vì đã trừ trong vòng lặp
                            }
                        }
                        yPosition -= leading * 0.8f; 
                    }
                }

                // 4. Thêm phần đáp án nếu được yêu cầu
                if (includeAnswers) {
                    yPosition -= leading * 1.5f; // Khoảng cách lớn trước phần đáp án
                    if (yPosition < margin + leading * 5) { // Kiểm tra trang mới cho tiêu đề đáp án
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = yStart;
                    }

                    contentStream.beginText();
                    contentStream.setFont(unicodeFont, 16); // Font to hơn cho tiêu đề đáp án
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText("ĐÁP ÁN");
                    contentStream.endText();
                    yPosition -= leading * 1.5f;
                    
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(margin + pageWidth, yPosition);
                    contentStream.stroke();
                    yPosition -= leading;


                    if (test.getQuestionsInTest() != null) {
                        int qNumAns = 1;
                        for (Question q : test.getQuestionsInTest()) {
                            String correctAnswerString = getCorrectAnswerString(q);
                            String answerLineText = qNumAns++ + ". " + correctAnswerString;
                            
                            float estimatedAnswerLineHeight = calculateEstimatedHeightForText(answerLineText, pageWidth, unicodeFont, fontSize, leading);
                            if (yPosition - estimatedAnswerLineHeight < margin) {
                                contentStream.close();
                                page = new PDPage(PDRectangle.A4);
                                document.addPage(page);
                                contentStream = new PDPageContentStream(document, page);
                                yPosition = yStart;
                            }
                            contentStream.beginText();
                            contentStream.setFont(unicodeFont, fontSize);
                            contentStream.newLineAtOffset(margin, yPosition);
                            yPosition -= addWrappedText(contentStream, answerLineText, margin, yPosition, pageWidth, unicodeFont, fontSize, leading, true);
                            contentStream.endText();
                        }
                    }
                }

            } finally {
                if (contentStream != null) {
                    contentStream.close();
                }
            }
            document.save(file);
        } 
        System.out.println("Xuất PDF thành công vào: " + file.getAbsolutePath());
    }

    // Phương thức helper để ngắt dòng và ghi văn bản vào PDF
    private static float addWrappedText(PDPageContentStream contentStream, String text, float x, float y, float maxWidth, PDType0Font font, float fontSize, float leading, boolean isFirstLineOfBlock) throws IOException {
        List<String> lines = new ArrayList<>();
        splitTextToLines(text, maxWidth, font, fontSize, lines);
        float totalHeightUsed = 0;

        for (int i = 0; i < lines.size(); i++) {
            if (i == 0 && !isFirstLineOfBlock) { // Nếu không phải dòng đầu của block, chỉ di chuyển xuống
                 contentStream.newLineAtOffset(0, -leading);
            } else if (i > 0) { // Các dòng tiếp theo của cùng block
                 contentStream.newLineAtOffset(0, -leading);
            }
            // Nếu là isFirstLineOfBlock và i=0, vị trí đã được set bởi newLineAtOffset trước khi gọi hàm này
            contentStream.showText(lines.get(i));
            totalHeightUsed += leading;
        }
        return totalHeightUsed;
    }
    
    // Phương thức helper để ước lượng chiều cao cần thiết cho một câu hỏi (bao gồm đáp án)
    private static float calculateEstimatedHeight(Question q, float pageWidth, PDType0Font font, float fontSize, float leading) throws IOException {
        float height = 0;
        List<String> lines = new ArrayList<>();
        splitTextToLines(q.getQuestionText(), pageWidth, font, fontSize, lines);
        height += lines.size() * leading;

        if ("Trắc nghiệm".equals(q.getQuestionType()) && q.getAnswers() != null) {
            for (Answer ans : q.getAnswers()) {
                lines.clear();
                splitTextToLines(ans.getAnswerText(), pageWidth - 20, font, fontSize, lines);
                height += lines.size() * leading;
            }
        }
        return height + (leading * 0.5f); // Thêm khoảng cách nhỏ
    }
    
    // Phương thức helper để ước lượng chiều cao cho một đoạn văn bản
    private static float calculateEstimatedHeightForText(String text, float pageWidth, PDType0Font font, float fontSize, float leading) throws IOException {
         List<String> lines = new ArrayList<>();
         splitTextToLines(text, pageWidth, font, fontSize, lines);
         return lines.size() * leading;
    }
    
    // Helper method to split text into lines that fit the page width
    private static void splitTextToLines(String text, float maxWidth, PDType0Font font, float fontSize, List<String> lines) throws IOException {
    	 if (text == null) return;
    	
    	
    	 String[] words = text.split("(?<=\\s|-|(?=\\p{Punct}))|(?<=\\p{Punct})(?=\\w)|(?<=\\p{Lo})(?=\\p{Lo})|(?<=\\p{Han})(?=\\p{Han})|(?<=\\p{Hiragana})(?=\\p{Hiragana})|(?<=\\p{Katakana})(?=\\p{Katakana})");
         StringBuilder currentLine = new StringBuilder();
         for (String word : words) {
             if (word == null || word.isEmpty()) continue;
             float width = font.getStringWidth(currentLine.toString() + word) / 1000 * fontSize;
             if (width > maxWidth && currentLine.length() > 0) {
                 lines.add(currentLine.toString().trim());
                 currentLine = new StringBuilder(word);
             } else {
                 if (currentLine.length() > 0 && !Character.isWhitespace(word.charAt(0)) && !Character.isWhitespace(currentLine.charAt(currentLine.length()-1)) && !isPunctuation(word.charAt(0)) && !isPunctuation(currentLine.charAt(currentLine.length()-1))) {
                     // Thêm khoảng trắng nếu từ hiện tại không bắt đầu bằng dấu câu và dòng hiện tại không kết thúc bằng dấu câu/khoảng trắng
                     // Điều này có thể không hoàn hảo cho mọi ngôn ngữ
                 }
                 currentLine.append(word);
             }
         }
         if (currentLine.length() > 0) {
             lines.add(currentLine.toString().trim());
         }
    }

    private static boolean isPunctuation(char c) {
        // Một số dấu câu cơ bản
        return ".?!,;:)]}".indexOf(c) != -1;
    }
    
    
    // Helper method to add a paragraph with line wrapping (cần được gọi trong beginText/endText context)
    private static float addParagraph(PDPageContentStream contentStream, String text, float x, float yPosition, float width, PDType0Font font, float fontSize, float leading) throws IOException {
        List<String> lines = new ArrayList<>();
        splitTextToLines(text, width, font, fontSize, lines);
        float totalHeightUsed = 0;
        // Không gọi newLineAtOffset ở đây vì nó sẽ được gọi cho từng dòng
        // contentStream.newLineAtOffset(x, yPosition); // Đặt vị trí ban đầu cho đoạn văn

        for (String line : lines) {
            contentStream.newLineAtOffset(x, yPosition - totalHeightUsed); // Đặt vị trí cho từng dòng
            contentStream.showText(line);
            contentStream.newLineAtOffset(-x, -leading); // Reset x về 0 so với lề và xuống dòng
            totalHeightUsed += leading;
        }
        return totalHeightUsed;
    }


    public static void exportTestToDocx(Test test, File file, boolean includeAnswersInSeparateSection) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             FileOutputStream fos = new FileOutputStream(file)) {

            XWPFParagraph titleP = document.createParagraph();
            titleP.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleP.createRun();
            titleRun.setText("Đề thi: " + (test.getTestName() != null ? test.getTestName() : "Không có tên"));
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily(DEFAULT_FONT_FAMILY);
            titleRun.addBreak();

            if (test.getDescription() != null && !test.getDescription().isEmpty()) {
                XWPFParagraph descP = document.createParagraph();
                descP.setAlignment(ParagraphAlignment.BOTH);
                XWPFRun descRun = descP.createRun();
                descRun.setText("Mô tả: " + test.getDescription());
                descRun.setFontSize(12);
                descRun.setFontFamily(DEFAULT_FONT_FAMILY);
                descRun.addBreak();
            }
            if (test.getCreatedAt() != null) {
                XWPFParagraph dateP = document.createParagraph();
                dateP.setAlignment(ParagraphAlignment.LEFT);
                XWPFRun dateRun = dateP.createRun();
                dateRun.setText("Ngày tạo: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(test.getCreatedAt()));
                dateRun.setFontSize(10);
                dateRun.setItalic(true);
                dateRun.setFontFamily(DEFAULT_FONT_FAMILY);
            }
            XWPFParagraph separatorAfterHeader = document.createParagraph();
            separatorAfterHeader.setBorderBottom(Borders.SINGLE);
            separatorAfterHeader.createRun().addBreak();


            // Danh sách câu hỏi
            if (test.getQuestionsInTest() != null && !test.getQuestionsInTest().isEmpty()) {
                int qNum = 1;
                for (Question q : test.getQuestionsInTest()) {
                    XWPFParagraph qP = document.createParagraph();
                    qP.setSpacingBefore(200);

                    // 1. Số thứ tự và Nội dung câu hỏi chính
                    XWPFRun qTextRun = qP.createRun();
                    qTextRun.setText(qNum++ + ". ");
                    qTextRun.setBold(true);
                    qTextRun.setFontSize(12);
                    qTextRun.setFontFamily(DEFAULT_FONT_FAMILY);

                    if (q.getQuestionText() != null && !q.getQuestionText().isEmpty()) {
                        String[] questionLines = q.getQuestionText().split("\\r?\\n");
                        for (int i = 0; i < questionLines.length; i++) {
                            XWPFRun lineRun = qP.createRun();
                            lineRun.setText(questionLines[i]);
                            lineRun.setFontSize(12);
                            lineRun.setFontFamily(DEFAULT_FONT_FAMILY);
                            if (i < questionLines.length - 1) {
                                lineRun.addBreak();
                            }
                        }
                    } else {
                        XWPFRun placeholderRun = qP.createRun();
                        placeholderRun.setText("[Nội dung câu hỏi bị thiếu]");
                        placeholderRun.setItalic(true);
                        placeholderRun.setFontSize(12);
                        placeholderRun.setFontFamily(DEFAULT_FONT_FAMILY);
                    }
                    
                    // Chỉ dẫn âm thanh (nếu có)
                    if (q.getAudioFile() != null && q.getAudioFile().getFilePath() != null) {
                        XWPFRun audioIndicatorRun = qP.createRun();
                        audioIndicatorRun.addBreak(); 
                        audioIndicatorRun.setText("    (Nghe audio: " + q.getAudioFile().getFileName() + ")");
                        audioIndicatorRun.setItalic(true);
                        audioIndicatorRun.setFontSize(10);
                        audioIndicatorRun.setFontFamily(DEFAULT_FONT_FAMILY);
                    }

                    // Hình ảnh đính kèm (nếu có) - Hiển thị NGAY SAU nội dung câu hỏi chính và chỉ dẫn audio
                    if (q.getImageFile() != null && q.getImageFile().getFilePath() != null && !q.getImageFile().getFilePath().isEmpty()) {
                        // Tạo một paragraph mới, riêng biệt cho hình ảnh để dễ căn chỉnh
                        XWPFParagraph imgParagraph = document.createParagraph();
                        imgParagraph.setAlignment(ParagraphAlignment.CENTER); // Căn giữa ảnh

                        File imageFile = new File(q.getImageFile().getFilePath());
                        System.out.println("[Exporter LOG] Kiểm tra ảnh: " + imageFile.getAbsolutePath());

                        if (imageFile.exists() && imageFile.isFile()) {
                            System.out.println("[Exporter LOG] Ảnh tồn tại và là file. Đang thử chèn: " + imageFile.getName());
                            try (InputStream fis = new FileInputStream(imageFile)) { // Quan trọng: InputStream phải được mở ở đây
                                XWPFRun imgRun = imgParagraph.createRun();

                                String fileName = imageFile.getName().toLowerCase();
                                int format; // Xác định định dạng ảnh
                                if (fileName.endsWith(".png")) {
                                    format = XWPFDocument.PICTURE_TYPE_PNG;
                                } else if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
                                    format = XWPFDocument.PICTURE_TYPE_JPEG;
                                } else if (fileName.endsWith(".gif")) {
                                    format = XWPFDocument.PICTURE_TYPE_GIF;
                                } else if (fileName.endsWith(".bmp")) {
                                    format = XWPFDocument.PICTURE_TYPE_BMP;
                                } else if (fileName.endsWith(".wmf")) {
                                    format = XWPFDocument.PICTURE_TYPE_WMF;
                                } else if (fileName.endsWith(".emf")) {
                                    format = XWPFDocument.PICTURE_TYPE_EMF;
                                } else {
                                    System.err.println("[Exporter WARNING] Định dạng ảnh không được hỗ trợ trực tiếp hoặc không xác định: " + fileName + ". Thử dùng PICTURE_TYPE_PNG.");
                                    format = XWPFDocument.PICTURE_TYPE_PNG; // Thử mặc định là PNG
                                }

                                BufferedImage bimg = ImageIO.read(new File(q.getImageFile().getFilePath())); // Đọc lại file để đảm bảo có thể đọc được
                                if (bimg == null) {
                                    throw new IOException("Không thể đọc file ảnh bằng ImageIO (kết quả null): " + imageFile.getAbsolutePath());
                                }
                                int origWidthPx = bimg.getWidth();
                                int origHeightPx = bimg.getHeight();

                                // Giới hạn chiều rộng tối đa của ảnh và giữ tỷ lệ
                                int displayWidthPx = Math.min(origWidthPx, 450); // Ví dụ: tối đa 450px
                                // Tính toán chiều cao tương ứng để giữ tỷ lệ
                                int displayHeightPx = (int) (((double) displayWidthPx / origWidthPx) * origHeightPx);

                                if (displayWidthPx <= 0 || displayHeightPx <= 0) {
                                    throw new IOException("Kích thước ảnh tính toán không hợp lệ: " + displayWidthPx + "x" + displayHeightPx);
                                }

                                System.out.println("[Exporter LOG] Chèn ảnh: " + fileName + " với kích thước (px): " + displayWidthPx + "x" + displayHeightPx + ", format code: " + format);
                                imgRun.addPicture(fis, format, imageFile.getName(), Units.toEMU(displayWidthPx), Units.toEMU(displayHeightPx));

                            } catch (Exception ex) { // Bắt Exception rộng hơn để gỡ lỗi
                                System.err.println("[Exporter ERROR] Lỗi nghiêm trọng khi thêm hình ảnh '" + q.getImageFile().getFileName() + "' vào DOCX:");
                                ex.printStackTrace(); // In chi tiết lỗi ra console
                                XWPFRun errorRun = imgParagraph.createRun(); // Thêm text lỗi vào imgParagraph
                                errorRun.setText("[Lỗi hiển thị hình ảnh: " + q.getImageFile().getFileName() + " - " + ex.getClass().getSimpleName() + "]");
                                errorRun.setItalic(true); errorRun.setFontSize(10); errorRun.setFontFamily(DEFAULT_FONT_FAMILY);
                            }
                        } else {
                            System.out.println("[Exporter LOG] File ảnh không tồn tại hoặc không phải file: " + imageFile.getAbsolutePath());
                            XWPFParagraph errorImgParagraph = document.createParagraph();
                            errorImgParagraph.setAlignment(ParagraphAlignment.CENTER);
                            XWPFRun errorRun = errorImgParagraph.createRun();
                            errorRun.setText("[Hình ảnh không tìm thấy tại: " + q.getImageFile().getFilePath() + "]");
                            errorRun.setItalic(true); errorRun.setFontSize(10); errorRun.setFontFamily(DEFAULT_FONT_FAMILY);
                        }
                         // Tạo một paragraph trống sau ảnh để đảm bảo xuống dòng đúng cách
                        document.createParagraph();
                    }


                    // *** PHẦN THAY ĐỔI QUAN TRỌNG ĐỂ HIỂN THỊ LỰA CHỌN ĐÁP ÁN ***
                    // Hiển thị các lựa chọn trả lời ngay bên dưới câu hỏi (và ảnh nếu có)
                    // Áp dụng cho các loại câu hỏi có danh sách `answers` (ví dụ: "Trắc nghiệm", "Ngữ pháp", "Từ vựng", "Kanji")
                    // và KHÔNG hiển thị đâu là đáp án đúng.
                    if (q.getAnswers() != null && !q.getAnswers().isEmpty()) {
                        // Thêm một dòng trống nhỏ trước các lựa chọn nếu câu hỏi chính hoặc ảnh đã được hiển thị
                        if ((q.getQuestionText() != null && !q.getQuestionText().isEmpty()) || q.getImageFile() != null) {
                            // Có thể tạo một paragraph mới chỉ để tạo khoảng cách nếu cần
                            // document.createParagraph().createRun().addBreak(); // Hoặc
                            qP.createRun().addBreak(); // Thêm break vào paragraph câu hỏi hiện tại
                        }

                        char optionChar = 'A';
                        for (Answer ans : q.getAnswers()) {
                            XWPFParagraph ansP = document.createParagraph(); // Paragraph mới cho mỗi lựa chọn
                            ansP.setIndentationLeft(720); // Thụt lề cho đáp án
                            XWPFRun ansRun = ansP.createRun();
                            ansRun.setText((optionChar++) + ". " + (ans.getAnswerText() != null ? ans.getAnswerText() : "[Lựa chọn bị thiếu]"));
                            ansRun.setFontSize(12);
                            ansRun.setFontFamily(DEFAULT_FONT_FAMILY);
                        }
                    }
//                    else if (!"Nghe hiểu".equals(q.getQuestionType()) && !"Đọc hiểu".equals(q.getQuestionType()) && !"Bảng Chữ Cái".equals(q.getQuestionType())) {
//                        // Đối với các loại câu hỏi khác không có sẵn danh sách lựa chọn
//                        // và không phải là các loại hình đặc thù như nghe/đọc/bảng chữ cái,
//                        // bạn có thể thêm dòng để người dùng tự điền câu trả lời.
//                        qP.createRun().addBreak();
//                        XWPFRun answerLineRun = qP.createRun(); // Thêm vào paragraph câu hỏi hiện tại
//                        answerLineRun.setText("Trả lời: _________________________________________");
//                        // answerLineRun.addBreak(); // Không cần break nữa nếu đây là phần cuối của câu hỏi
//                    }
                }
            } else {
                XWPFParagraph noQuestionsP = document.createParagraph();
                noQuestionsP.createRun().setText("Đề thi này hiện chưa có câu hỏi nào.");
                noQuestionsP.createRun().setFontFamily(DEFAULT_FONT_FAMILY);
            }

            // Phần đáp án riêng ở cuối (nếu includeAnswersInSeparateSection là true)
            if (includeAnswersInSeparateSection) {
          
                XWPFParagraph pageBreakP = document.createParagraph();
                pageBreakP.setPageBreak(true);

                XWPFParagraph answerTitleP = document.createParagraph();
                answerTitleP.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun answerTitleRun = answerTitleP.createRun();
                answerTitleRun.setText("ĐÁP ÁN");
                answerTitleRun.setBold(true);
                answerTitleRun.setFontSize(16);
                answerTitleRun.setFontFamily(DEFAULT_FONT_FAMILY);
                answerTitleRun.addBreak();

                XWPFParagraph answerSeparator = document.createParagraph();
                answerSeparator.setBorderBottom(Borders.SINGLE);

                if (test.getQuestionsInTest() != null && !test.getQuestionsInTest().isEmpty()) {
                    int qNumAns = 1;
                    for (Question q : test.getQuestionsInTest()) {
                        XWPFParagraph ansKeyP = document.createParagraph();
                        XWPFRun ansKeyRun = ansKeyP.createRun();
                        ansKeyRun.setText(qNumAns++ + ". " + getCorrectAnswerString(q));
                        ansKeyRun.setFontSize(12);
                        ansKeyRun.setFontFamily(DEFAULT_FONT_FAMILY);
                    }
                } else {
                     XWPFParagraph noAnsP = document.createParagraph();
                     noAnsP.createRun().setText("Không có câu hỏi nào để hiển thị đáp án.");
                     noAnsP.createRun().setFontFamily(DEFAULT_FONT_FAMILY);
                }
            }
            document.write(fos);
        }
        System.out.println("Xuất DOCX thành công vào: " + file.getAbsolutePath());
    }

    // getCorrectAnswerString giữ nguyên để dùng cho phần ĐÁP ÁN RIÊNG
    private static String getCorrectAnswerString(Question q) {
        if (q == null) return "[Câu hỏi không hợp lệ]";
        // Mở rộng điều kiện này để bao gồm các loại câu hỏi bạn muốn hiển thị lựa chọn đáp án trong phần "ĐÁP ÁN"
        // Ví dụ: "Trắc nghiệm", "Ngữ pháp", "Từ vựng", "Kanji"
      
        if (q.getAnswers() != null && !q.getAnswers().isEmpty() &&
            ("Trắc nghiệm".equals(q.getQuestionType()) ||
             "Ngữ pháp".equals(q.getQuestionType()) ||
             "Từ vựng".equals(q.getQuestionType()) ||
             "Đọc hiểu".equals(q.getQuestionType()) ||
             "Kanji".equals(q.getQuestionType()) )) {
            char optionChar = 'A';
            for (Answer ans : q.getAnswers()) {
                if (ans.isCorrect()) {
                    return optionChar + ". " + (ans.getAnswerText() != null ? ans.getAnswerText() : "");
                }
                optionChar++;
            }
            return "[Không có đáp án đúng được đánh dấu cho câu hỏi trắc nghiệm/lựa chọn]";
        } else if (q.getAiSuggestedAnswer() != null && !q.getAiSuggestedAnswer().isEmpty()) {
            return q.getAiSuggestedAnswer();
        }
        return "[Không có đáp án hoặc loại câu hỏi không hỗ trợ đáp án rõ ràng]";
    }
    

    
    public static void generateAndExportAnswerKey(Test test, File file, String format) throws IOException {

        System.out.println("Đang tạo đáp án cho đề: " + test.getTestName() + " và xuất ra " + format);
        StringBuilder answerKeyContent = new StringBuilder("ĐÁP ÁN - ĐỀ THI: " + test.getTestName().toUpperCase() + "\n");
        if (test.getDescription() != null && !test.getDescription().isEmpty()) {
            answerKeyContent.append("Mô tả: ").append(test.getDescription()).append("\n");
        }
        answerKeyContent.append("Ngày tạo đề: ").append(test.getCreatedAt() != null ? new SimpleDateFormat("dd/MM/yyyy").format(test.getCreatedAt()) : "N/A").append("\n");
        answerKeyContent.append("--------------------------------------------------\n\n");

        int qNum = 1;
        if (test.getQuestionsInTest() != null) {
            for (Question q : test.getQuestionsInTest()) {
                answerKeyContent.append(qNum++).append(". ");
                if ("Trắc nghiệm".equals(q.getQuestionType()) && q.getAnswers() != null) {
                    boolean foundCorrect = false;
                    char optionChar = 'A';
                    for(Answer ans : q.getAnswers()){
                        if(ans.isCorrect()){
                            answerKeyContent.append(optionChar).append(". ").append(ans.getAnswerText());
                            foundCorrect = true;
                            break; 
                        }
                        optionChar++;
                    }
                    if(!foundCorrect)  answerKeyContent.append("[Không có đáp án đúng được đánh dấu]");
                } else if (q.getAiSuggestedAnswer() != null && !q.getAiSuggestedAnswer().isEmpty()) { 
                    answerKeyContent.append(q.getAiSuggestedAnswer());
                } else {
                    answerKeyContent.append("[Chưa có đáp án]");
                }
                answerKeyContent.append("\n");
            }
        } else {
            answerKeyContent.append("Đề thi này không có câu hỏi nào.\n");
        }

        // Hiện tại chỉ hỗ trợ xuất ra file text cho đáp án
        if ("txt".equalsIgnoreCase(format)) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(answerKeyContent.toString().getBytes("UTF-8"));
            }
            System.out.println("Tạo và xuất đáp án (TXT) thành công.");
        } else {
            System.err.println("Định dạng file đáp án '" + format + "' chưa được hỗ trợ trong bản mô phỏng này.");
            throw new IOException("Định dạng file đáp án '" + format + "' chưa được hỗ trợ.");
        }
    }
}
