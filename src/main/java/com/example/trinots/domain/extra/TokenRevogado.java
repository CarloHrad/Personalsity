package com.example.trinots.domain.extra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lista de revogação de JWT (logout). Guarda o "jti" do token até a data em que ele expiraria;
 * depois disso a linha pode ser apagada, pois o token já seria rejeitado pela expiração.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "token_revogado")
public class TokenRevogado {

    @Id
    private String jti;

    @Column(name = "data_expiracao", nullable = false)
    private Instant dataExpiracao;

    public TokenRevogado(String jti, Instant dataExpiracao) {
        this.jti = jti;
        this.dataExpiracao = dataExpiracao;
    }
}
