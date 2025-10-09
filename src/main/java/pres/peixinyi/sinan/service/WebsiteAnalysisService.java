package pres.peixinyi.sinan.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pres.peixinyi.sinan.dto.response.WebsiteAnalysisResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 网站分析服务
 * 使用 Spring AI 调用 OpenAI API 分析网站内容并自动分类打标签
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebsiteAnalysisService {

    private final ChatClient.Builder chatClientBuilder;

    /**
     * 流式分析网站并通过SSE返回进度信息
     *
     * @param url 网站URL
     * @param existingSpaces 现有的分类列表
     * @param existingTags 现有的标签列表
     * @param emitter SSE发射器
     */
    public void analyzeWebsiteStreaming(String url, List<String> existingSpaces, List<String> existingTags, SseEmitter emitter) {
        try {
            // 1. 发送开始抓取的状态
            sendEvent(emitter, "status", "正在抓取网站信息...", null);

            // 抓取网站信息
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            String title = doc.title();
            String description = doc.select("meta[name=description]").attr("content");
            if (description.isEmpty()) {
                description = doc.select("meta[property=og:description]").attr("content");
            }

            // 2. 发送网站基本信息
            Map<String, String> basicInfo = new HashMap<>();
            basicInfo.put("url", url);
            basicInfo.put("name", title);
            basicInfo.put("description", description);
            sendEvent(emitter, "basic_info", "网站信息抓取完成", basicInfo);

            // 3. 发送AI分析状态
            sendEvent(emitter, "status", "正在使用AI分析网站...", null);

            // 构建提示词
            String prompt = buildPrompt(url, title, description, existingSpaces, existingTags);

            // 4. 调用 AI 进行分析
            String aiResponse = callAI(prompt);

            // 5. 解析 AI 返回的 JSON
            WebsiteAnalysisResponse result = parseAIResponse(aiResponse, url, title, description);

            // 6. 发送最终结果
            sendEvent(emitter, "result", "分析完成", result);

            // 7. 完成
            emitter.complete();

        } catch (IOException e) {
            log.error("抓取网站信息失败: {}", url, e);
            sendEvent(emitter, "error", "无法访问该网站: " + e.getMessage(), null);
            emitter.completeWithError(e);
        } catch (Exception e) {
            log.error("分析网站失败: {}", url, e);
            sendEvent(emitter, "error", "分析失败: " + e.getMessage(), null);
            emitter.completeWithError(e);
        }
    }

    /**
     * 发送SSE事件
     */
    private void sendEvent(SseEmitter emitter, String eventType, String message, Object data) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("type", eventType);
            eventData.put("message", message);
            if (data != null) {
                eventData.put("data", data);
            }
            eventData.put("timestamp", System.currentTimeMillis());

            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(eventData));

            log.info("SSE事件发送成功: type={}, message={}", eventType, message);
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 分析网站并返回分类和标签建议(同步版本,保留用于兼容)
     *
     * @param url 网站URL
     * @param existingSpaces 现有的分类列表
     * @param existingTags 现有的标签列表
     * @return 网站分析结果
     */
    public WebsiteAnalysisResponse analyzeWebsite(String url, List<String> existingSpaces, List<String> existingTags) {
        try {
            // 1. 抓取网站信息
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();

            String title = doc.title();
            String description = doc.select("meta[name=description]").attr("content");
            if (description.isEmpty()) {
                description = doc.select("meta[property=og:description]").attr("content");
            }

            // 2. 构建提示词
            String prompt = buildPrompt(url, title, description, existingSpaces, existingTags);

            // 3. 调用 AI 进行分析
            String aiResponse = callAI(prompt);

            // 4. 解析 AI 返回的 JSON
            return parseAIResponse(aiResponse, url, title, description);

        } catch (IOException e) {
            log.error("抓取网站信息失败: {}", url, e);
            throw new RuntimeException("无法访问该网站: " + e.getMessage());
        } catch (Exception e) {
            log.error("分析网站失败: {}", url, e);
            throw new RuntimeException("分析失败: " + e.getMessage());
        }
    }

    /**
     * 构建发送给 AI 的提示词
     */
    private String buildPrompt(String url, String title, String description,
                               List<String> existingSpaces, List<String> existingTags) {
        StringBuilder promptBuilder = new StringBuilder();

        promptBuilder.append("你的任务是读取给定网站的信息，获取网站名称和描述内容，");
        promptBuilder.append("然后判断该网站属于什么分类，并为其匹配合适的标签。分类是必须选择的。");
        promptBuilder.append("如果已有的分类和标签都不匹配，则建议创建新的分类和标签，");
        promptBuilder.append("新的标签需要根据标签名称给定适合的颜色，颜色的返回格式必须要是16进制的，");
        promptBuilder.append("必须保证返回的标签包含颜色，如果是已经存在的标签则不需要填写颜色。最终输出采用JSON格式。\n\n");

        promptBuilder.append("首先，请仔细阅读以下网站的网址：\n");
        promptBuilder.append("<url>\n").append(url).append("\n</url>\n\n");

        promptBuilder.append("以下是已有的分类：\n");
        promptBuilder.append("<spaces>\n").append(String.join(", ", existingSpaces)).append("\n</spaces>\n\n");

        promptBuilder.append("以下是已有的标签：\n");
        promptBuilder.append("<tags>\n").append(String.join(", ", existingTags)).append("\n</tags>\n\n");

        promptBuilder.append("在判断时，请遵循以下规则：\n");
        promptBuilder.append("1. 分类只能选择一个，且只能从<spaces>中选择，如果已有的分类都不匹配，则建议创建新的分类。\n");
        promptBuilder.append("2. 标签可以有多个，且只能从<tags>中选择，如果已有的标签都不匹配，则建议创建新的标签。\n");
        promptBuilder.append("3. 如果是新的分类则在分类名称后添加new标记，使用:分割。\n");
        promptBuilder.append("4. 如果是新的Tag则在标签后添加上new标记，使用:分割。\n\n");

        promptBuilder.append("请在<output>标签中给出最终的JSON格式输出，格式如下：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("    \"url\": \"url\",\n");
        promptBuilder.append("    \"name\":\"网站名称\",\n");
        promptBuilder.append("    \"description\":\"描述内容\",\n");
        promptBuilder.append("    \"spaces\": \"网站1:new\",\n");
        promptBuilder.append("    \"tags\": [\n");
        promptBuilder.append("        \"Tag1\",\"Tag2:new:#000000\"\n");
        promptBuilder.append("    ]\n");
        promptBuilder.append("}\n\n");
        promptBuilder.append("<output>\n");
        promptBuilder.append("[在此给出最终的JSON格式输出]\n");
        promptBuilder.append("</output>");

        return promptBuilder.toString();
    }

    /**
     * 调用 AI 进行分析
     */
    private String callAI(String prompt) {
        ChatClient chatClient = chatClientBuilder.build();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是一个专业的网站分类和标签推荐助手。请根据用户提供的网站信息，准确分析网站类型并推荐合适的分类和标签。"));
        messages.add(new UserMessage(prompt));

        String response = chatClient.prompt(new Prompt(messages))
                .call()
                .content();

        log.info("AI响应: {}", response);
        return response;
    }

    /**
     * 解析 AI 返回的响应，提取 JSON 内容
     */
    private WebsiteAnalysisResponse parseAIResponse(String aiResponse, String url, String title, String description) {
        try {
            // 从 <output> 标签中提取 JSON
            Pattern pattern = Pattern.compile("<output>\\s*([\\s\\S]*?)\\s*</output>");
            Matcher matcher = pattern.matcher(aiResponse);

            String jsonContent;
            if (matcher.find()) {
                jsonContent = matcher.group(1).trim();
            } else {
                // 如果没有找到 output 标签，尝试直接解析整个响应
                jsonContent = aiResponse.trim();
            }

            // 移除可能的 markdown 代码块标记
            jsonContent = jsonContent.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();

            log.info("提取的JSON: {}", jsonContent);

            // 解析 JSON
            WebsiteAnalysisResponse response = JSON.parseObject(jsonContent, WebsiteAnalysisResponse.class);

            // 如果 AI 没有返回某些字段，使用抓取的原始数据
            if (response.getUrl() == null || response.getUrl().isEmpty()) {
                response.setUrl(url);
            }
            if (response.getName() == null || response.getName().isEmpty()) {
                response.setName(title);
            }
            if (response.getDescription() == null || response.getDescription().isEmpty()) {
                response.setDescription(description);
            }

            return response;
        } catch (Exception e) {
            log.error("解析AI响应失败，原始响应: {}", aiResponse, e);

            // 返回一个基础响应
            WebsiteAnalysisResponse fallbackResponse = new WebsiteAnalysisResponse();
            fallbackResponse.setUrl(url);
            fallbackResponse.setName(title);
            fallbackResponse.setDescription(description);
            fallbackResponse.setSpaces("未分类");
            fallbackResponse.setTags(new ArrayList<>());

            return fallbackResponse;
        }
    }
}
