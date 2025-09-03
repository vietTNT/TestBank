// File: AiService/AIAnswerSuggester.java
package AiService;

import java.util.List;

public class AIAnswerSuggester {

    public static String suggestAnswer(String questionContent, List<String> answers) {
    	 if (answers == null || answers.isEmpty()) {
    	        return "Lỗi: Vui lòng cung cấp ít nhất một lựa chọn đáp án.";
    	    }

    	    StringBuilder promptBuilder = new StringBuilder();
    	    promptBuilder.append("Bạn là một trợ lý chuyên gia tiếng Nhật. Nhiệm vụ của bạn là phân tích câu hỏi và các lựa chọn đáp án được cung cấp bằng tiếng Nhật (hoặc liên quan đến tiếng Nhật) dưới đây.\n");
    	    promptBuilder.append("Nhiệm vụ của bạn là phân tích kỹ lưỡng câu hỏi và các lựa chọn đáp án được cung cấp dưới đây.\n");
            promptBuilder.append("Sau đó, hãy xác định lựa chọn nào là đáp án chính xác nhất cho câu hỏi.\n\n");
    	    promptBuilder.append("Hãy xác định lựa chọn nào là đáp án đúng nhất.\n\n");
    	    promptBuilder.append("Yêu cầu định dạng đầu ra:\n");
    	    promptBuilder.append("1. Dòng đầu tiên: Chỉ ghi lại TOÀN BỘ NỘI DUNG TEXT của lựa chọn mà bạn cho là ĐÚNG NHẤT.\n");
    	    promptBuilder.append("   Ví dụ: Nếu lựa chọn đúng là 'こんにちは', dòng đầu tiên chỉ ghi 'こんにちは'.\n");
            promptBuilder.append("2. Dòng thứ hai trở đi: Đưa ra giải thích ngắn gọn, rõ ràng bằng TIẾNG VIỆT tại sao lựa chọn đó là đúng. Tập trung vào kiến thức ngữ pháp, từ vựng, ngữ cảnh hoặc logic liên quan.\n");
    	    promptBuilder.append("3. KHÔNG thêm bất kỳ thông tin nào khác ngoài hai dòng trên.\n\n");

    	    promptBuilder.append("Ví dụ định dạng mong muốn:\n");
    	    promptBuilder.append("ありがとう\n"); // Giả sử đây là nội dung lựa chọn đúng
    	    promptBuilder.append("Vì 'ありがとう' là cách nói 'cảm ơn' phổ biến và phù hợp trong hầu hết các tình huống giao tiếp thông thường.\n\n");

    	    promptBuilder.append("Dưới đây là câu hỏi và các lựa chọn:\n");
    	    promptBuilder.append("Câu hỏi:\n").append(questionContent).append("\n\n");
    	    promptBuilder.append("Các lựa chọn đáp án:\n");
    	    char optionChar = 'A';
    	    for (String answerText : answers) {
    	        promptBuilder.append(optionChar++).append(". ").append(answerText).append("\n");
    	    }

    	    String prompt = promptBuilder.toString();

    	    
    	     String aiResponse = GeminiConnect.generateSuggestAnswer(prompt); // Giả sử lớp này xử lý việc gọi API
    	     return aiResponse;

    	   
    	
    }
}