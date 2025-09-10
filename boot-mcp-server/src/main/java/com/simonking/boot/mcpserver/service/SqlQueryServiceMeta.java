package com.simonking.boot.mcpserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class SqlQueryServiceMeta {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取数据库表结构信息，用于帮助AI理解数据库结构
     */
    @Tool(description = """
        获取数据库表结构信息。参数：
        - tablePattern: 表名模式（可以是部分表名，支持模糊匹配）
        - includeColumns: 是否包含列信息（默认true）
        返回表结构的详细信息，包括表名、列名、数据类型等
        """)
    public String getDatabaseSchema(String tablePattern, Boolean includeColumns) {
        try {
            if (includeColumns == null) includeColumns = true;

            DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            List<Map<String, Object>> tableInfo = new ArrayList<>();

            // 获取匹配的表
            String pattern = tablePattern != null ? "%" + tablePattern + "%" : "%";
            ResultSet tables = metaData.getTables(null, null, pattern, new String[]{"TABLE"});

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableComment = tables.getString("REMARKS");

                Map<String, Object> table = new HashMap<>();
                table.put("tableName", tableName);
                table.put("comment", tableComment);

                if (includeColumns) {
                    List<Map<String, Object>> columns = new ArrayList<>();
                    ResultSet rs = metaData.getColumns(null, null, tableName, null);

                    while (rs.next()) {
                        Map<String, Object> column = new HashMap<>();
                        column.put("columnName", rs.getString("COLUMN_NAME"));
                        column.put("dataType", rs.getString("TYPE_NAME"));
                        column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                        column.put("nullable", rs.getBoolean("NULLABLE"));
                        column.put("comment", rs.getString("REMARKS"));
                        columns.add(column);
                    }
                    rs.close();
                    table.put("columns", columns);
                }
                tableInfo.add(table);
            }
            tables.close();

            return formatSchemaInfo(tableInfo);
        } catch (SQLException e) {
            log.error("获取数据库表结构失败", e);
            return "获取数据库表结构失败: " + e.getMessage();
        }
    }

    /**
     * 执行查询SQL并返回结果
     */
    @Tool(description = """
        执行SQL查询语句。参数：
        - sql: 要执行的SQL查询语句（仅支持SELECT语句）
        - maxRows: 最大返回行数（默认100）
        返回查询结果的JSON格式数据
        """)
    public String executeQuery(String sql, Integer maxRows) {
        try {
            // 安全检查：只允许SELECT语句
            if (!sql.trim().toUpperCase().startsWith("SELECT")) {
                return "出于安全考虑，只允许执行SELECT查询语句";
            }

            if (maxRows == null) maxRows = 100;

            // 添加LIMIT限制
            String limitedSql = sql;
            if (!sql.toUpperCase().contains("LIMIT")) {
                limitedSql += " LIMIT " + maxRows;
            }

            List<Map<String, Object>> result = jdbcTemplate.queryForList(limitedSql);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("rowCount", result.size());
            response.put("data", result);
            response.put("sql", limitedSql);

            return formatQueryResult(response);
        } catch (Exception e) {
            log.error("SQL执行失败: " + sql, e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("sql", sql);
            return formatQueryResult(errorResponse);
        }
    }

    /**
     * 根据自然语言描述生成SQL查询建议
     */
    @Tool(description = """
        根据用户的自然语言查询需求，提供SQL生成指导。参数：
        - queryDescription: 查询需求的自然语言描述
        返回SQL生成的指导信息和建议
        """)
    public String generateSqlGuidance(String queryDescription) {
        return String.format("""
        基于查询需求：%s
        
        请按以下步骤生成SQL：
        1. 首先调用 getDatabaseSchema 工具，传入可能相关的表名关键词来获取数据库结构
        2. 分析查询需求，确定需要的表和字段
        3. 生成合适的SQL查询语句
        4. 调用 executeQuery 工具执行生成的SQL
        
        SQL生成要求：
        - 只生成SELECT查询语句
        - 使用适当的WHERE条件进行数据过滤
        - 考虑使用JOIN连接多个表（如需要）
        - 添加ORDER BY进行排序（如需要）
        - 字段名和表名使用反引号包围以避免关键字冲突
        
        输出格式要求：
        - 如果查询成功，以表格形式展示数据
        - 包含查询统计信息（如记录数、执行时间等）
        - 如果出错，清晰说明错误原因和建议
        """, queryDescription);
    }

    private String formatSchemaInfo(List<Map<String, Object>> tableInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库表结构信息：\n\n");

        for (Map<String, Object> table : tableInfo) {
            sb.append("表名: ").append(table.get("tableName"));
            if (table.get("comment") != null) {
                sb.append(" (").append(table.get("comment")).append(")");
            }
            sb.append("\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> columns = (List<Map<String, Object>>) table.get("columns");
            if (columns != null) {
                sb.append("列信息:\n");
                for (Map<String, Object> column : columns) {
                    sb.append("  - ").append(column.get("columnName"))
                            .append(" (").append(column.get("dataType")).append(")");
                    if (column.get("comment") != null) {
                        sb.append(" - ").append(column.get("comment"));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatQueryResult(Map<String, Object> result) {
        StringBuilder sb = new StringBuilder();

        if ((Boolean) result.get("success")) {
            sb.append("查询执行成功！\n\n");
            sb.append("执行的SQL: ").append(result.get("sql")).append("\n");
            sb.append("返回记录数: ").append(result.get("rowCount")).append("\n\n");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            if (data != null && !data.isEmpty()) {
                // 格式化为表格
                Set<String> allKeys = data.stream()
                        .flatMap(row -> row.keySet().stream())
                        .collect(Collectors.toSet());

                sb.append("查询结果:\n");
                // 表头
                sb.append(String.join(" | ", allKeys)).append("\n");
                sb.append(String.join(" | ", Collections.nCopies(allKeys.size(), "---"))).append("\n");

                // 数据行
                for (Map<String, Object> row : data) {
                    List<String> values = allKeys.stream()
                            .map(key -> String.valueOf(row.get(key)))
                            .collect(Collectors.toList());
                    sb.append(String.join(" | ", values)).append("\n");
                }
            }
        } else {
            sb.append("查询执行失败！\n\n");
            sb.append("错误信息: ").append(result.get("error")).append("\n");
            sb.append("执行的SQL: ").append(result.get("sql")).append("\n");
        }

        return sb.toString();
    }
}
