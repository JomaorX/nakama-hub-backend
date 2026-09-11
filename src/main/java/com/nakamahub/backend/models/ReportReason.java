package com.nakamahub.backend.models;

/** Motivos de reporte. Conviene que sean pocos y claros: una lista larga hace que la gente elija al azar. */
public enum ReportReason {
    SPAM,
    ACOSO,
    DISCURSO_DE_ODIO,
    CONTENIDO_SEXUAL,
    VIOLENCIA,
    SPOILER_SIN_AVISO,
    SUPLANTACION,
    OTRO
}
