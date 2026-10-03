package com.portifolio.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {

    private String idToken;

    private Boolean rememberMe = false;
}
