// dto/PagedResponseDto.java
package com.agilesolutions.account.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paged response wrapper")
public class PagedResponseDto<T> {

    @Schema(description = "Data content list")
    private List<T> content;

    @Schema(description = "Current page number (0-based)")
    private int page;

    @Schema(description = "Page size")
    private int size;

    @Schema(description = "Total number of elements")
    private long totalElements;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Is last page")
    private boolean last;
}