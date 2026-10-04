package com.portifolio.dto;

import java.time.LocalDateTime;

public record ExclusaoContaResponse(String comprovanteHash, LocalDateTime dataExclusao, String status) {}
