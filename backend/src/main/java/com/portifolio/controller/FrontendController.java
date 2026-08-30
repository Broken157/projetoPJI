package com.portifolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({"/vagas", "/vagas/", "/vagas/{*path}"})
    public String encaminharRotasReactDeVagas() {
        return "forward:/index.html";
    }
}
