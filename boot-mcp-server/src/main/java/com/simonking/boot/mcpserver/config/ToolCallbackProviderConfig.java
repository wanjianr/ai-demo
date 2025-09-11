package com.simonking.boot.mcpserver.config;

import com.simonking.boot.mcpserver.service.SqlQueryServiceHif0911;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO
 *
 * @Author: ws
 * @Date: 2025/4/27 15:41
 */
@Configuration
public class ToolCallbackProviderConfig {

    @Bean
    public ToolCallbackProvider gzhRecommendTools(SqlQueryServiceHif0911 gzhService) {
        return MethodToolCallbackProvider.builder().toolObjects(gzhService).build();
    }
}
