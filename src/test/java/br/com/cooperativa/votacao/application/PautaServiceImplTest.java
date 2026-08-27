package br.com.cooperativa.votacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.cooperativa.votacao.application.impl.PautaServiceImpl;
import br.com.cooperativa.votacao.domain.exception.RecursoNaoEncontradoException;
import br.com.cooperativa.votacao.domain.model.Pauta;
import br.com.cooperativa.votacao.domain.repository.PautaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Testes dos casos de uso de pauta. */
@ExtendWith(MockitoExtension.class)
@DisplayName("PautaServiceImpl")
class PautaServiceImplTest {

    private static final Instant AGORA = Instant.parse("2026-08-27T14:00:00Z");

    @Mock private PautaRepository repositorio;

    private PautaServiceImpl servico() {
        return new PautaServiceImpl(repositorio, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("carimba a criação com o instante do relogio injetado")
    void criar() {
        when(repositorio.salvar(any(Pauta.class))).thenAnswer(c -> c.getArgument(0));

        var pauta = servico().criar("Reforma do estatuto", "Artigos 12 a 18.");

        assertThat(pauta.getTitulo()).isEqualTo("Reforma do estatuto");
        assertThat(pauta.getDescricao()).isEqualTo("Artigos 12 a 18.");
        // Sem o Clock injetado, este valor seria o tempo real e o teste não
        // conseguiria afirmar nada sobre ele.
        assertThat(pauta.getCriadaEm()).isEqualTo(AGORA);
        assertThat(pauta.getId()).isNotNull();
    }

    @Test
    @DisplayName("monta a página com o total vindo do repositorio")
    void listar() {
        var uma = Pauta.criar("A", null, AGORA);
        var outra = Pauta.criar("B", null, AGORA);
        when(repositorio.listarMaisRecentes(1, 2)).thenReturn(List.of(uma, outra));
        when(repositorio.contar()).thenReturn(7L);

        var pagina = servico().listar(1, 2);

        assertThat(pagina.conteudo()).containsExactly(uma, outra);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.tamanho()).isEqualTo(2);
        assertThat(pagina.totalElementos()).isEqualTo(7);
        // 7 elementos em páginas de 2 exigem 4 páginas: é o arredondamento para
        // cima que impede a última pagina de ser ignorada pelo cliente.
        assertThat(pagina.totalPaginas()).isEqualTo(4);
        assertThat(pagina.ultima()).isFalse();
    }

    @Test
    @DisplayName("busca devolve a pauta encontrada")
    void buscar() {
        var pauta = Pauta.criar("Reforma", null, AGORA);
        when(repositorio.buscarPorId(pauta.getId())).thenReturn(Optional.of(pauta));

        assertThat(servico().buscar(pauta.getId())).isEqualTo(pauta);
    }

    @Test
    @DisplayName("busca de pauta inexistente vira erro de negócio, não Optional vazio")
    void buscarInexistente() {
        var id = UUID.randomUUID();
        when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());
        var servico = servico();

        // Traduzir aqui evita que cada chamador precise decidir o que fazer com
        // um Optional vazio — e garante o mesmo 404 em todas as superfícies.
        assertThatThrownBy(() -> servico.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());
    }
}
