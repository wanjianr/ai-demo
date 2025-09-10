package com.simonking.boot.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * created on 2025-06-27 09:48
 *
 * @author 刘康
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "查询语句请求对象")
public class QueryPageRequestDTO extends PageRequestDTO {

    @ToolParam(description = "用户查询语句")
    @Schema(description = "用户查询语句")
    private String query;

}
