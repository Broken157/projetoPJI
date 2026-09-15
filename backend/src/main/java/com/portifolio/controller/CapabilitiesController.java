package com.portifolio.controller;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/capacidades")
public class CapabilitiesController {
    @Value("${app.database.legacy:false}") private boolean legacy;
    @GetMapping public Map<String,Object> get() { return Map.of(
        "taxonomia",!legacy,"rascunhoVaga",false,"exclusaoConta",false,
        "edicaoAvancadaPerfil",false,"editarMensagens",!legacy,"comunidades",true,
        "motivoTaxonomia",legacy?"BLOQUEADA POR SCHEMA DO BANCO: áreas, funções e especializações ausentes.":""); }
}
