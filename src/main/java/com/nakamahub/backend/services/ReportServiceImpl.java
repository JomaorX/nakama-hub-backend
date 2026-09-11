package com.nakamahub.backend.services;

import com.nakamahub.backend.dtos.report.CreateReportDTO;
import com.nakamahub.backend.dtos.report.ReportResponseDTO;
import com.nakamahub.backend.dtos.report.ResolveReportDTO;
import com.nakamahub.backend.models.*;
import com.nakamahub.backend.repositories.CommentRepository;
import com.nakamahub.backend.repositories.PostRepository;
import com.nakamahub.backend.repositories.ReportRepository;
import com.nakamahub.backend.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    private static final int EXCERPT_LENGTH = 500;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             PostRepository postRepository,
                             CommentRepository commentRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public ReportResponseDTO createReport(CreateReportDTO dto, String reporterUsername) {
        User reporter = userRepository.findByUsername(reporterUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(dto.getTargetType());
        report.setTargetId(dto.getTargetId());
        report.setReason(dto.getReason());
        report.setDetails(dto.getDetails());

        snapshotTarget(report, reporter);

        if (reportRepository.existsByReporterAndTargetTypeAndTargetIdAndStatus(
                reporter, dto.getTargetType(), dto.getTargetId(), ReportStatus.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya has reportado esto y todavía está pendiente de revisión");
        }

        return toDTO(reportRepository.save(report));
    }

    /**
     * Comprueba que lo denunciado existe y guarda una copia del contenido. Sin esta
     * copia, borrar o editar el contenido dejaría al moderador sin nada que juzgar.
     */
    private void snapshotTarget(Report report, User reporter) {
        switch (report.getTargetType()) {
            case POST -> {
                Post post = postRepository.findWithAuthorById(report.getTargetId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "El post reportado no existe"));
                requireNotSelf(post.getAuthor(), reporter, "No tiene sentido reportar tu propio post");
                report.setReportedAuthorUsername(post.getAuthor().getUsername());
                report.setReportedExcerpt(excerpt(post.getTitle() + " — " + post.getContent()));
            }
            case COMMENT -> {
                Comment comment = commentRepository.findWithAuthorById(report.getTargetId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "El comentario reportado no existe"));
                requireNotSelf(comment.getAuthor(), reporter, "No tiene sentido reportar tu propio comentario");
                report.setReportedAuthorUsername(comment.getAuthor().getUsername());
                report.setReportedExcerpt(excerpt(comment.getContent()));
            }
            case USER -> {
                User target = userRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "El usuario reportado no existe"));
                requireNotSelf(target, reporter, "No puedes reportarte a ti mismo");
                report.setReportedAuthorUsername(target.getUsername());
                report.setReportedExcerpt(excerpt(target.getBio()));
            }
        }
    }

    private void requireNotSelf(User target, User reporter, String message) {
        if (target.equals(reporter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String excerpt(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportResponseDTO> listReports(ReportStatus status, Pageable pageable) {
        Page<Report> reports = status == null
                ? reportRepository.findBy(pageable)
                : reportRepository.findByStatus(status, pageable);

        return reports.map(this::toDTO);
    }

    @Override
    public ReportResponseDTO resolveReport(Long id, ResolveReportDTO dto, String moderatorUsername) {
        if (dto.getStatus() == ReportStatus.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un reporte se cierra como RESUELTO o DESCARTADO");
        }

        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado"));

        if (report.getStatus() != ReportStatus.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este reporte ya estaba revisado");
        }

        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Moderador no encontrado"));

        report.setStatus(dto.getStatus());
        report.setModeratorNote(dto.getModeratorNote());
        report.setReviewedBy(moderator);
        report.setReviewedAt(LocalDateTime.now());

        return toDTO(report);
    }

    private ReportResponseDTO toDTO(Report report) {
        return ReportResponseDTO.builder()
                .id(report.getId())
                .reporterUsername(report.getReporter().getUsername())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .details(report.getDetails())
                .reportedExcerpt(report.getReportedExcerpt())
                .reportedAuthorUsername(report.getReportedAuthorUsername())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .reviewedByUsername(report.getReviewedBy() != null
                        ? report.getReviewedBy().getUsername() : null)
                .moderatorNote(report.getModeratorNote())
                .build();
    }
}
