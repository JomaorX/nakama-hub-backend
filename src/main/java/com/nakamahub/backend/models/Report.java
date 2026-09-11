package com.nakamahub.backend.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Denuncia de un contenido o de una cuenta.
 *
 * El Reglamento de Servicios Digitales obliga a cualquier servicio que aloje
 * contenido de terceros a ofrecer un canal para reportar material ilegal, con
 * independencia de su tamaño.
 *
 * Se guarda una copia del contenido denunciado en el momento del reporte. Si no,
 * editarlo o borrarlo destruiría la prueba antes de que nadie la revise.
 */
@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_status", columnList = "status"),
        @Index(name = "idx_reports_target", columnList = "targetType,targetId")
})
@NoArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    /**
     * Sin cascada a propósito: si la cuenta que denunció se da de baja, el reporte
     * sigue siendo válido para el historial de moderación, ya anonimizado.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 1000)
    private String details;

    /** Copia del contenido denunciado, tal como estaba al reportarlo. */
    @Column(length = 500)
    private String reportedExcerpt;

    private String reportedAuthorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ToString.Include
    private ReportStatus status = ReportStatus.PENDIENTE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(length = 1000)
    private String moderatorNote;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Report report)) {
            return false;
        }
        return id != null && id.equals(report.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
