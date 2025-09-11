package com.simonking.boot.mcp.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * created on 2025-06-27 09:01
 *
 * @author 刘康
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Antd表格响应对象")
public class AntdTableResponseDTO {

    @JsonProperty("columns")
    @Schema(description = "列头定义", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Column> columns;

    @JsonProperty("dataSource")
    @Schema(description = "数据源", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Map<String, Object>> dataSource;

    @JsonProperty("topText")
    @Schema(description = "顶部提示信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String topText;

    @JsonProperty("bottomText")
    @Schema(description = "底部总结信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bottomText;

    @JsonProperty("pagination")
    @Schema(description = "分页信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Pagination pagination;

    /**
     * 判断是否为数字类型
     */
    public static boolean isNumberClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return Number.class.isAssignableFrom(clazz)
                || clazz == byte.class
                || clazz == short.class
                || clazz == int.class
                || clazz == long.class
                || clazz == float.class
                || clazz == double.class;
    }

    /**
     * 判断是否为日期时间类型
     */
    public static boolean isDateTimeClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        return java.util.Date.class.isAssignableFrom(clazz) ||
                java.time.temporal.Temporal.class.isAssignableFrom(clazz);
    }

    /**
     * 列定义
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "列定义")
    public static class Column {

        @JsonProperty("title")
        @Schema(description = "列标题", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;

        @JsonProperty("dataIndex")
        @Schema(description = "数据索引", requiredMode = Schema.RequiredMode.REQUIRED)
        private String dataIndex;

        @JsonProperty("key")
        @Schema(description = "唯一键", requiredMode = Schema.RequiredMode.REQUIRED)
        private String key;

        @JsonProperty("align")
        @Schema(description = "对齐方式")
        private String align;

        @JsonProperty("width")
        @Schema(description = "列宽")
        private Integer width;

        @JsonProperty("resizable")
        @Schema(description = "是否可调整大小")
        private Boolean resizable;

        @JsonProperty("ellipsis")
        @Schema(description = "是否显示省略号")
        private Boolean ellipsis;

        /**
         * 基于标题和数据类型创建列定义
         */
        public Column(String title, Class<?> clazz) {
            this.title = title;
            this.dataIndex = title;
            this.key = title;
            this.width = computeWidth(title, clazz);
            if (isNumberClass(clazz)) {
                this.align = "right";
            }
            this.resizable = true;
            this.ellipsis = true;
        }

        /**
         * 计算列宽
         */
        public static int computeWidth(String inputStr, Class<?> clazz) {
            int width = 20;

            for (int i = 0; i < inputStr.length(); i++) {
                char c = inputStr.charAt(i);
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    width += 17; // 中文字符
                } else if (Character.isLetter(c)) {
                    width += 12; // 英文字母
                } else if (Character.isDigit(c)) {
                    width += 8;  // 数字
                } else {
                    width += 7;  // 其他字符
                }
            }

            if (isNumberClass(clazz)) {
                return Math.max(120, width);
            }

            if (isDateTimeClass(clazz)) {
                return Math.max(160, width);
            }

            return width;
        }
    }

    /**
     * 分页信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "分页信息")
    public static class Pagination {

        @JsonProperty("total")
        @Schema(description = "总记录数", requiredMode = Schema.RequiredMode.REQUIRED)
        private long total;

        @JsonProperty("current")
        @Schema(description = "当前页码", requiredMode = Schema.RequiredMode.REQUIRED)
        private int current;

        @JsonProperty("pageSize")
        @Schema(description = "每页大小", requiredMode = Schema.RequiredMode.REQUIRED)
        private int pageSize;
    }
}
