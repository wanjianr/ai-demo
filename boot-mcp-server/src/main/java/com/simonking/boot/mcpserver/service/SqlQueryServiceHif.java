package com.simonking.boot.mcpserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>PURPOSE:
 * <p>DESCRIPTION:
 * <p>CALLED BY: wanjian
 * <p>CREATE DATE: 2025/9/10
 * <p>UPDATE DATE: 2025/9/10
 * <p>UPDATE USER:
 * <p>HISTORY: 1.0
 *
 * @author wanjian
 * @version 1.0
 * @see
 * @since java 1.8
 */
@Service
@Slf4j
public class SqlQueryServiceHif {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行SQL查询并返回结果
     */
    @Tool(description = """
        执行SQL查询语句并返回格式化结果。参数：
        - sql: 要执行的SQL查询语句（仅支持SELECT语句）
        返回查询结果的格式化文本
        """)
    public String executeQuery(String sql) {
        try {
            // 安全检查
            if (!isValidSelectQuery(sql)) {
                return "安全限制：只允许执行SELECT查询语句，不支持INSERT、UPDATE、DELETE等操作";
            }


            // 清理和优化SQL
            String cleanSql = cleanSql(sql);
            String limitedSql = cleanSql;

            log.info("执行SQL: {}", limitedSql);

            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> result = jdbcTemplate.queryForList(limitedSql);
            long executionTime = System.currentTimeMillis() - startTime;

            return formatSuccessResult(result, limitedSql, executionTime);

        } catch (Exception e) {
            log.error("SQL执行失败: " + sql, e);
            return formatErrorResult(sql, e.getMessage());
        }
    }

    /**
     * 获取医保基金系统数据库结构信息（静态版本）
     */
    @Tool(description = """
        获取医保基金系统中预定义的数据库表名信息。
        返回当前系统支持查询的所有表名及说明说明
        """)
    public String getDatabaseTables() {
        try {
            ClassPathResource resource = new ClassPathResource("database-info.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("获取数据库表名失败", e);
            return "";
        }
    }

    /**
     * 获取医保基金系统数据库表信息
     */
    @Tool(description = """
        获取医保基金系统中预定义的数据库表信息。
        返回当前系统支持查询的所有表结构说明
        """)
    public String getDatabaseStructure() {
        try {
            ClassPathResource resource = new ClassPathResource("database-tablestructural.txt");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("获取数据库表结构失败", e);
            return "";
        }
    }



    // 私有辅助方法
    private boolean isValidSelectQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) return false;

        String upperSql = sql.trim().toUpperCase();

        // 只允许SELECT语句
        if (!upperSql.startsWith("SELECT")) return false;

        // 检查是否包含危险关键字
        String[] dangerousKeywords = {
                "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
                "TRUNCATE", "EXEC", "EXECUTE", "DECLARE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) return false;
        }

        return true;
    }

    private String cleanSql(String sql) {
        return sql.trim().replaceAll(";+$", ""); // 移除末尾的分号
    }

    private String addLimitIfNeeded(String sql, int maxRows) {
        String upperSql = sql.toUpperCase();
        if (!upperSql.contains("LIMIT")) {
            return sql + " LIMIT " + maxRows;
        }
        return sql;
    }

    private String formatSuccessResult(List<Map<String, Object>> data, String sql, long executionTime) {
        StringBuilder sb = new StringBuilder();

        sb.append("医保基金系统查询执行成功！\n\n");
        sb.append("执行的SQL：\n```sql\n").append(sql).append("\n```\n\n");
        sb.append("查询统计：\n");
        sb.append("- 返回记录数：").append(data.size()).append("条\n");
        sb.append("- 执行时间：").append(executionTime).append("ms\n\n");

        if (data.isEmpty()) {
            sb.append("📝 查询结果：无数据\n");
        } else {
            sb.append("📋 查询结果：\n");
            sb.append(formatAsTable(data));
        }

        return sb.toString();
    }

    private String formatErrorResult(String sql, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("医保基金系统查询执行失败！\n\n");
        sb.append("执行的SQL：\n```sql\n").append(sql).append("\n```\n\n");
        sb.append("错误信息：").append(error).append("\n\n");
        sb.append("建议检查：\n");
        sb.append("- 表名是否正确（hif_iption_type_stt_d、hif_cert_stt_d等）\n");
        sb.append("- 字段名是否准确（admdvs、biz_date、iption_cnt等）\n");
        sb.append("- 日期格式是否正确（YYYYMMDD或YYYYMM）\n");
        sb.append("- SQL语法是否有误\n");
        sb.append("- 是否有权限访问相关表\n");
        return sb.toString();
    }

    private String formatAsTable(List<Map<String, Object>> data) {
        if (data.isEmpty()) return "无数据";

        // 获取所有列名
        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> row : data) {
            allColumns.addAll(row.keySet());
        }

        List<String> columns = new ArrayList<>(allColumns);
        StringBuilder sb = new StringBuilder();

        // 计算列宽
        Map<String, Integer> columnWidths = new HashMap<>();
        for (String col : columns) {
            int maxWidth = col.length();
            for (Map<String, Object> row : data) {
                Object value = row.get(col);
                String strValue = value != null ? value.toString() : "NULL";
                maxWidth = Math.max(maxWidth, strValue.length());
            }
            columnWidths.put(col, Math.min(maxWidth, 25)); // 限制最大列宽
        }

        // 表头
        sb.append("```\n");
        for (String col : columns) {
            sb.append(String.format("%-" + columnWidths.get(col) + "s | ", col));
        }
        sb.append("\n");

        // 分隔线
        for (String col : columns) {
            sb.append(String.join("", Collections.nCopies(columnWidths.get(col), "-")));
            sb.append("-+-");
        }
        sb.append("\n");

        // 数据行
        for (Map<String, Object> row : data) {
            for (String col : columns) {
                Object value = row.get(col);
                String strValue = value != null ? value.toString() : "NULL";
                if (strValue.length() > 25) {
                    strValue = strValue.substring(0, 22) + "...";
                }
                sb.append(String.format("%-" + columnWidths.get(col) + "s | ", strValue));
            }
            sb.append("\n");
        }
        sb.append("```\n");

        return sb.toString();
    }

}
