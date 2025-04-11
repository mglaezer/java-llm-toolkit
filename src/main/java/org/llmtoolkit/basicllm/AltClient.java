package org.llmtoolkit.basicllm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Alternative to Langchain4j client that properly handles timeouts for OpenAI-compatible providers.
 * While Langchain4j's {@link OpenAiChatModelProvider} supports timeouts,
 * it only works with OpenAI itself. Other providers using the OpenAI-compatible API (Inference.net, DeepSeek, etc.)
 * need this alternative implementation if the requests exceed 60 sec.
 */
@SuppressWarnings("unused")
public class AltClient {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient;

    private AltClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public static AltClient createClient(String baseUrl, String apiKey) {
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new AltClient(restClient);
    }

    public String answer(
            String prompt,
            String model,
            Double temperature,
            Double topP,
            Integer maxTokens,
            Integer thinkingTokens,
            Integer timeout) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        if (timeout != null) requestBody.put("timeout", timeout);
        if (temperature != null) requestBody.put("temperature", temperature);
        if (topP != null) requestBody.put("top_p", topP);
        if (thinkingTokens != null) requestBody.put("max_completion_tokens", thinkingTokens);
        if (maxTokens != null) requestBody.put("max_tokens", maxTokens);
        requestBody.put("stream", false);
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        String jsonResponse = restClient
                .post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    String errorBody = new String(response.getBody().readAllBytes());
                    throw new RuntimeException("API error code: " + response.getStatusCode() + "\nBody: " + errorBody);
                })
                .body(String.class);

        return extractAnswer(jsonResponse);
    }

    private static String extractAnswer(String jsonResponse) {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(jsonResponse);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error parsing response body: " + e.getMessage(), e);
        }

        if (rootNode.has("choices")
                && rootNode.get("choices").isArray()
                && !rootNode.get("choices").isEmpty()
                && rootNode.get("choices").get(0).has("message")
                && rootNode.get("choices").get(0).get("message").has("content")) {

            return rootNode.get("choices").get(0).get("message").get("content").asText();
        } else {
            throw new RuntimeException("Unexpected response format: " + jsonResponse);
        }
    }
}
