package com.simonking.boot.mcp.client.controller;

import com.simonking.boot.mcp.client.dto.AntdTableResponseDTO;
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
@RequestMapping("/hif")
public class HifController {

    private final ChatClient chatClient;

    public HifController(ChatClient.Builder aiClientBuilder, ToolCallbackProvider mcpTools) {
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


    /**
     * POST方式的SQL查询接口 - 支持分页和缓存
     */
    @PostMapping("/sql")
    public ResponseEntity<AntdTableResponseDTO> executeSqlQuery(@RequestBody QueryPageRequestDTO request) {
        String queryDescription = request.getQuery();
        System.err.println("SQL查询请求: " + queryDescription);
        System.err.println("分页参数: page=" + request.getPage() + ", size=" + request.getSize());

        try {
            // 使用Spring AI的类型转换器直接转换为AntdTableResponseDTO
            AntdTableResponseDTO response = this.chatClient
                    .prompt(buildSqlQueryPrompt(request))
                    .call()
                    .entity(AntdTableResponseDTO.class);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("查询执行异常: " + e.getMessage());
            e.printStackTrace();

            // 返回错误响应
            AntdTableResponseDTO errorResponse = AntdTableResponseDTO.builder()
                    .columns(java.util.Collections.emptyList())
                    .dataSource(java.util.Collections.emptyList())
                    .topText("查询执行失败")
                    .bottomText("错误信息: " + e.getMessage())
                    .pagination(AntdTableResponseDTO.Pagination.builder()
                            .total(0)
                            .current(request.getPage())
                            .pageSize(request.getSize())
                            .build())
                    .build();
            return ResponseEntity.ok(errorResponse);
        }
    }


    /**
     * 获取缓存SQL的接口
     */
    @GetMapping("/sql/cache")
    public ResponseEntity<String> getCachedSql(@RequestParam(name = "query") String queryDescription) {
        System.err.println("获取缓存SQL请求: " + queryDescription);

        String cachePrompt = String.format("""
            用户想要获取之前缓存的SQL语句：%s
            
            请使用getCachedSql工具查找相关的缓存SQL。
            """, queryDescription);

        String response = this.chatClient
                .prompt(cachePrompt)
                .call().content();
        return ResponseEntity.ok(response);
    }

    /**
     * 清空SQL缓存的接口
     */
    @PostMapping("/sql/cache/clear")
    public ResponseEntity<String> clearSqlCache() {
        System.err.println("清空SQL缓存请求");

        String response = this.chatClient
                .prompt("请使用clearSqlCache工具清空所有缓存的SQL语句。")
                .call().content();
        return ResponseEntity.ok(response);
    }

    /**
     * 查看所有缓存SQL的接口
     */
    @GetMapping("/sql/cache/list")
    public ResponseEntity<String> listCachedSqls() {
        System.err.println("查看缓存SQL列表请求");

        String response = this.chatClient
                .prompt("请使用listCachedSqls工具显示当前所有缓存的SQL语句。")
                .call().content();
        return ResponseEntity.ok(response);
    }

    private String getSystemPrompt() {
        try {
            ClassPathResource resource = new ClassPathResource("prompt/system-prompt.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private String buildSqlQueryPrompt(QueryPageRequestDTO request) {
        String queryPrompt;
        try {
            ClassPathResource resource = new ClassPathResource("prompt/user-prompt.txt");
            queryPrompt = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            queryPrompt = "";
        }
        return String.format(queryPrompt, request.getQuery(),
                request.getPage(), request.getSize(), request.getPage(), request.getSize(), request.getQuery());
    }

}
