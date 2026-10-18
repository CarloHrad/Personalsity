package com.example.trinots.dto.CursoDTO;

import jakarta.validation.constraints.*;

public record CursoRequestDTO(
    @NotBlank(message = "Nome do curso é obrigatório")
    @Size(min = 2, max = 150, message = "Nome do curso deve ter entre 2 e 150 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'’()&/-]*$", message = "Curso contém caracteres inválidos")
    String nomeCurso,

    @NotBlank(message = "Instituição é obrigatória")
    @Size(min = 2, max = 150, message = "Instituição deve ter entre 2 e 150 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'’()&/-]*$", message = "Instituição contém caracteres inválidos")
    String instituicao,

    @NotNull(message = "Duração é obrigatória")
    @Min(value = 1, message = "Duração deve ser no mínimo 1 semestre")
    @Max(value = 20, message = "Duração deve ser no máximo 20 semestres")
    Integer duracao
) {}