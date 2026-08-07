package com.example.trinots.dto.TarefaDTO;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TarefaRequestDTO(
        @NotBlank(message = "Nome da tarefa é obrigatório")
        @Size(min = 2, max = 150, message = "Nome da tarefa deve ter entre 2 e 150 caracteres")
        String nomeTarefa,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
        String descricao,

        @NotNull(message = "Data de entrega é obrigatória") //faz sentido deixar opcional?
        LocalDate dataEntrega,

        @NotNull(message = "Disciplina é obrigatória")
        UUID idDisciplina
) {}