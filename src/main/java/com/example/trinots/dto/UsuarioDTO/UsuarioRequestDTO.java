package com.example.trinots.dto.UsuarioDTO;

import com.example.trinots.dto.CursoDTO.CursoRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record UsuarioRequestDTO (
    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ'\\- ]+$", message = "Nome deve conter apenas letras")
    String nome,

    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(min = 2, max = 100, message = "Sobrenome deve ter entre 2 e 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÖØ-öø-ÿ'\\- ]+$", message = "Sobrenome deve conter apenas letras")
    String sobrenome,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    String email,

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 4, max = 32, message = "Senha deve ter entre 4 e 32 caracteres")
    String senha,

    @NotNull(message = "Curso é obrigatório")
    @Valid
    CursoRequestDTO curso,

    @NotNull(message = "Semestre é obrigatório")
    @Min(value = 1, message = "Semestre deve ser no mínimo 1")
    @Max(value = 20, message = "Semestre deve ser no máximo 20")
    Integer semestreAtual,

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    LocalDate dataNascimento
){}