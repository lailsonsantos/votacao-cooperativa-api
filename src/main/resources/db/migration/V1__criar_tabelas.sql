-- ---------------------------------------------------------------------------
-- Estrutura inicial: pauta, sessao de votacao e voto.
--
-- O schema e versionado pelo Flyway em vez de gerado por ddl-auto, para que a
-- evolucao seja explicita, revisavel e reproduzivel em qualquer ambiente.
-- ---------------------------------------------------------------------------

CREATE TABLE pauta (
    id         UUID          NOT NULL,
    titulo     VARCHAR(200)  NOT NULL,
    descricao  VARCHAR(2000),
    criada_em  TIMESTAMP     NOT NULL,
    CONSTRAINT pk_pauta PRIMARY KEY (id)
);

CREATE TABLE sessao_votacao (
    id             UUID      NOT NULL,
    pauta_id       UUID      NOT NULL,
    abertura_em    TIMESTAMP NOT NULL,
    fechamento_em  TIMESTAMP NOT NULL,
    CONSTRAINT pk_sessao_votacao PRIMARY KEY (id),
    CONSTRAINT fk_sessao_pauta   FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    -- Uma pauta tem no maximo uma sessao. A regra vive no banco para que nem
    -- mesmo duas requisicoes simultaneas de abertura consigam viola-la.
    CONSTRAINT uk_sessao_pauta   UNIQUE (pauta_id)
);

CREATE TABLE voto (
    id            UUID        NOT NULL,
    sessao_id     UUID        NOT NULL,
    associado_id  VARCHAR(11) NOT NULL,
    opcao         VARCHAR(3)  NOT NULL,
    criado_em     TIMESTAMP   NOT NULL,
    CONSTRAINT pk_voto        PRIMARY KEY (id),
    CONSTRAINT fk_voto_sessao FOREIGN KEY (sessao_id) REFERENCES sessao_votacao (id),
    CONSTRAINT ck_voto_opcao  CHECK (opcao IN ('SIM', 'NAO')),
    -- Garantia de "um voto por associado por pauta". E esta constraint, e nao
    -- uma consulta previa na aplicacao, que resolve corretamente a corrida entre
    -- duas requisicoes simultaneas do mesmo associado.
    CONSTRAINT uk_voto_sessao_associado UNIQUE (sessao_id, associado_id)
);

-- Serve a consulta de apuracao (COUNT ... GROUP BY opcao) como index-only scan,
-- mantendo o custo constante mesmo com centenas de milhares de votos.
CREATE INDEX ix_voto_sessao_opcao ON voto (sessao_id, opcao);

-- Acelera a listagem de pautas ordenada por data de cadastro.
CREATE INDEX ix_pauta_criada_em ON pauta (criada_em DESC);
