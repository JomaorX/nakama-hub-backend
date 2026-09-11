package com.nakamahub.backend.models;

public enum ReportStatus {
    /** A la espera de que lo revise un moderador. */
    PENDIENTE,
    /** Revisado y se actuó sobre el contenido o la cuenta. */
    RESUELTO,
    /** Revisado y se descartó. */
    DESCARTADO
}
