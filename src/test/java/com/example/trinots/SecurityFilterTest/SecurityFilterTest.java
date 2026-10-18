package com.example.trinots.SecurityFilterTest;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.trinots.domain.Usuario;
import com.example.trinots.repository.TokenRevogadoRepository;
import com.example.trinots.repository.UsuarioRepository;
import com.example.trinots.security.SecurityFilter;
import com.example.trinots.service.AccessTokenService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityFilter")
class SecurityFilterTest {

    private AccessTokenService accessTokenService;
    private UsuarioRepository usuarioRepository;
    private TokenRevogadoRepository tokenRevogadoRepository;
    private SecurityFilter filtro;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        accessTokenService = mock(AccessTokenService.class);
        usuarioRepository = mock(UsuarioRepository.class);
        tokenRevogadoRepository = mock(TokenRevogadoRepository.class);
        filtro = new SecurityFilter(accessTokenService, usuarioRepository, tokenRevogadoRepository);

        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer abc");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);

        usuario = new Usuario();
        usuario.setIdUsuario(UUID.randomUUID());
        usuario.setEmail("joao@email.com");
        usuario.setAtivo(true);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private DecodedJWT jwtComJti(String jti) {
        DecodedJWT jwt = mock(DecodedJWT.class);
        lenient().when(jwt.getId()).thenReturn(jti);
        lenient().when(jwt.getSubject()).thenReturn("joao@email.com");
        return jwt;
    }

    @Test
    @DisplayName("token válido, não revogado e de conta ativa autentica o usuário")
    void autenticaTokenValido() throws Exception {
        DecodedJWT jwt = jwtComJti("jti-1");
        when(accessTokenService.decodificar("abc")).thenReturn(jwt);
        when(tokenRevogadoRepository.existsById("jti-1")).thenReturn(false);
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(usuario);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("token revogado no logout não autentica, mesmo ainda dentro da validade")
    void tokenRevogadoNaoAutentica() throws Exception {
        DecodedJWT jwt = jwtComJti("jti-1");
        when(accessTokenService.decodificar("abc")).thenReturn(jwt);
        when(tokenRevogadoRepository.existsById("jti-1")).thenReturn(true);

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(usuarioRepository, never()).findByEmail(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("conta desativada não autentica, mesmo com token válido")
    void contaDesativadaNaoAutentica() throws Exception {
        usuario.setAtivo(false);
        DecodedJWT jwt = jwtComJti("jti-1");
        when(accessTokenService.decodificar("abc")).thenReturn(jwt);
        when(tokenRevogadoRepository.existsById("jti-1")).thenReturn(false);
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(usuario));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("token sem jti (não revogável) é rejeitado")
    void tokenSemJtiRejeitado() throws Exception {
        DecodedJWT jwt = mock(DecodedJWT.class);

        when(jwt.getId()).thenReturn(null);
        when(accessTokenService.decodificar("abc")).thenReturn(jwt);

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenRevogadoRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("token inválido ou expirado não autentica")
    void tokenInvalidoNaoAutentica() throws Exception {
        when(accessTokenService.decodificar("abc")).thenReturn(null);

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("requisição sem header Authorization segue sem autenticação")
    void semHeader() throws Exception {
        MockHttpServletRequest semHeader = new MockHttpServletRequest();

        filtro.doFilter(semHeader, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(accessTokenService);
        verify(chain).doFilter(semHeader, response);
    }
}
