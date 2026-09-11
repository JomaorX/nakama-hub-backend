package com.nakamahub.backend.repositories;

import com.nakamahub.backend.models.Report;
import com.nakamahub.backend.models.ReportStatus;
import com.nakamahub.backend.models.ReportTargetType;
import com.nakamahub.backend.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @EntityGraph(attributePaths = {"reporter", "reviewedBy"})
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "reviewedBy"})
    Page<Report> findBy(Pageable pageable);

    /** Evita que una misma persona inunde la cola con el mismo reporte. */
    boolean existsByReporterAndTargetTypeAndTargetIdAndStatus(
            User reporter, ReportTargetType targetType, Long targetId, ReportStatus status);
}
