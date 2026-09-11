package com.nakamahub.backend.dtos.report;

import com.nakamahub.backend.models.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReportDTO {

    /** RESUELTO si se actuó, DESCARTADO si no procedía. */
    @NotNull(message = "Hay que indicar cómo se cierra el reporte")
    private ReportStatus status;

    @Size(max = 1000, message = "La nota no puede superar los 1000 caracteres")
    private String moderatorNote;
}
