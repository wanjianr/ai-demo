package com.simonking.boot.mcp.client.controller;

import com.simonking.boot.mcp.client.dto.PageRequestDTO;
import com.simonking.boot.mcp.client.dto.QueryPageRequestDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    private final ChatClient chatClient ;

    public ToolsController(ChatClient.Builder aiClientBuilder, ToolCallbackProvider mcpTools) {
        Map<String, String> commonHeaders = new HashMap<>();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .httpHeaders(commonHeaders)
                .build();

        this.chatClient = aiClientBuilder
                .defaultTools(mcpTools)
                .defaultOptions(options)
                .defaultSystem(getSystemPrompt())
                .build();
    }

    @GetMapping("/word")
    public ResponseEntity<String> getWordGeneration(@RequestParam(name = "prompt") String prompt) {
        System.err.println("词汇生成请求: " + prompt);
        String response = this.chatClient
                .prompt(prompt)
                .call().content();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sql")
    public ResponseEntity<String> executeSqlQuery(@RequestBody QueryPageRequestDTO request) {
        String queryDescription = request.getQuery();
        System.err.println("SQL查询请求: " + queryDescription);

        String response = this.chatClient
                .prompt(buildSqlQueryPrompt(request))
                .call().content();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sql")
    public ResponseEntity<String> executeSqlQueryGet(@RequestParam(name = "query") String queryDescription) {
        System.err.println("SQL查询请求: " + queryDescription);
        QueryPageRequestDTO queryPageRequestDTO = new QueryPageRequestDTO();
        queryPageRequestDTO.setQuery(queryDescription);
        String response = this.chatClient
                .prompt(buildSqlQueryPrompt(queryPageRequestDTO))
                .call().content();
        return ResponseEntity.ok(response);
    }

    private String getSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompt/chat-client-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String buildSqlQueryPrompt(QueryPageRequestDTO queryDescription) {
        String quryPrompt;
        try {
            ClassPathResource resource = new ClassPathResource("prompt/query-model-prompt.txt");
            quryPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }

        return String.format(quryPrompt, queryDescription.getQuery(),
                queryDescription.getPage(), queryDescription.getSize());
    }
}
