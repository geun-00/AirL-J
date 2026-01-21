package project.airbnb.clone.dto.accommodation;

import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.DetailReviewDto;
import project.airbnb.clone.repository.dto.ReservedDateQueryDto;

import java.time.LocalDate;
import java.util.List;

import static project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.DetailImageDto;

public record DetailAccommodationResDto(
        Long accommodationId,
        String title,
        int maxPeople,
        String address,
        double mapX,
        double mapY,
        String checkIn,
        String checkOut,
        String description,
        String number,
        String refundRegulation,

        int price,
        boolean isInWishlist,
        Long wishlistId,
        String wishlistName,
        double avgRate,
        DetailImageDto images,
        List<String> amenities,
        List<DetailReviewDto> reviews,
        List<ReservedDateDto> reservedDates
) {

    public record WishlistInfo(
            Long accommodationId,
            boolean isInWishlist,
            Long wishlistId,
            String wishlistName
    ) {
        public static WishlistInfo empty() {
            return new WishlistInfo(null, false, null, null);
        }
    }

    public record ReservedDateDto(LocalDate start, LocalDate end) { }

    public static DetailAccommodationResDto from(AccommodationCommonInfo commonInfo, WishlistInfo wishlistInfo, List<ReservedDateQueryDto> reservedDates) {
        return new DetailAccommodationResDto(
                commonInfo.getAccommodationId(),
                commonInfo.getTitle(),
                commonInfo.getMaxPeople(),
                commonInfo.getAddress(),
                commonInfo.getMapX(),
                commonInfo.getMapY(),
                commonInfo.getCheckIn(),
                commonInfo.getCheckOut(),
                commonInfo.getDescription(),
                commonInfo.getNumber(),
                commonInfo.getRefundRegulation(),
                commonInfo.getPrice(),
                wishlistInfo.isInWishlist(),
                wishlistInfo.wishlistId(),
                wishlistInfo.wishlistName(),
                commonInfo.getAvgRate(),
                commonInfo.getImages(),
                commonInfo.getAmenities(),
                commonInfo.getReview(),
                reservedDates.stream()
                             .map(date -> new ReservedDateDto(date.startDate().toLocalDate(), date.endDate().toLocalDate()))
                             .toList()
        );
    }
}
