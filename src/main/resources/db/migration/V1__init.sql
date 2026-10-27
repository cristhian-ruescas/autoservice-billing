CREATE TABLE orcamento (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ordem_servico_id VARCHAR(36) NOT NULL UNIQUE,
    correlation_id VARCHAR(36) NOT NULL,
    valor_total DECIMAL(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    email_enviado BOOLEAN NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE TABLE orcamento_item (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    orcamento_id VARCHAR(36) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    valor DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_item_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamento (id)
);

CREATE TABLE pagamento (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ordem_servico_id VARCHAR(36) NOT NULL UNIQUE,
    orcamento_id VARCHAR(36) NOT NULL,
    correlation_id VARCHAR(36) NOT NULL,
    mp_order_id VARCHAR(120),
    status VARCHAR(40) NOT NULL,
    status_mp VARCHAR(40),
    valor DECIMAL(12, 2) NOT NULL,
    qr_data VARCHAR(2000),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);
