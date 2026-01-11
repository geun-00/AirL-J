package project.airbnb.clone.fixtures;

import project.airbnb.clone.dto.accommodation.AccommodationProcessorDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.accommodation.AccommodationImage;
import project.airbnb.clone.entity.area.SigunguCode;

import java.util.UUID;

public class AccommodationFixture {

    public static Accommodation create(String title, SigunguCode sigunguCode, double mapX, double mapY) {
        Accommodation accommodation = Accommodation.createEmpty();
        AccommodationProcessorDto dto = new AccommodationProcessorDto(UUID.randomUUID().toString(), "20300101150000");
        dto.setMapX(mapX);
        dto.setMapY(mapY);
        dto.setTitle(title);
        dto.setAddress("주소");

        accommodation.updateOrInit(dto, sigunguCode);
        return accommodation;
    }

    public static AccommodationImage createThumbnail(Accommodation accommodation) {
        return AccommodationImage.thumbnailOf(accommodation, "https://test-image.com");
    }
}
