package com.portifolio.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardSecaoResponse<T> {
    private List<T> content;
    private long totalElements;
    private boolean hasMore;
}
