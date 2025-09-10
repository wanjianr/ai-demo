package com.simonking.boot.mcp.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
@Schema(description = "分页请求对象")
public class PageRequestDTO {

    @ToolParam(description = "分页的页码,从1开始")
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @ToolParam(description = "分页的页数,不可以超过100")
    @Schema(description = "页数", example = "10")
    private Integer size = 10;

}
