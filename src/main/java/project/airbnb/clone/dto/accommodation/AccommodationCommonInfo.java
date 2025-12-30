package project.airbnb.clone.dto.accommodation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import project.airbnb.clone.repository.dto.DetailAccommodationQueryDto;
import project.airbnb.clone.repository.dto.ImageDataQueryDto;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationCommonInfo {

    private Long accommodationId;
    private String title;
    private int maxPeople;
    private String address;
    private double mapX;
    private double mapY;
    private String checkIn;
    private String checkOut;
    private String description;
    private String number;
    private String refundRegulation;
    private int price;
    private boolean isInWishlist;
    private Long wishlistId;
    private String wishlistName;
    private Double avgRate;
    private DetailImageDto images;
    private List<String> amenities;
    private List<DetailReviewDto> review;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailReviewDto {

        private Long memberId;
        private String memberName;
        private String profileUrl;
        private LocalDateTime memberCreatedDate;
        private LocalDateTime reviewCreatedDate;
        private double rating;
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailImageDto {

        private String thumbnail;
        private List<String> others;
    }

    public static AccommodationCommonInfo from(DetailAccommodationQueryDto detail, List<String> amenities, List<DetailReviewDto> reviews, List<ImageDataQueryDto> images) {
        String thumbnail = images.stream()
                                 .filter(ImageDataQueryDto::isThumbnail)
                                 .map(ImageDataQueryDto::imageUrl)
                                 .findFirst()
                                 .orElse(null);
        List<String> others = images.stream()
                                    .filter(dto -> !dto.isThumbnail())
                                    .map(ImageDataQueryDto::imageUrl)
                                    .toList();
        DetailImageDto detailImageDto = new DetailImageDto(thumbnail, others);

        return new AccommodationCommonInfo(
                detail.accommodationId(),
                detail.title(),
                detail.maxPeople(),
                detail.address(),
                detail.mapX(),
                detail.mapY(),
                detail.checkIn(),
                detail.checkOut(),
                detail.description(),
                detail.number(),
                detail.refundRegulation(),
                detail.price(),
                detail.isInWishlist(),
                detail.wishlistId(),
                detail.wishlistName(),
                detail.avgRate(),
                detailImageDto,
                amenities,
                reviews
        );
    }
}
