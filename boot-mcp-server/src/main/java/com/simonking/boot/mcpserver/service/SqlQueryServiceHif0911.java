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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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
public class SqlQueryServiceHif0911 {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // SQL缓存 - 使用ConcurrentHashMap保证线程安全
    // key: 查询描述的hash值, value: 包含SQL和相关信息的Map
    private final Map<String, Map<String, Object>> sqlCache = new ConcurrentHashMap<>();

    /**
     * 执行SQL查询并返回结果（支持分页）
     */
    @Tool(description = """
        执行SQL查询语句并返回格式化结果，支持分页功能。参数：
        - sql: 要执行的SQL查询语句（仅支持SELECT语句）
        - page: 页码（从1开始，默认为1）
        - pageSize: 每页大小（默认为10，最大100）
        - queryDescription: 查询描述（用于缓存，可选）
        返回查询结果的格式化文本和分页信息
        """)
    public String executeQuery(String sql, Integer page, Integer pageSize, String queryDescription) {
        try {
            // 安全检查
            if (!isValidSelectQuery(sql)) {
                return "安全限制：只允许执行SELECT查询语句，不支持INSERT、UPDATE、DELETE等操作";
            }

            // 参数处理
            if (page == null || page <= 0) page = 1;
            if (pageSize == null || pageSize <= 0) pageSize = 10;
            if (pageSize > 100) pageSize = 100;

            // 清理SQL
            String cleanSql = cleanSql(sql);

            // 缓存SQL（如果提供了查询描述）
            if (queryDescription != null && !queryDescription.trim().isEmpty()) {
                cacheSql(queryDescription, cleanSql);
            }

            long startTime = System.currentTimeMillis();

            // 执行分页查询
            Map<String, Object> paginationResult = executeWithPagination(cleanSql, page, pageSize);

            long executionTime = System.currentTimeMillis() - startTime;

            return formatSuccessResult(paginationResult, cleanSql, executionTime, page, pageSize);

        } catch (Exception e) {
            log.error("SQL执行失败: " + sql, e);
            return formatErrorResult(sql, e.getMessage());
        }
    }

    /**
     * 根据查询描述从缓存中获取SQL
     */
    @Tool(description = """
        根据查询描述从内存缓存中获取之前生成的SQL语句。参数：
        - queryDescription: 查询需求的自然语言描述
        返回缓存的SQL语句，如果不存在则提示需要AI生成
        """)
    public String getCachedSql(String queryDescription) {
        if (queryDescription == null || queryDescription.trim().isEmpty()) {
            return "查询描述不能为空";
        }

        String cacheKey = generateCacheKey(queryDescription);
        Map<String, Object> cachedData = sqlCache.get(cacheKey);

        if (cachedData != null) {
            String sql = (String) cachedData.get("sql");
            String originalDescription = (String) cachedData.get("description");
            java.util.Date cacheTime = (java.util.Date) cachedData.get("cacheTime");

            StringBuilder result = new StringBuilder();
            result.append("✅ 找到缓存的SQL语句:\n\n");
            result.append("查询描述: ").append(originalDescription).append("\n");
            result.append("缓存时间: ").append(cacheTime).append("\n\n");
            result.append("SQL语句:\n```sql\n").append(sql).append("\n```\n\n");
            result.append("💡 您可以直接使用executeQuery工具执行此SQL，或根据需要进行修改");

            return result.toString();
        } else {
            return "❌ 未找到相关的缓存SQL\n\n" +
                    "查询描述: " + queryDescription + "\n\n" +
                    "💡 建议:\n" +
                    "1. 请使用AI生成新的SQL语句\n" +
                    "2. 确保查询描述准确，以便后续缓存\n" +
                    "3. 可以先使用getDatabaseTables和getDatabaseStructure获取表结构信息";
        }
    }

    /**
     * 清空SQL缓存
     */
    @Tool(description = """
        清空内存中的SQL缓存。
        返回清空结果
        """)
    public String clearSqlCache() {
        int cacheSize = sqlCache.size();
        sqlCache.clear();
        return String.format("✅ SQL缓存已清空，共清除了 %d 条缓存记录", cacheSize);
    }

    /**
     * 查看所有缓存的SQL
     */
    @Tool(description = """
        查看当前内存中缓存的所有SQL语句摘要。
        返回缓存的SQL列表
        """)
    public String listCachedSqls() {
        if (sqlCache.isEmpty()) {
            return "📝 当前没有缓存的SQL语句";
        }

        StringBuilder result = new StringBuilder();
        result.append("📋 当前缓存的SQL语句列表:\n\n");

        int index = 1;
        for (Map.Entry<String, Map<String, Object>> entry : sqlCache.entrySet()) {
            Map<String, Object> cachedData = entry.getValue();
            String description = (String) cachedData.get("description");
            String sql = (String) cachedData.get("sql");
            java.util.Date cacheTime = (java.util.Date) cachedData.get("cacheTime");

            result.append(String.format("%d. 查询描述: %s\n", index++, description));
            result.append(String.format("   缓存时间: %s\n", cacheTime));
            result.append(String.format("   SQL摘要: %s...\n",
                    sql.length() > 50 ? sql.substring(0, 50) : sql));
            result.append("\n");
        }

        result.append(String.format("共 %d 条缓存记录", sqlCache.size()));
        return result.toString();
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
            return "获取数据库表名信息失败";
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
            return "获取数据库表结构信息失败";
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 执行分页查询
     */
    private Map<String, Object> executeWithPagination(String sql, int page, int pageSize) {
        Map<String, Object> result = new HashMap<>();

        // 1. 先查询总记录数（只有当需要分页时才查询）
        String countSql = buildCountSql(sql);
        Integer totalCount = null;
        try {
            log.info("执行总数查询SQL: " + countSql);
            totalCount = jdbcTemplate.queryForObject(countSql, Integer.class);
        } catch (Exception e) {
            log.warn("查询总数失败，将使用实际返回数据作为总数: " + e.getMessage());
        }

        // 2. 执行分页数据查询
        String paginatedSql = buildPaginatedSql(sql, page, pageSize);
        log.info("执行分页查询SQL: " + paginatedSql);
        List<Map<String, Object>> data = jdbcTemplate.queryForList(paginatedSql);

        // 3. 计算分页信息
        int actualCount = data.size();
        int total = totalCount != null ? totalCount : actualCount;
        int totalPages = (int) Math.ceil((double) total / pageSize);

        result.put("data", data);
        result.put("pagination", Map.of(
                "current", page,
                "pageSize", pageSize,
                "total", total,
                "totalPages", totalPages,
                "hasNext", page < totalPages,
                "hasPrev", page > 1
        ));

        return result;
    }

    /**
     * 构建统计总数的SQL
     */
    private String buildCountSql(String sql) {
        // 简单的COUNT SQL构建 - 对于复杂查询可能需要优化
        String upperSql = sql.toUpperCase().trim();
        if (upperSql.contains("GROUP BY")) {
            // 如果包含GROUP BY，需要用子查询
            return String.format("SELECT COUNT(*) FROM (%s) as count_table", sql);
        } else {
            // 简单查询，直接替换SELECT部分
            return sql.replaceFirst("(?i)SELECT.*?FROM", "SELECT COUNT(*) FROM");
        }
    }

    /**
     * 构建分页SQL
     */
    private String buildPaginatedSql(String sql, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        String upperSql = sql.toUpperCase();

        if (upperSql.contains("LIMIT")) {
            // 如果已经有LIMIT，替换它
            return sql.replaceFirst("(?i)LIMIT\\s+\\d+(?:\\s*,\\s*\\d+)?",
                    String.format("LIMIT %d, %d", offset, pageSize));
        } else {
            // 添加LIMIT
            return String.format("%s LIMIT %d, %d", sql, offset, pageSize);
        }
    }

    /**
     * 缓存SQL
     */
    private void cacheSql(String queryDescription, String sql) {
        String cacheKey = generateCacheKey(queryDescription);
        Map<String, Object> cacheData = new LinkedHashMap<>();
        cacheData.put("description", queryDescription);
        cacheData.put("sql", sql);
        cacheData.put("cacheTime", new java.util.Date());

        sqlCache.put(cacheKey, cacheData);
        log.info("缓存SQL成功: {}", queryDescription);
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String queryDescription) {
        // 简化查询描述，去除特殊字符和多余空格，转小写
        return queryDescription.trim().toLowerCase()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", "")
                .replaceAll("\\s+", " ");
    }

    /**
     * 验证SQL安全性
     */
    private boolean isValidSelectQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) return false;

        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT")) return false;

        String[] dangerousKeywords = {
                "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
                "TRUNCATE", "EXEC", "EXECUTE", "DECLARE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) return false;
        }
        return true;
    }

    /**
     * 清理SQL
     */
    private String cleanSql(String sql) {
        return sql.trim().replaceAll(";+$", "");
    }

    /**
     * 格式化成功结果
     */
    private String formatSuccessResult(Map<String, Object> result, String originalSql,
                                       long executionTime, int page, int pageSize) {
        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) result.get("pagination");

        sb.append("✅ 医保基金系统查询执行成功！\n\n");
        sb.append("📊 查询统计：\n");
        sb.append(String.format("- 当前页：第%d页\n", page));
        sb.append(String.format("- 每页大小：%d条\n", pageSize));
        sb.append(String.format("- 当前页记录数：%d条\n", data.size()));
        sb.append(String.format("- 总记录数：%d条\n", pagination.get("total")));
        sb.append(String.format("- 总页数：%d页\n", pagination.get("totalPages")));
        sb.append(String.format("- 执行时间：%dms\n\n", executionTime));

        sb.append("🔍 基础SQL（不含分页）：\n```sql\n").append(originalSql).append("\n```\n\n");

        if (data.isEmpty()) {
            sb.append("📝 查询结果：当前页无数据\n");
        } else {
            sb.append("📋 查询结果：\n");
            sb.append(formatAsTable(data));
        }

        // 分页导航提示
        if ((Boolean) pagination.get("hasPrev") || (Boolean) pagination.get("hasNext")) {
            sb.append("\n🔄 分页导航：\n");
            if ((Boolean) pagination.get("hasPrev")) {
                sb.append(String.format("- 上一页：page=%d\n", page - 1));
            }
            if ((Boolean) pagination.get("hasNext")) {
                sb.append(String.format("- 下一页：page=%d\n", page + 1));
            }
        }

        return sb.toString();
    }

    /**
     * 格式化错误结果
     */
    private String formatErrorResult(String sql, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("❌ 医保基金系统查询执行失败！\n\n");
        sb.append("🔍 执行的SQL：\n```sql\n").append(sql).append("\n```\n\n");
        sb.append("💥 错误信息：").append(error).append("\n\n");
        sb.append("🔧 建议检查：\n");
        sb.append("- 表名是否正确（hif_iption_type_stt_d、hif_cert_stt_d等）\n");
        sb.append("- 字段名是否准确（admdvs、biz_date、iption_cnt等）\n");
        sb.append("- 日期格式是否正确（YYYYMMDD或YYYYMM）\n");
        sb.append("- SQL语法是否有误\n");
        sb.append("- 是否有权限访问相关表\n");
        return sb.toString();
    }

    /**
     * 格式化表格显示
     */
    private String formatAsTable(List<Map<String, Object>> data) {
        if (data.isEmpty()) return "无数据";

        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> row : data) {
            allColumns.addAll(row.keySet());
        }

        List<String> columns = new ArrayList<>(allColumns);
        StringBuilder sb = new StringBuilder();

        Map<String, Integer> columnWidths = new HashMap<>();
        for (String col : columns) {
            int maxWidth = col.length();
            for (Map<String, Object> row : data) {
                Object value = row.get(col);
                String strValue = value != null ? value.toString() : "NULL";
                maxWidth = Math.max(maxWidth, strValue.length());
            }
            columnWidths.put(col, Math.min(maxWidth, 25));
        }

        sb.append("```\n");
        for (String col : columns) {
            sb.append(String.format("%-" + columnWidths.get(col) + "s | ", col));
        }
        sb.append("\n");

        for (String col : columns) {
            sb.append(String.join("", Collections.nCopies(columnWidths.get(col), "-")));
            sb.append("-+-");
        }
        sb.append("\n");

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
