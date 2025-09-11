package com.simonking.boot.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * created on 2025-06-27 09:01
 *
 * @author 刘康
 **/
@Data
@Builder
@Schema(description = "请求响应")
public class AntdTableResponseDTO {

    @Schema(description = "列头", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Column> columns;

    @Schema(description = "数据源", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Map<String, Object>> dataSource;

    @Schema(description = "提示信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String topText;

    @Schema(description = "总结信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bottomText;

    @Schema(description = "分页信息", requiredMode = Schema.RequiredMode.REQUIRED)
    private Pagination pagination;

    public static boolean isNumberClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        // 包装类型和基本类型都支持
        return Number.class.isAssignableFrom(clazz)
            || clazz == byte.class
            || clazz == short.class
            || clazz == int.class
            || clazz == long.class
            || clazz == float.class
            || clazz == double.class;
    }

    public static boolean isDateTimeClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        return java.util.Date.class.isAssignableFrom(clazz) ||
            java.time.temporal.Temporal.class.isAssignableFrom(clazz);
    }

    @Data
    public static class Column {
        @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
        private String title;
        @Schema(description = "数据索引", requiredMode = Schema.RequiredMode.REQUIRED)
        private String dataIndex;
        @Schema(description = "键", requiredMode = Schema.RequiredMode.REQUIRED)
        private String key;
        @Schema(description = "对齐方式", requiredMode = Schema.RequiredMode.REQUIRED)
        private String align;
        @Schema(description = "列宽", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer width;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean resizable;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean ellipsis;

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

        public static int computeWidth(String inputStr, Class<?> clazz) {

            int width = 20;

            for (int i = 0; i < inputStr.length(); i++) {
                char c = inputStr.charAt(i);
                // 检查是否为汉字
                if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                    width += 17;
                } else if (Character.isLetter(c)) {
                    width += 12;
                } else if (Character.isDigit(c)) {
                    width += 8;
                } else {
                    width += 7;
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

    @Data
    @Builder
    public static class Pagination {
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private long total;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private int current;
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        private int pageSize;
    }


}
