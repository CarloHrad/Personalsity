package com.example.trinots.dto.TarefaDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TarefaRequestDTO(
        @NotBlank(message = "Nome da tarefa é obrigatório")
        @Size(min = 2, max = 150, message = "Nome da tarefa deve ter entre 2 e 150 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'’()&/+:_\\-]*$", message = "Nome da tarefa contém caracteres inválidos")
        String nomeTarefa,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        @Pattern(regexp = "^[\\p{L}\\p{N}\\p{Zs}\\p{P}\\p{S}\\r\\n\\t]*$", message = "Descrição contém caracteres inválidos")
        String descricao,

        @NotNull(message = "Data de entrega é obrigatória")
        LocalDate dataEntrega,

        LocalTime horaEntrega,

        @NotNull(message = "Disciplina é obrigatória")
        UUID idDisciplina
) {}