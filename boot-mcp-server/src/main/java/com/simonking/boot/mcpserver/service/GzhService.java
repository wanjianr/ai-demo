package com.simonking.boot.mcpserver.service;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 接口
 *
 * @Author: ws
 * @Date: 2025/4/27 19:48
 */
public interface GzhService {

    /**
     * 生成词汇信息
     */
    String generateWords();
}
