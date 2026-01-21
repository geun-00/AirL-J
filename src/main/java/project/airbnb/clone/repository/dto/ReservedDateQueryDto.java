package project.airbnb.clone.repository.dto;

import java.time.LocalDateTime;

public record ReservedDateQueryDto(LocalDateTime startDate, LocalDateTime endDate) {
}
