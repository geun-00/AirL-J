package project.airbnb.clone.dto.accommodation;

import project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.DetailReviewDto;
import project.airbnb.clone.repository.dto.DetailAccommodationQueryDto;

import java.util.List;

import static project.airbnb.clone.dto.accommodation.AccommodationCommonInfo.*;

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
        List<DetailReviewDto> reviews) {

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

    public static DetailAccommodationResDto from(DetailAccommodationQueryDto queryDto,
                                                 DetailImageDto imageDto,
                                                 List<String> amenities,
                                                 List<DetailReviewDto> reviewDtos) {
        return new DetailAccommodationResDto(
                queryDto.accommodationId(),
                queryDto.title(),
                queryDto.maxPeople(),
                queryDto.address(),
                queryDto.mapX(),
                queryDto.mapY(),
                queryDto.checkIn(),
                queryDto.checkOut(),
                queryDto.description(),
                queryDto.number(),
                queryDto.refundRegulation(),
                queryDto.price(),
                queryDto.isInWishlist(),
                queryDto.wishlistId(),
                queryDto.wishlistName(),
                queryDto.avgRate(),
                imageDto,
                amenities,
                reviewDtos
        );
    }

    public static DetailAccommodationResDto from(AccommodationCommonInfo commonInfo, WishlistInfo wishlistInfo) {
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
                commonInfo.getReview()
        );
    }
}
