package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.report.CreateReportDTO;
import com.nakamahub.backend.dtos.report.ReportResponseDTO;
import com.nakamahub.backend.dtos.report.ResolveReportDTO;
import com.nakamahub.backend.models.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReportService {

    ReportResponseDTO createReport(CreateReportDTO createReportDTO, String reporterUsername);

    /** @param status filtro opcional; null devuelve la cola entera. */
    Page<ReportResponseDTO> listReports(ReportStatus status, Pageable pageable);

    ReportResponseDTO resolveReport(Long id, ResolveReportDTO dto, String moderatorUsername);
}
