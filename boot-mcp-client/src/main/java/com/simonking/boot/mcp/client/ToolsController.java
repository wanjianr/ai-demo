package com.simonking.boot.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/tools")
public class ToolsController {

    private final ChatClient chatClient ;

    public ToolsController(ChatClient.Builder aiClientBuilder, ToolCallbackProvider mcpTools) {
        Map commonHeaders = new HashMap();
        OpenAiChatOptions options = OpenAiChatOptions.builder().httpHeaders(commonHeaders).build();

        this.chatClient = aiClientBuilder
                .defaultTools(mcpTools)
                .defaultOptions(options)
                .build() ;
    }

    @GetMapping("/word")
    public ResponseEntity<String> getAirQuality(@RequestParam(name = "prompt") String prompt) {
        System.err.println(prompt) ;
        String response = this.chatClient
                .prompt(prompt)
                .call().content() ;
        return ResponseEntity.ok(response) ;
    }
}
