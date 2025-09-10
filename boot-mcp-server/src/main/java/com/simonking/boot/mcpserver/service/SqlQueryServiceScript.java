package com.simonking.boot.mcpserver.service;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


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
public class SqlQueryServiceScript {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行SQL查询并返回结果
     */
    @Tool(description = """
        执行SQL查询语句并返回格式化结果。参数：
        - sql: 要执行的SQL查询语句（仅支持SELECT语句）
        - maxRows: 最大返回行数（默认50，最大200）
        返回查询结果的格式化文本
        """)
    public String executeQuery(String sql, Integer maxRows) {
        try {
            // 安全检查
            if (!isValidSelectQuery(sql)) {
                return "安全限制：只允许执行SELECT查询语句，不支持INSERT、UPDATE、DELETE等操作";
            }

            if (maxRows == null || maxRows <= 0) maxRows = 50;
            if (maxRows > 200) maxRows = 200;

            // 清理和优化SQL
            String cleanSql = cleanSql(sql);
            String limitedSql = addLimitIfNeeded(cleanSql, maxRows);

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
     * 验证并生成SQL查询语句
     */
    @Tool(description = """
        根据自然语言描述验证和生成SQL查询语句。参数：
        - description: 查询需求的自然语言描述
        - suggestedSql: 建议的SQL语句（可选）
        返回SQL生成建议和注意事项
        """)
    public String validateAndSuggestSql(String description, String suggestedSql) {
        StringBuilder response = new StringBuilder();
        response.append("医保基金系统查询需求分析：").append(description).append("\n\n");

        if (suggestedSql != null && !suggestedSql.trim().isEmpty()) {
            response.append("建议的SQL语句：\n");
            response.append("```sql\n").append(suggestedSql).append("\n```\n\n");

            // 基本验证
            if (!isValidSelectQuery(suggestedSql)) {
                response.append("SQL验证失败：只允许SELECT查询语句\n\n");
            } else {
                response.append("SQL基本验证通过\n\n");
            }
        }

        response.append("医保系统SQL生成建议：\n");
        response.append("- 使用反引号包围表名和字段名：`table_name`, `field_name`\n");
        response.append("- 日期字段格式：YYYYMMDD（如 biz_date）或 YYYYMM（如 biz_mon）\n");
        response.append("- 区划字段：admdvs（医保区划6位）、prov_admdvs（省级区划6位）\n");
        response.append("- 银行相关：bankacct（银行账号）、bank_acct_name（银行账户名称）\n");
        response.append("- 添加适当的WHERE条件，特别是日期和区划过滤\n");
        response.append("- 考虑使用ORDER BY按日期或区划排序\n");
        response.append("- 系统会自动添加LIMIT限制以确保性能\n\n");

        response.append("🔧 使用executeQuery工具执行生成的SQL语句");

        return response.toString();
    }

    /**
     * 获取医保基金系统数据库结构信息（静态版本）
     */
    @Tool(description = """
        获取医保基金系统中预定义的数据库表结构信息。
        返回当前系统支持查询的所有表结构说明
        """)
    public String getDatabaseStructure() {
        return getMedicalInsuranceSchemaTemplate();
    }

    /**
     * 医保系统常用查询模板
     */
    @Tool(description = """
        获取医保基金系统的常用查询模板和示例。参数：
        - queryType: 查询类型（可选）：'统计'、'明细'、'区划'、'银行'、'凭证'
        返回相应类型的查询模板和示例
        """)
    public String getQueryTemplates(String queryType) {
        return getMedicalInsuranceQueryTemplates(queryType);
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

    /**
     * 医保基金系统数据库结构模板
     */
    private String getMedicalInsuranceSchemaTemplate() {
        return """
        医保基金系统数据库结构说明
        
        ==========================================
        
        医保基金归集统计相关表：
        
        1. `hif_iption_type_stt_d` - 医保基金归集分类统计表
           - `iption_cnt` (INT) - 归集条数
           - `iption_data_type` (VARCHAR(20)) - 归集数据分类
           - `biz_date` (VARCHAR(20)) - 业务日期（格式：YYYYMMDD）
           - `biz_mon` (VARCHAR(6)) - 月份（格式：YYYYMM）
           - `prov_admdvs` (VARCHAR(6)) - 省级区划
           - `admdvs` (VARCHAR(6)) - 医保区划
           - 主键：(iption_data_type, biz_date, admdvs)
        
        2. `hif_cert_stt_d` - 医保基金凭证统计表
           - `biz_date` (VARCHAR(8)) - 凭证日期（格式：YYYYMMDD）
           - `biz_mon` (VARCHAR(6)) - 月份（格式：YYYYMM）
           - `biz_msg_no` (VARCHAR(6)) - 业务报文编号
           - `prov_admdvs` (VARCHAR(6)) - 省级区划
           - `admdvs` (VARCHAR(6)) - 医保区划
           - `bankacct` (VARCHAR(50)) - 银行账号
           - `bank_acct_name` (VARCHAR(200)) - 银行账户名称
           - `cert_cnt` (INT) - 归集条数
           - 主键：(biz_date, biz_msg_no, admdvs, bankacct, bank_acct_name)
        
        🗺️ 行政区划相关表：
        
        3. `admdvs_a` - 区划表
           - `ADMDVS` (VARCHAR(6)) - 医保区划（主键）
           - `ADMDVS_NAME` (VARCHAR(100)) - 医保区划名称
           - `PRNT_ADMDVS` (VARCHAR(9)) - 上级财政区划
           - `admdvs_lv` (VARCHAR(6)) - 医保区划级别
           - `vali_flag` (VARCHAR(3)) - 有效标志（默认'1'）
        
        📋 医保基金凭证详细信息表：
        
        4. `hif_cert_xml_d` - 医保基金凭证XML表 (数据库：adb_ylpj_prd_v103)
           - `cert_xml_id` (VARCHAR(40)) - 凭证xmlId（主键）
           - `hi_bank_docno` (VARCHAR(30)) - 医保银行单号
           - `cert_date` (VARCHAR(8)) - 凭证日期（格式：YYYYMMDD）
           - `biz_msg_no` (VARCHAR(6)) - 业务报文编号
           - `admdvs` (VARCHAR(6)) - 医保区划
           - `year` (VARCHAR(4)) - 年度
           - `bankacct` (VARCHAR(50)) - 银行账号
           - `bank_acct_name` (VARCHAR(200)) - 银行账户名称
           - `xml_data` (LONGTEXT) - xml数据
           - `biz_date` (VARCHAR(20)) - 业务日期
        
        💰 医保钱包银行统计表：
        
        5. `hi_wlt_bank_stt_d` - 医保钱包银行统计表 (数据库：adb_ylpj_prd_v103)
           - `ADMDVS` (VARCHAR(6)) - 医保区划
           - `BANK_TYPE_CODE` (VARCHAR(3)) - 银行行别代码
           - `NORM_CNT` (INT) - 正常量（默认0）
           - `BIZ_DATE` (VARCHAR(10)) - 业务日期（格式：YYYY-MM-DD）
           - 主键：(ADMDVS, BANK_TYPE_CODE, BIZ_DATE)
        
        ==========================================
        
        📅 日期字段格式说明：
        - `biz_date` (业务日期)：YYYYMMDD 或 YYYY-MM-DD
        - `biz_mon` (月份)：YYYYMM
        - `cert_date` (凭证日期)：YYYYMMDD
        - `year` (年度)：YYYY
        
        🏛️ 区划字段说明：
        - `admdvs`：医保区划（6位数字）
        - `prov_admdvs`：省级区划（6位数字）
        - `PRNT_ADMDVS`：上级财政区划（9位）
        - `admdvs_lv`：区划级别
        
        🏦 银行相关字段：
        - `bankacct`：银行账号（50位）
        - `bank_acct_name`：银行账户名称（200位）
        - `BANK_TYPE_CODE`：银行行别代码（3位）
        - `hi_bank_docno`：医保银行单号（30位）
        
        💡 查询示例：
        
        基础统计查询：
        ```sql
        -- 按区划统计归集数据
        SELECT `admdvs`, `iption_data_type`, SUM(`iption_cnt`) as total_cnt
        FROM `hif_iption_type_stt_d`
        WHERE `biz_date` >= '20240101'
        GROUP BY `admdvs`, `iption_data_type`
        ORDER BY total_cnt DESC;
        
        -- 查询特定月份的凭证统计
        SELECT `admdvs`, `bank_acct_name`, SUM(`cert_cnt`) as total_certs
        FROM `hif_cert_stt_d`
        WHERE `biz_mon` = '202401'
        GROUP BY `admdvs`, `bank_acct_name`
        ORDER BY total_certs DESC;
        ```
        
        关联查询示例：
        ```sql
        -- 归集统计与区划信息关联
        SELECT h.`admdvs`, a.`ADMDVS_NAME`, h.`iption_data_type`, 
               SUM(h.`iption_cnt`) as total_cnt
        FROM `hif_iption_type_stt_d` h
        LEFT JOIN `admdvs_a` a ON h.`admdvs` = a.`ADMDVS`
        WHERE h.`biz_date` >= '20240101' AND a.`vali_flag` = '1'
        GROUP BY h.`admdvs`, a.`ADMDVS_NAME`, h.`iption_data_type`
        ORDER BY total_cnt DESC;
        
        -- 银行统计查询
        SELECT `ADMDVS`, `BANK_TYPE_CODE`, SUM(`NORM_CNT`) as total_norm
        FROM `adb_ylpj_prd_v103`.`hi_wlt_bank_stt_d`
        WHERE `BIZ_DATE` >= '2024-01-01'
        GROUP BY `ADMDVS`, `BANK_TYPE_CODE`
        ORDER BY total_norm DESC;
        ```
        
        ⚠️  医保系统查询注意事项：
        - 跨数据库查询时需要指定数据库名：adb_ylpj_prd_v103.表名
        - 日期字段格式要准确：YYYYMMDD、YYYYMM、YYYY-MM-DD
        - 区划代码通常是6位数字字符串
        - 银行相关字段长度限制较严格
        - 建议查询时添加日期范围条件以提升性能
        - 使用 vali_flag = '1' 过滤有效记录
        """;
    }

    /**
     * 医保系统常用查询模板
     */
    private String getMedicalInsuranceQueryTemplates(String queryType) {
        StringBuilder templates = new StringBuilder();
        templates.append("🔍 医保基金系统常用查询模板\n\n");

        if (queryType == null || queryType.contains("统计")) {
            templates.append("📊 统计类查询模板：\n\n");
            templates.append("1. 按区划统计归集数据：\n");
            templates.append("```sql\n");
            templates.append("SELECT `admdvs`, SUM(`iption_cnt`) as total_cnt\n");
            templates.append("FROM `hif_iption_type_stt_d`\n");
            templates.append("WHERE `biz_date` >= '20240101'\n");
            templates.append("GROUP BY `admdvs`\n");
            templates.append("ORDER BY total_cnt DESC;\n");
            templates.append("```\n\n");

            templates.append("2. 月度凭证统计：\n");
            templates.append("```sql\n");
            templates.append("SELECT `biz_mon`, SUM(`cert_cnt`) as monthly_certs\n");
            templates.append("FROM `hif_cert_stt_d`\n");
            templates.append("GROUP BY `biz_mon`\n");
            templates.append("ORDER BY `biz_mon` DESC;\n");
            templates.append("```\n\n");
        }

        if (queryType == null || queryType.contains("明细")) {
            templates.append("📋 明细类查询模板：\n\n");
            templates.append("3. 凭证详细信息查询：\n");
            templates.append("```sql\n");
            templates.append("SELECT `cert_xml_id`, `hi_bank_docno`, `cert_date`, `bank_acct_name`\n");
            templates.append("FROM `adb_ylpj_prd_v103`.`hif_cert_xml_d`\n");
            templates.append("WHERE `cert_date` >= '20240101' AND `admdvs` = '440100'\n");
            templates.append("ORDER BY `cert_date` DESC;\n");
            templates.append("```\n\n");
        }

        if (queryType == null || queryType.contains("区划")) {
            templates.append("🗺️ 区划相关查询模板：\n\n");
            templates.append("4. 区划信息查询：\n");
            templates.append("```sql\n");
            templates.append("SELECT `ADMDVS`, `ADMDVS_NAME`, `admdvs_lv`\n");
            templates.append("FROM `admdvs_a`\n");
            templates.append("WHERE `vali_flag` = '1' AND `PRNT_ADMDVS` = '440000000'\n");
            templates.append("ORDER BY `ADMDVS`;\n");
            templates.append("```\n\n");
        }

        if (queryType == null || queryType.contains("银行")) {
            templates.append("🏦 银行相关查询模板：\n\n");
            templates.append("5. 银行统计查询：\n");
            templates.append("```sql\n");
            templates.append("SELECT `BANK_TYPE_CODE`, COUNT(*) as bank_count, SUM(`NORM_CNT`) as total_norm\n");
            templates.append("FROM `adb_ylpj_prd_v103`.`hi_wlt_bank_stt_d`\n");
            templates.append("WHERE `BIZ_DATE` >= '2024-01-01'\n");
            templates.append("GROUP BY `BANK_TYPE_CODE`\n");
            templates.append("ORDER BY total_norm DESC;\n");
            templates.append("```\n\n");
        }

        if (queryType == null || queryType.contains("凭证")) {
            templates.append("📄 凭证相关查询模板：\n\n");
            templates.append("6. 凭证与统计关联查询：\n");
            templates.append("```sql\n");
            templates.append("SELECT c.`admdvs`, c.`bank_acct_name`, SUM(c.`cert_cnt`) as total_certs\n");
            templates.append("FROM `hif_cert_stt_d` c\n");
            templates.append("WHERE c.`biz_date` >= '20240101'\n");
            templates.append("GROUP BY c.`admdvs`, c.`bank_acct_name`\n");
            templates.append("HAVING total_certs > 100\n");
            templates.append("ORDER BY total_certs DESC;\n");
            templates.append("```\n\n");
        }

        templates.append("💡 使用提示：\n");
        templates.append("- 将上述模板中的日期、区划代码替换为实际需要的值\n");
        templates.append("- 跨数据库查询时记得加上数据库前缀\n");
        templates.append("- 大数据量查询建议添加LIMIT限制\n");
        templates.append("- 使用executeQuery工具执行这些模板\n");

        return templates.toString();
    }
}
