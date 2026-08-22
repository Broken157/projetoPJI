package com.portifolio.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatSalaPaginaResponse {
    private List<ChatSalaResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasMore;
}
