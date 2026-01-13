package project.airbnb.clone.dto.accommodation;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ViewHistoryDto(
        LocalDateTime viewDate,
        Long accommodationId,
        String title,
        double avgRate,
        String thumbnailUrl,
        boolean isInWishlist,
        Long wishlistId,
        String wishlistName) {
}
