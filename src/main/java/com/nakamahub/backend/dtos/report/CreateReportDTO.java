package com.nakamahub.backend.dtos.report;

import com.nakamahub.backend.models.ReportReason;
import com.nakamahub.backend.models.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportDTO {

    @NotNull(message = "Hay que indicar qué se está reportando")
    private ReportTargetType targetType;

    @NotNull(message = "Hay que indicar el identificador del contenido reportado")
    private Long targetId;

    @NotNull(message = "El motivo es obligatorio")
    private ReportReason reason;

    @Size(max = 1000, message = "La explicación no puede superar los 1000 caracteres")
    private String details;
}
