package com.azevedo.notification_service.business.dto;


import com.azevedo.notification_service.business.enums.StatusNotificacaoEnum;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TarefasDTO {

    private String id;
    private String nomeTarefa;
    private String descricao;
    private Instant dataCriacao;
    private Instant dataEvento;
    private String emailUsuario;
    private Instant dataAlteracao;
    private StatusNotificacaoEnum status;
}
