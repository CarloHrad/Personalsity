-- Esquema inicial do Trinots (PostgreSQL).
-- Os nomes seguem a convencao do Spring/Hibernate (camelCase -> snake_case), porque em producao
-- o Hibernate roda com ddl-auto=validate e confere este esquema com as entidades.

CREATE TABLE curso (
    id_curso     UUID         NOT NULL,
    nome_curso   VARCHAR(255),
    instituicao  VARCHAR(255),
    duracao      INTEGER,
    CONSTRAINT pk_curso PRIMARY KEY (id_curso)
);

CREATE TABLE usuario (
    id_usuario        UUID         NOT NULL,
    nome              VARCHAR(255),
    sobrenome         VARCHAR(255),
    email             VARCHAR(255) NOT NULL,
    senha             VARCHAR(255),
    id_curso          UUID,
    semestre_atual    INTEGER,
    media_aprovacao   DOUBLE PRECISION,
    ativo             BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verificado  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT fk_usuario_curso FOREIGN KEY (id_curso) REFERENCES curso (id_curso)
);
CREATE INDEX idx_usuario_curso ON usuario (id_curso);

CREATE TABLE disciplina (
    id_disciplina    UUID         NOT NULL,
    nome_disciplina  VARCHAR(255),
    periodo          INTEGER,
    professor        VARCHAR(255),
    sala             VARCHAR(255),
    andar            INTEGER,
    cor              VARCHAR(255),
    status           VARCHAR(255),
    tipo_media       VARCHAR(255),
    id_usuario       UUID,
    arquivada        BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_disciplina PRIMARY KEY (id_disciplina),
    CONSTRAINT uk_disciplina_usuario_nome UNIQUE (id_usuario, nome_disciplina),
    CONSTRAINT fk_disciplina_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE horario (
    id_hora        UUID         NOT NULL,
    dia_semana     VARCHAR(255),
    hora_inicio    TIME,
    hora_fim       TIME,
    id_disciplina  UUID,
    CONSTRAINT pk_horario PRIMARY KEY (id_hora),
    CONSTRAINT fk_horario_disciplina FOREIGN KEY (id_disciplina) REFERENCES disciplina (id_disciplina)
);
CREATE INDEX idx_horario_disciplina ON horario (id_disciplina);

CREATE TABLE tarefa (
    id_tarefa       UUID         NOT NULL,
    nome_tarefa     VARCHAR(255),
    descricao       VARCHAR(500),
    data_entrega    DATE,
    hora_entrega    TIME,
    data_conclusao  TIMESTAMP(6),
    concluida       BOOLEAN      NOT NULL DEFAULT FALSE,
    id_disciplina   UUID,
    CONSTRAINT pk_tarefa PRIMARY KEY (id_tarefa),
    CONSTRAINT fk_tarefa_disciplina FOREIGN KEY (id_disciplina) REFERENCES disciplina (id_disciplina)
);
CREATE INDEX idx_tarefa_disciplina ON tarefa (id_disciplina);
CREATE INDEX idx_tarefa_data_entrega ON tarefa (data_entrega);

CREATE TABLE avaliacao (
    id_avaliacao    UUID         NOT NULL,
    nome_avaliacao  VARCHAR(255),
    descricao       VARCHAR(500),
    data_avaliacao  DATE,
    data_conclusao  TIMESTAMP(6),
    nota_obtida     DOUBLE PRECISION,
    nota_maxima     DOUBLE PRECISION,
    peso            DOUBLE PRECISION,
    concluida       BOOLEAN      NOT NULL DEFAULT FALSE,
    id_disciplina   UUID,
    CONSTRAINT pk_avaliacao PRIMARY KEY (id_avaliacao),
    CONSTRAINT fk_avaliacao_disciplina FOREIGN KEY (id_disciplina) REFERENCES disciplina (id_disciplina)
);
CREATE INDEX idx_avaliacao_disciplina ON avaliacao (id_disciplina);
CREATE INDEX idx_avaliacao_data ON avaliacao (data_avaliacao);

-- Codigos de 6 digitos (verificacao de e-mail e recuperacao de senha); so o hash e guardado.
CREATE TABLE codigo_verificacao (
    id_codigo       UUID         NOT NULL,
    id_usuario      UUID         NOT NULL,
    tipo            VARCHAR(255) NOT NULL,
    codigo_hash     VARCHAR(255) NOT NULL,
    data_expiracao  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    usado           BOOLEAN      NOT NULL DEFAULT FALSE,
    tentativas      INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_codigo_verificacao PRIMARY KEY (id_codigo),
    CONSTRAINT fk_codigo_verificacao_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
CREATE INDEX idx_codigo_verificacao_usuario ON codigo_verificacao (id_usuario);
CREATE INDEX idx_codigo_verificacao_expiracao ON codigo_verificacao (data_expiracao);

-- Lista de revogacao de JWT (logout).
CREATE TABLE token_revogado (
    jti             VARCHAR(255) NOT NULL,
    data_expiracao  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_token_revogado PRIMARY KEY (jti)
);
CREATE INDEX idx_token_revogado_expiracao ON token_revogado (data_expiracao);

-- LEGADO: a entidade TokenRecuperacaoSenha continua no codigo (sem uso; foi substituida por
-- codigo_verificacao). A tabela existe so para o Hibernate (validate) nao falhar enquanto a classe existir.
-- Ao apagar a classe, crie uma migration V2 que remova esta tabela.
CREATE TABLE token_recuperacao_senha (
    id_token        UUID         NOT NULL,
    id_usuario      UUID,
    token           VARCHAR(255),
    data_expiracao  TIMESTAMP(6) WITH TIME ZONE,
    usado           BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_token_recuperacao_senha PRIMARY KEY (id_token),
    CONSTRAINT fk_token_recuperacao_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);
