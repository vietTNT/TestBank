package util; // Hoặc package bạn muốn đặt lớp này, ví dụ: AiService

import model.Question; // Model Question của dự án bạn
import model.Answer;   // Model Answer của dự án bạn

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiImageParserUtil {

   
    private static final String API_KEY = "AIzaSyDDX3gU6JmBHZNg3RbR3JyEM-IcQXsjiko"; // <<--- THAY THẾ BẰNG API KEY CỦA BẠN

   
    public static String scanImageAndGetStructuredText(String imagePath) {
        if (API_KEY.equals("YOUR_GEMINI_API_KEY_HERE") || API_KEY.trim().isEmpty()) {
            System.err.println("[AI OCR ERROR] API Key chưa được cấu hình trong AiImageParserUtil.java");
            return "Lỗi AI OCR: API Key chưa được cấu hình.";
        }
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "Lỗi AI OCR: Đường dẫn ảnh không được để trống.";
        }
        File imageFile = new File(imagePath);
        if (!imageFile.exists() || !imageFile.isFile()) {
            return "Lỗi AI OCR: File ảnh không tồn tại hoặc không phải là file: " + imagePath;
        }

        try {
            // Sử dụng model có khả năng xử lý multimodal (ảnh + text)
        
            String modelName = "gemini-2.5-flash-preview-05-20"; // Cập nhật model nếu cần
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + API_KEY;

            URL url = new URI(apiUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setDoOutput(true);

            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            String mimeType = "image/png"; // Mặc định
            String lowerImagePath = imagePath.toLowerCase();
            if (lowerImagePath.endsWith(".jpg") || lowerImagePath.endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else if (lowerImagePath.endsWith(".webp")) {
                mimeType = "image/webp";
            } // Thêm các mime type khác nếu cần

            String promptText = "Đọc kỹ các câu hỏi và các lựa chọn từ hình ảnh được cung cấp. " +
            		           "Trích xuất tất cả văn bản bạn thấy trong hình ảnh này một cách chính xác nhất có thể. Cố gắng giữ nguyên các dấu xuống dòng và định dạng cơ bản nếu có thể. Chỉ trả về nội dung văn bản đã trích xuất, không thêm bất kỳ lời giải thích hay bình luận nào khác."+
                             //   "Chỉ trích xuất văn bản của các câu hỏi và các lựa chọn mà bạn tìm thấy. " +
                                "KHÔNG thêm bất kỳ giải thích, mô tả, hay bình luận nào khác ngoài định dạng yêu cầu. " +
                                "Định dạng đầu ra cho MỖI câu hỏi phải như sau (lặp lại nếu có nhiều câu hỏi, mỗi câu hỏi kết thúc bằng ---END_QUESTION---):\n" +
                                "Section: [Phân loại câu hỏi, ví dụ: Ngữ pháp, Từ vựng, Đọc hiểu, Nghe hiểu, Kanji, Bảng chữ cái]\n" +
                                "Passage: [Nội dung đoạn văn nếu đây là câu hỏi Đọc hiểu và có đoạn văn. Nếu không có, bỏ qua hoàn toàn dòng này.]\n" +
                                "Question: [Nội dung đầy đủ của câu hỏi]\n" +
                                "A. [Nội dung lựa chọn A]\n" +
                                "B. [Nội dung lựa chọn B]\n" +
                                "C. [Nội dung lựa chọn C]\n" +
                                "D. [Nội dung lựa chọn D (Nếu chỉ có 3 lựa chọn, bỏ qua dòng này)]\n" +
                                // "E. [Nội dung lựa chọn E (Nếu có)]\n" + // Thêm nếu có thể có nhiều hơn 4 lựa chọn
                                "Answer: [Chỉ ghi một chữ cái ĐÁP ÁN ĐÚNG, ví dụ: A hoặc B hoặc C hoặc D. Nếu không thể xác định, ghi X]\n" +
                                "---END_QUESTION---";

            // Tạo JSON request body. Cần escape các ký tự đặc biệt trong promptText.
            String escapedPromptText = promptText.replace("\\", "\\\\")
                                                 .replace("\"", "\\\"")
                                                 .replace("\n", "\\n")
                                                 .replace("\r", "\\r");

            String requestBody = String.format("{\n" +
                    "  \"contents\": [\n" +
                    "    {\n" +
                    "      \"parts\": [\n" +
                    "        {\"text\": \"%s\"},\n" +
                    "        {\n" +
                    "          \"inline_data\": {\n" +
                    "            \"mime_type\": \"%s\",\n" +
                    "            \"data\": \"%s\"\n" +
                    "          }\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}", escapedPromptText, mimeType, base64Image);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            System.out.println("[AI LOG] Response Code: " + responseCode);

            StringBuilder responseContent = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader( (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseContent.append(responseLine.trim());
                }
            }

            System.out.println("[AI LOG] Raw AI Response (first 500 chars): " + responseContent.toString().substring(0, Math.min(responseContent.length(), 500)) + "...");

            if (responseCode >= 200 && responseCode < 300) {
                Pattern pattern = Pattern.compile("\"text\":\\s*\"(.*?)\"", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(responseContent.toString());
                StringBuilder extractedTextBuilder = new StringBuilder();
                while (matcher.find()) {
                    extractedTextBuilder.append(matcher.group(1));
                }

                if (extractedTextBuilder.length() == 0 && responseContent.toString().contains("error")) {
                     System.err.println("[AI ERROR] API trả về lỗi trong JSON: " + responseContent.toString());
                     return "Lỗi AI OCR: API trả về lỗi - " + responseContent.toString().substring(0, Math.min(responseContent.length(), 200));
                }

                String actualText = extractedTextBuilder.toString()
                                        .replace("\\n", "\n")
                                        .replace("\\r", "")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                        .replace("\\u003c", "<")
                                        .replace("\\u003e", ">")
                                        .replace("\\u2019", "'")
                                        .replace("\\u0026", "&");

                System.out.println("[AI LOG] Extracted Text from AI (first 500 chars): " + actualText.substring(0, Math.min(actualText.length(), 500)) + "...");
                if (actualText.trim().isEmpty() && responseCode == 200) {
                    System.out.println("[AI WARNING] AI không trả về nội dung text nào dù response code là 200.");
                    return "AI không tìm thấy văn bản nào trong ảnh.";
                }
                return actualText;
            } else {
                System.err.println("[AI ERROR] Lỗi từ API (Code: " + responseCode + "): " + responseContent.toString());
                return "Lỗi AI OCR: " + responseCode + " - " + responseContent.toString().substring(0, Math.min(responseContent.length(), 200));
            }

        } catch (IOException e) {
            System.err.println("[AI ERROR] Lỗi I/O khi gửi yêu cầu đến AI: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi AI OCR: Vấn đề kết nối hoặc đọc file.";
        } catch (Exception e) {
            System.err.println("[AI ERROR] Lỗi không xác định: " + e.getMessage());
            e.printStackTrace();
            return "Lỗi AI OCR không xác định: " + e.getMessage();
        }
    }

    public static List<Question> parseStructuredTextToQuestions(String structuredText) {
        List<Question> questions = new ArrayList<>();
        if (structuredText == null || structuredText.trim().isEmpty() || structuredText.startsWith("Lỗi AI OCR:") || structuredText.startsWith("AI không tìm thấy văn bản")) {
            System.out.println("[AI PARSER LOG] Đầu vào không hợp lệ hoặc chứa lỗi, không phân tích: " + structuredText);
            return questions;
        }
        System.out.println("[AI PARSER LOG] Bắt đầu phân tích văn bản từ AI (độ dài: " + structuredText.length() + "): \n" + structuredText.substring(0, Math.min(structuredText.length(), 300)) + "...");
        String[] questionBlocks = structuredText.split("---END_QUESTION---");

        for (String block : questionBlocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            System.out.println("\n[AI PARSER LOG] Đang xử lý khối:\n" + block);

            Question q = new Question();
            List<Answer> answers = new ArrayList<>();       
            char correctAnswerChar = 'X';
            StringBuilder currentPassage = new StringBuilder();
            StringBuilder currentQuestionContentBuilder = new StringBuilder();
            boolean readingQuestionText = false;
            String currentSection = "Chưa xác định"; // Mặc định

            boolean inPassageText = false;
            String[] lines = block.split("\\n");
            Pattern optionPattern = Pattern.compile("^[A-Za-z]\\.\\s*(.*)");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("Section:")) {
                    currentSection = line.substring("Section:".length()).trim();
                    q.setQuestionType(currentSection);
                    readingQuestionText = false;
                    inPassageText = false;
                } else if (line.startsWith("Passage:")) {
                	
                	currentPassage.setLength(0); // Xóa nội dung cũ
                	currentPassage.append(line.substring("Passage:".length()).trim());

                    inPassageText = true;
                    readingQuestionText = false;
                } else if (line.startsWith("Question:")) {
                	currentQuestionContentBuilder.setLength(0); 
                	currentQuestionContentBuilder.append(line.substring("Question:".length()).trim());
                    inPassageText = false;
                    readingQuestionText = true;
                } else if (line.matches("^[A-Za-z]\\.\\s*.*")) {
                	 inPassageText = false;
                	readingQuestionText = false;
                    Answer ans = new Answer();
                    String optionText = line.substring(line.indexOf(".") + 1).trim();
                    ans.setAnswerText(optionText);
                    ans.setCorrect(false);
                    answers.add(ans);
                } else if (line.startsWith("Answer:")) {
                    readingQuestionText = false;
                    inPassageText = false;
                    String ansCharStr = line.substring("Answer:".length()).trim().toUpperCase();
                    if (!ansCharStr.isEmpty()) {
                        correctAnswerChar = ansCharStr.charAt(0);
                    }
                } else {
                    Matcher optionMatcher = optionPattern.matcher(line);
                    if (optionMatcher.matches()) { // Nếu dòng này là một lựa chọn
                    	readingQuestionText = false;
                        inPassageText = false;
                        Answer ans = new Answer();
                        ans.setAnswerText(optionMatcher.group(1).trim());
                        ans.setCorrect(false);
                        answers.add(ans);
                    } else if (inPassageText) {
                        // Nếu đang trong trạng thái đọc Passage
                    	currentPassage.append("\n").append(line); // Nối các dòng của Passage
                    } else if (readingQuestionText) {
                    currentQuestionContentBuilder.append(" ").append(line);
                }
                }
            }
            
            String finalPassage = currentPassage.toString().trim();
            String finalQuestionText = currentQuestionContentBuilder.toString().trim();
            if (!finalPassage.isEmpty()) {
                // Bạn có thể tùy chỉnh cách hiển thị, ví dụ:
            	if(currentSection.equals("Đọc hiểu"))
                q.setQuestionText("Đoạn văn:\n" + finalPassage + "\n\nCâu hỏi:\n" + finalQuestionText);
                // Hoặc nếu bạn muốn nó liền mạch hơn cho các loại câu hỏi khác không phải "Đọc hiểu":
                 q.setQuestionText(finalPassage + "\n" + finalQuestionText);
            } else {
                q.setQuestionText(finalQuestionText);
            }
            
            if (currentPassage != null && !currentPassage.isEmpty() && "Đọc hiểu".equalsIgnoreCase(currentSection)) {
                q.setQuestionText("Đoạn văn:\n" + currentPassage + "\n\nCâu hỏi:\n" + finalQuestionText);
            } else {
                q.setQuestionText(finalQuestionText);
            }

            if (correctAnswerChar != 'X' && !answers.isEmpty()) {
                int correctIndex = correctAnswerChar - 'A';
                if (correctIndex >= 0 && correctIndex < answers.size()) {
                    answers.get(correctIndex).setCorrect(true);
                } else {
                    System.err.println("[AI PARSER WARNING] Ký tự đáp án '" + correctAnswerChar + "' không hợp lệ cho số lượng lựa chọn: " + answers.size() + " cho câu hỏi: " + q.getQuestionText().substring(0, Math.min(q.getQuestionText().length(),30)));
                }
            }
            q.setAnswers(answers);

            if (q.getQuestionType() == null || q.getQuestionType().isEmpty()) {
                q.setQuestionType(currentSection);
            }
            q.setDifficultyLevel("Khác");

            if (q.getQuestionText() != null && !q.getQuestionText().trim().isEmpty()) {
                questions.add(q);
                System.out.println("[AI PARSER LOG] Đã tạo câu hỏi: " + q.getQuestionText().substring(0, Math.min(q.getQuestionText().length(), 50)) + "...");
            } else {
                 System.out.println("[AI PARSER WARNING] Bỏ qua khối vì không có nội dung câu hỏi: " + block.substring(0, Math.min(block.length(), 50)));
            }
        }
        System.out.println("[AI PARSER LOG] Tổng cộng đã phân tích được " + questions.size() + " câu hỏi.");
        return questions;
    }
}
