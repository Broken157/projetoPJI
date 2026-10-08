package com.portifolio.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.portifolio.dto.*;
import com.portifolio.exception.*;
import com.portifolio.model.*;
import com.portifolio.model.enums.NivelExperiencia;
import com.portifolio.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Catálogo futuro simulado em memória: não insere nem completa o seed oficial. */
class PerfilProfissionalServiceTest {
    AreaArtisticaRepository areas = mock(AreaArtisticaRepository.class);
    FuncaoRepository funcoes = mock(FuncaoRepository.class);
    EspecializacaoRepository specs = mock(EspecializacaoRepository.class);
    PerfilArtistaRepository perfis = mock(PerfilArtistaRepository.class);
    PerfilProfissionalService service = new PerfilProfissionalService(areas, funcoes, specs, perfis);
    Map<Long, Funcao> catalogo = new HashMap<>();
    Map<Long, Especializacao> especializacoes = new HashMap<>();
    AreaArtistica area;
    PerfilArtista perfil;

    @BeforeEach void preparar() {
        area = new AreaArtistica(); area.setId((short)1);
        perfil = new PerfilArtista();
        for (long id = 1; id <= 5; id++) {
            var spec = new Especializacao(); spec.setId(id); especializacoes.put(id, spec);
            var funcao = new Funcao(); funcao.setId(id); funcao.setArea(area);
            funcao.setEspecializacoes(Set.of(spec)); catalogo.put(id, funcao);
        }
        when(areas.findAllById(any())).thenReturn(List.of(area));
        when(funcoes.buscarTaxonomia(anySet())).thenAnswer(a -> ((Set<Long>)a.getArgument(0)).stream()
                .map(catalogo::get).filter(Objects::nonNull).toList());
        when(specs.findAllById(any())).thenAnswer(a -> ((Set<Long>)a.getArgument(0)).stream()
                .map(especializacoes::get).filter(Objects::nonNull).toList());
    }

    @Test void cincoFuncoesECincoEspecializacoesSaoAceitas() {
        var ids = LongStream.rangeClosed(1, 5).boxed().toList();
        var selecoes = service.validar(perfil, pedido(ids, ids));
        service.reconciliar(perfil, selecoes);
        var vinculo = perfil.getAreas().iterator().next();
        assertThat(vinculo.getFuncoes()).hasSize(5);
        assertThat(vinculo.getEspecializacoes()).hasSize(5);
    }

    @ParameterizedTest @ValueSource(strings = {"funcoes", "especializacoes"})
    void sextaOpcaoRejeitadaAntesDeConsultarOuModificar(String tipo) {
        var seis = LongStream.rangeClosed(1, 6).boxed().toList();
        var request = pedido(tipo.equals("funcoes") ? seis : List.of(1L),
                tipo.equals("especializacoes") ? seis : List.of(1L));
        assertThatThrownBy(() -> service.validar(perfil, request)).isInstanceOf(UnprocessableEntityException.class);
        verifyNoInteractions(areas, funcoes, specs, perfis);
        assertThat(perfil.getAreas()).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"funcoes", "especializacoes"})
    void idsDuplicadosRejeitadosNoNovoContrato(String tipo) {
        var request = pedido(tipo.equals("funcoes") ? List.of(1L, 1L) : List.of(1L),
                tipo.equals("especializacoes") ? List.of(1L, 1L) : List.of(1L));
        assertThatThrownBy(() -> service.validar(perfil, request)).isInstanceOf(UnprocessableEntityException.class);
    }

    @Test void podaPreservaEspecializacaoAceitaPorOutraFuncaoDaMesmaArea() {
        catalogo.get(2L).setEspecializacoes(Set.of(especializacoes.get(1L), especializacoes.get(2L)));
        var vinculo = new PerfilArtistaArea(); vinculo.setArea(area); vinculo.setPrincipal(true);
        vinculo.setNivelExperiencia(NivelExperiencia.INICIANTE);
        vinculo.getFuncoes().addAll(List.of(catalogo.get(1L), catalogo.get(2L), catalogo.get(3L)));
        vinculo.getEspecializacoes().addAll(especializacoes.values()); perfil.getAreas().add(vinculo);
        var request = pedido(List.of(2L), null);
        service.reconciliar(perfil, service.validar(perfil, request));
        assertThat(vinculo.getEspecializacoes()).extracting(Especializacao::getId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(vinculo.getNivelExperiencia()).isEqualTo(NivelExperiencia.INICIANTE);
    }

    @Test void vinculoInalteradoPreservaIdentidadeExperienciaETimestamp() {
        var vinculo = new PerfilArtistaArea(); vinculo.setArea(area); vinculo.setPrincipal(true);
        vinculo.setNivelExperiencia(NivelExperiencia.EXPERIENTE);
        var timestamp = LocalDateTime.of(2025, 1, 1, 0, 0); vinculo.setUltimaAtualizacao(timestamp);
        vinculo.getFuncoes().add(catalogo.get(1L)); vinculo.getEspecializacoes().add(especializacoes.get(1L));
        perfil.getAreas().add(vinculo);
        service.reconciliar(perfil, service.validar(perfil, pedido(null, null)));
        assertThat(perfil.getAreas()).containsExactly(vinculo);
        assertThat(vinculo.getUltimaAtualizacao()).isEqualTo(timestamp);
        assertThat(vinculo.getNivelExperiencia()).isEqualTo(NivelExperiencia.EXPERIENTE);
    }

    @Test void idInexistenteNaoModificaEstado() {
        assertThatThrownBy(() -> service.validar(perfil, pedido(List.of(999L), List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(perfil.getAreas()).isEmpty(); verifyNoInteractions(perfis);
    }

    @Test void especializacaoDeOutraFuncaoNaoEhAceita() {
        assertThatThrownBy(() -> service.validar(perfil, pedido(List.of(1L), List.of(2L))))
                .isInstanceOf(UnprocessableEntityException.class);
        assertThat(perfil.getAreas()).isEmpty();
    }

    @Test void funcaoNaoPodeAtravessarAreas() {
        var outra = new AreaArtistica(); outra.setId((short)2); catalogo.get(1L).setArea(outra);
        assertThatThrownBy(() -> service.validar(perfil, pedido(List.of(1L), List.of())))
                .isInstanceOf(UnprocessableEntityException.class);
    }

    @Test void camposProfissionaisLegadosENovosNaoPodemSeContradizer() {
        var request = pedido(List.of(1L), List.of(1L)); request.setAreaPrincipalId((short)2);
        assertThatThrownBy(() -> service.validar(perfil, request)).isInstanceOf(UnprocessableEntityException.class);
    }

    @Test void limitesSaoPorAreaSemLimiteGlobalDeCinco() {
        var outra = new AreaArtistica(); outra.setId((short)2);
        when(areas.findAllById(any())).thenReturn(List.of(area, outra));
        for (long id = 6; id <= 10; id++) {
            var spec = new Especializacao(); spec.setId(id); especializacoes.put(id, spec);
            var funcao = new Funcao(); funcao.setId(id); funcao.setArea(outra);
            funcao.setEspecializacoes(Set.of(spec)); catalogo.put(id, funcao);
        }
        var request = pedido(LongStream.rangeClosed(1, 5).boxed().toList(), LongStream.rangeClosed(1, 5).boxed().toList());
        var secundaria = new PerfilArtistaAreaRequest(); secundaria.setAreaId((short)2);
        secundaria.setFuncaoIds(LongStream.rangeClosed(6, 10).boxed().toList());
        secundaria.setEspecializacaoIds(LongStream.rangeClosed(6, 10).boxed().toList());
        request.setAreas(List.of(request.getAreas().getFirst(), secundaria));
        service.reconciliar(perfil, service.validar(perfil, request));
        assertThat(perfil.getAreas()).hasSize(2);
        assertThat(perfil.getFuncoes()).hasSize(10);
        assertThat(perfil.getAreas()).allSatisfy(a -> assertThat(a.getEspecializacoes()).hasSize(5));
    }

    private PerfilArtistaRequest pedido(List<Long> funcs, List<Long> especializacoes) {
        var request = new PerfilArtistaRequest(); var a = new PerfilArtistaAreaRequest();
        a.setAreaId((short)1); a.setPrincipal(true); a.setFuncaoIds(funcs); a.setEspecializacaoIds(especializacoes);
        request.setAreas(List.of(a)); return request;
    }
}
