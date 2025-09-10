package com.simonking.boot.mcp.client.controller;


import com.simonking.boot.mcp.client.dto.ActorsFilms;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring AI
 *
 * @Author: ws
 * @Date: 2025/2/24 15:10
 */
@RestController
public class FooController {

    private final ChatClient chatClient ;

    public FooController(ChatClient.Builder aiClientBuilder) {
        Map<String, String> commonHeaders = new HashMap<>();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .httpHeaders(commonHeaders)
                .build();

        this.chatClient = aiClientBuilder
                .defaultOptions(options)
                .build();
    }

    /**
     * 格式化输出示例
     * @return
     */
    @GetMapping("/ai/actor")
    public ActorsFilms generate(@RequestParam(value = "message", defaultValue = "谢霆锋") String message) {
        return chatClient.prompt()
                .user(u -> u.text("为 {actor} 生成 5 部电影的电影作品。")
                        .param("actor", message))
                .call()
                .entity(ActorsFilms.class);
    }
}
