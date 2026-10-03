package com.portifolio.service;

import com.portifolio.dto.PortfolioPagina;
import com.portifolio.exception.UnprocessableEntityException;
import com.portifolio.model.EmbedExterno;
import com.portifolio.repository.EmbedExternoRepository;
import com.portifolio.validation.VideoPortfolioValidator;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class PortfolioVideoService {
    @org.springframework.beans.factory.annotation.Value("${app.database.legacy:false}")
    private boolean legacySchema;
    private final EmbedExternoRepository videos;
    private final PortfolioAccessService acesso;
    private final VideoPortfolioValidator validator;
    public record VideoResponse(Long id, String urlOriginal, String embedUrl, String provedor, String legenda) {}

    @Transactional
    public VideoResponse cadastrar(Map<String, String> request) {
        var dono = acesso.artistaAtual();
        validarCampos(request);
        var video = validator.validar(request.get("url"));
        // Provedor ja validado; persistir o metadado obrigatorio do database04.
        var item = new EmbedExterno(); item.setTipoMidia(legacySchema ? (video.provedor().equalsIgnoreCase("SPOTIFY") ? "audio" : "video") : (video.provedor().equalsIgnoreCase("SPOTIFY") ? "AUDIO" : "VIDEO")); item.setArtista(dono); item.setUrlOriginal(video.urlOriginal());
        item.setPlataforma(video.provedor());
        item.setCodigoIframe("<iframe src=\"" + video.embedUrl() + "\" title=\"Vídeo do portfólio\" loading=\"lazy\" allowfullscreen></iframe>");
        item.setLegenda(legenda(request));
        dono.setUltimaAtualizacao(LocalDateTime.now());
        return dto(videos.saveAndFlush(item));
    }
    @Transactional
    public VideoResponse atualizar(Long id, Map<String, String> request) {
        var dono = acesso.artistaAtual();
        var item = videos.findByIdAndArtistaUsuarioId(id, dono.getUsuarioId()).orElseThrow(PortfolioAccessService::naoEncontrado);
        validarCampos(request);
        var video = validator.validar(request.get("url"));
        item.setUrlOriginal(video.urlOriginal());
        item.setPlataforma(video.provedor());
        item.setTipoMidia(legacySchema ? (video.provedor().equalsIgnoreCase("SPOTIFY") ? "audio" : "video") : (video.provedor().equalsIgnoreCase("SPOTIFY") ? "AUDIO" : "VIDEO"));
        item.setCodigoIframe("<iframe src=\"" + video.embedUrl() + "\" title=\"Vídeo do portfólio\" loading=\"lazy\" allowfullscreen></iframe>");
        item.setLegenda(legenda(request));
        dono.setUltimaAtualizacao(LocalDateTime.now());
        return dto(videos.saveAndFlush(item));
    }
    private void validarCampos(Map<String, String> request) {
        if (request == null || !request.containsKey("url") || request.keySet().stream().anyMatch(key -> !key.equals("url") && !key.equals("legenda")))
            throw new UnprocessableEntityException("Envie somente o link e a legenda, sem HTML ou identificadores de proprietário.");
    }
    private String legenda(Map<String, String> request) {
        String value = request.get("legenda");
        if (value != null && (value.length() > 255 || value.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')))
            throw new UnprocessableEntityException("A legenda deve ter até 255 caracteres, sem caracteres de controle.");
        return value == null || value.isBlank() ? null : value.strip();
    }
    @Transactional(readOnly = true)
    public PortfolioPagina<VideoResponse> meus(int page, int size) {
        return listar(acesso.artistaAtual().getUsuarioId(), page, size);
    }
    @Transactional(readOnly = true)
    public PortfolioPagina<VideoResponse> publicos(Long artista, int page, int size) {
        acesso.exigirPublico(artista); return listar(artista, page, size);
    }
    private PortfolioPagina<VideoResponse> listar(Long artista, int page, int size) {
        return PortfolioPagina.of(videos.findByArtistaUsuarioId(artista, PortfolioAccessService.pagina(page, size, "id")).map(this::dto));
    }
    @Transactional
    public void excluir(Long id) {
        var dono = acesso.artistaAtual();
        videos.delete(videos.findByIdAndArtistaUsuarioId(id, dono.getUsuarioId()).orElseThrow(PortfolioAccessService::naoEncontrado));
        videos.flush(); dono.setUltimaAtualizacao(LocalDateTime.now());
    }
    private VideoResponse dto(EmbedExterno item) {
        // Nunca devolve nem confia no HTML armazenado, inclusive registros de versões anteriores.
        var video = validator.validar(item.getUrlOriginal());
        return new VideoResponse(item.getId(), video.urlOriginal(), video.embedUrl(), video.provedor(), item.getLegenda());
    }
}
