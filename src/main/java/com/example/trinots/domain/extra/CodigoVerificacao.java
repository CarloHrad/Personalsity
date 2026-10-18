package com.example.trinots.domain.extra;

import com.example.trinots.domain.Usuario;
import com.example.trinots.domain.enums.TipoCodigoEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Código numérico de 6 dígitos enviado por e-mail, usado tanto para verificar o e-mail
 * quanto para recuperar a senha. Só o hash do código é persistido.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "codigo_verificacao")
public class CodigoVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idCodigo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCodigoEnum tipo;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    @Column(nullable = false)
    private boolean usado = false;

    @Column(nullable = false)
    private int tentativas = 0;
}
