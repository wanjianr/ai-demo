package com.simonking.boot.mcpserver.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @Author: ws
 * @Date: 2025/4/27 19:48
 */
@Service
public class GzhServiceImpl implements GzhService {

    @Tool(description = "词汇生成")
    @Override
    public String generateWords() {
        // 约定生成内容的格式
        return "请以以下格式提供：        {\n" +
                "            \"word\": \"\",\n" +
                "            \"partOfSpeech\": \"\",\n" +
                "            \"phonetic\": \"\",\n" +
                "            \"translation\": \"\",\n" +
                "            \"exampleSentence\": \"\",\n" +
                "            \"exampleTranslation\": \"\"\n" +
                "        }";
    }
}
