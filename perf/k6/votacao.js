/**
 * Teste de carga da API de votacao (Tarefa Bonus 2).
 *
 * O enunciado pede que a aplicacao se comporte bem em cenarios com centenas de
 * milhares de votos e sugere testes de performance como forma de observar isso.
 * Este script cobre os tres cenarios que importam:
 *
 *   1. carga_votos          - volume alto de votos distintos e concorrentes;
 *   2. apuracao_concorrente - leitura do resultado enquanto a votacao acontece;
 *   3. voto_duplicado       - 100 usuarios votando com o MESMO CPF.
 *
 * O terceiro e o mais importante: e ele que prova que a unicidade sobrevive a
 * concorrencia real. Uma implementacao que checasse "SELECT antes do INSERT"
 * passaria nos outros dois e falharia neste de forma intermitente.
 *
 * Execucao:
 *   k6 run perf/k6/votacao.js
 *   k6 run -e BASE_URL=https://votacao-cooperativa-api.onrender.com perf/k6/votacao.js
 */

import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import exec from 'k6/execution';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API = `${BASE_URL}/api/v1`;

/** Votos aceitos com o mesmo CPF. O valor correto e sempre exatamente 1. */
const votosDuplicadosAceitos = new Counter('votos_duplicados_aceitos');

/** Proporcao de respostas 409 no cenario de duplicidade (o esperado). */
const conflitosEsperados = new Rate('conflitos_esperados');

export const options = {
  scenarios: {
    carga_votos: {
      executor: 'ramping-vus',
      exec: 'votar',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },
        { duration: '1m',  target: 500 },
        { duration: '2m',  target: 500 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
    apuracao_concorrente: {
      executor: 'constant-vus',
      exec: 'apurar',
      vus: 50,
      duration: '4m',
      startTime: '10s',
    },
    voto_duplicado: {
      executor: 'per-vu-iterations',
      exec: 'votarDuplicado',
      vus: 100,
      iterations: 1,
      startTime: '30s',
    },
  },

  thresholds: {
    // A escrita de voto e o caminho critico da assembleia.
    'http_req_duration{cenario:voto}':     ['p(95)<200'],
    // A apuracao usa COUNT agregado servido por indice; deve ser ainda mais rapida.
    'http_req_duration{cenario:apuracao}': ['p(95)<100'],
    'http_req_failed{cenario:apuracao}':   ['rate<0.001'],
    // Nenhum voto duplicado pode ser aceito. Zero, nao "quase zero".
    'votos_duplicados_aceitos':            ['count==1'],
  },
};

/**
 * Prepara a massa: cria a pauta e abre uma sessao longa o bastante para todo o teste.
 *
 * @returns {{pautaId: string}} identificador da pauta usada em todos os cenarios
 */
export function setup() {
  const pauta = http.post(
    `${API}/pautas`,
    JSON.stringify({ titulo: 'Teste de carga', descricao: 'Tarefa Bonus 2' }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  const pautaId = pauta.json('id');

  http.post(
    `${API}/pautas/${pautaId}/sessao`,
    JSON.stringify({ duracaoMinutos: 30 }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  return { pautaId };
}

/**
 * Gera um CPF valido e distinto por iteracao.
 *
 * CPFs precisam passar nos digitos verificadores, entao nao da para usar um
 * numero aleatorio qualquer: a API recusaria com 400 e o teste mediria o caminho
 * de erro em vez do caminho de escrita.
 *
 * @param {number} semente valor que distingue este CPF dos demais
 * @returns {string} CPF valido com 11 digitos
 */
function gerarCpf(semente) {
  const base = String(semente % 1_000_000_000).padStart(9, '0');
  const digitos = base.split('').map(Number);

  let soma = 0;
  for (let i = 0; i < 9; i++) soma += digitos[i] * (10 - i);
  let resto = soma % 11;
  const d1 = resto < 2 ? 0 : 11 - resto;
  digitos.push(d1);

  soma = 0;
  for (let i = 0; i < 10; i++) soma += digitos[i] * (11 - i);
  resto = soma % 11;
  const d2 = resto < 2 ? 0 : 11 - resto;

  return `${base}${d1}${d2}`;
}

/**
 * Cenario 1: registra votos de associados distintos sob carga crescente.
 *
 * @param {{pautaId: string}} dados massa preparada no setup
 */
export function votar(dados) {
  group('registro de voto', () => {
    // A combinacao de VU e iteracao garante um CPF unico por requisicao,
    // isolando o custo da escrita do custo do tratamento de duplicidade.
    const cpf = gerarCpf(exec.vu.idInTest * 100_000 + exec.vu.iterationInInstance);

    const resposta = http.post(
      `${API}/pautas/${dados.pautaId}/votos`,
      JSON.stringify({ associadoId: cpf, opcao: Math.random() > 0.4 ? 'SIM' : 'NAO' }),
      { headers: { 'Content-Type': 'application/json' }, tags: { cenario: 'voto' } },
    );

    check(resposta, { 'voto registrado (201)': (r) => r.status === 201 });
  });
}

/**
 * Cenario 2: apura o resultado enquanto a votacao esta em andamento.
 *
 * @param {{pautaId: string}} dados massa preparada no setup
 */
export function apurar(dados) {
  group('apuracao', () => {
    const resposta = http.get(`${API}/pautas/${dados.pautaId}/resultado`, {
      tags: { cenario: 'apuracao' },
    });

    check(resposta, {
      'resultado devolvido (200)': (r) => r.status === 200,
      'apuracao marcada como parcial': (r) => r.json('parcial') === true,
    });
  });
}

/**
 * Cenario 3: 100 usuarios simultaneos votando com o mesmo CPF.
 *
 * Exatamente um deve receber 201; todos os demais, 409. E o cenario que valida a
 * constraint unica sob concorrencia real.
 *
 * @param {{pautaId: string}} dados massa preparada no setup
 */
export function votarDuplicado(dados) {
  group('voto duplicado', () => {
    const resposta = http.post(
      `${API}/pautas/${dados.pautaId}/votos`,
      JSON.stringify({ associadoId: '19839091069', opcao: 'SIM' }),
      { headers: { 'Content-Type': 'application/json' }, tags: { cenario: 'duplicado' } },
    );

    if (resposta.status === 201) {
      votosDuplicadosAceitos.add(1);
    }
    conflitosEsperados.add(resposta.status === 409);

    check(resposta, {
      'aceito uma unica vez ou recusado com 409': (r) =>
        r.status === 201 || r.status === 409,
    });
  });
}
