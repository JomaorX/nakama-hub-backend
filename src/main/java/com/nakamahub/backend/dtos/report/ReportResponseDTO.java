package com.nakamahub.backend.dtos.report;

import com.nakamahub.backend.models.ReportReason;
import com.nakamahub.backend.models.ReportStatus;
import com.nakamahub.backend.models.ReportTargetType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReportResponseDTO(
        Long id,
        String reporterUsername,
        ReportTargetType targetType,
        Long targetId,
        ReportReason reason,
        String details,
        String reportedExcerpt,
        String reportedAuthorUsername,
        ReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt,
        String reviewedByUsername,
        String moderatorNote
) {
}
