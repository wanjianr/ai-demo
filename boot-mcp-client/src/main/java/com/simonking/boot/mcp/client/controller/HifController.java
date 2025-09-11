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

        String response = this.chatClient
                .prompt(buildSqlQueryPrompt(request))
                .call().content();
        // todo 将response结构化为AntdTableResponseDTO
        AntdTableResponseDTO tableResponse = AntdTableResponseDTO.builder()
                .build();
        return ResponseEntity.ok(tableResponse);
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
        // 构建增强的提示词
//        StringBuilder promptBuilder = new StringBuilder();
//        promptBuilder.append(queryPrompt).append("\n\n");
//
//        // 基础查询信息
//        promptBuilder.append(String.format("用户查询需求：%s\n\n", request.getQuery()));
//
//        // 分页参数
//        promptBuilder.append(String.format("分页要求：第%d页，每页%d条记录\n\n",
//                request.getPage(), request.getSize()));
//
//        // 缓存相关处理
//        promptBuilder.append(String.format("缓存处理：请先使用getCachedSql工具查找缓存键为'%s'的SQL语句\n\n",
//                request.getQuery()));
//
//        promptBuilder.append(String.format("""
//            请按照以下步骤处理这个查询：
//
//            1. 【可选】如果需要使用缓存，先调用getCachedSql工具查找已缓存的SQL
//            2. 如果没有缓存或需要生成新SQL，则：
//               a) 使用getDatabaseTables工具获取所有相关的表名
//               b) 使用getDatabaseStructure工具获取表的结构、关联关系
//               c) 基于表结构生成合适的SQL查询语句
//            3. 使用executeQuery工具执行SQL，必须传入以下参数：
//               - sql: 生成的SQL语句
//               - page: %d (页码)
//               - pageSize: %d (每页大小)
//               - queryDescription: "%s" (查询描述，用于缓存)
//
//            请确保：
//            - SQL语句语法正确，使用反引号包围字段名和表名
//            - 添加适当的WHERE条件和ORDER BY排序
//            - 正确处理分页参数
//            - 缓存生成的SQL以便后续使用
//            - 结果展示包含分页信息和导航提示
//            """, request.getPage(), request.getSize(), request.getQuery()));
//
//        return promptBuilder.toString();
    }

}
