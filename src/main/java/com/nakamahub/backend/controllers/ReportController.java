package com.nakamahub.backend.controllers;

import com.nakamahub.backend.dtos.report.CreateReportDTO;
import com.nakamahub.backend.dtos.report.ReportResponseDTO;
import com.nakamahub.backend.dtos.report.ResolveReportDTO;
import com.nakamahub.backend.models.ReportStatus;
import com.nakamahub.backend.security.SecurityUtils;
import com.nakamahub.backend.services.ReportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Abierto a cualquier usuario autenticado: es el canal de aviso que exige el DSA. */
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponseDTO createReport(@Valid @RequestBody CreateReportDTO body) {
        return reportService.createReport(body, SecurityUtils.requireCurrentUsername());
    }

    /** Cola de moderación. Por defecto muestra lo que queda por revisar. */
    @GetMapping("")
    @ResponseStatus(HttpStatus.OK)
    public Page<ReportResponseDTO> listReports(
            @RequestParam(required = false, defaultValue = "PENDIENTE") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return reportService.listReports(status,
                PageRequest.of(Math.max(0, page), Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by("createdAt").ascending()));
    }

    @PutMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.OK)
    public ReportResponseDTO resolveReport(@PathVariable Long id, @Valid @RequestBody ResolveReportDTO body) {
        return reportService.resolveReport(id, body, SecurityUtils.requireCurrentUsername());
    }
}
