package com.portifolio.controller;
import com.portifolio.service.AvatarUpdateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController @RequestMapping("/api/usuarios/me/avatar") @RequiredArgsConstructor
public class AvatarController {
    private final AvatarUpdateService service;
    public record Request(String avatarUrl){}
    @PatchMapping @ResponseStatus(HttpStatus.NO_CONTENT) public void update(@RequestBody Request body){service.atualizar(body.avatarUrl());}
}
