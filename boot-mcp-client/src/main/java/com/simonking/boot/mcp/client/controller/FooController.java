//package com.simonking.boot.mcp.client;
//
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
//import org.springframework.ai.ollama.OllamaChatModel;
//import org.springframework.ai.tool.ToolCallbackProvider;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * Spring AI
// *
// * @Author: ws
// * @Date: 2025/2/24 15:10
// */
//@RestController
//public class FooController {
//
//    @Autowired
//    private OllamaChatModel ollamaChatModel;
//    @Autowired
//    private ToolCallbackProvider toolCallbackProvider;
//
//    /**
//     * @Description: 普通的调用
//     *
//     * @Author: ws
//     * @Date: 2025/2/24 16:32
//     **/
//    @GetMapping("/ai/generate")
//    public String generate(@RequestParam(value = "message", defaultValue = "推荐一个公众号") String message) {
//        ChatClient chatClient = ChatClient.builder(ollamaChatModel)
//                .defaultTools(toolCallbackProvider.getToolCallbacks())
//                .build();
//        CallResponseSpec call = chatClient.prompt(message).call();
//        String content = call.content();
//        return content;
//    }
//}
