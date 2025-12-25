package project.airbnb.clone.entity.accommodation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.airbnb.clone.dto.accommodation.AccommodationProcessorDto;
import project.airbnb.clone.entity.BaseEntity;
import project.airbnb.clone.entity.area.SigunguCode;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "accommodations")
public class Accommodation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodation_id", nullable = false)
    private Long id;

    @Column(name = "map_x", nullable = false)
    private Double mapX;

    @Column(name = "map_y", nullable = false)
    private Double mapY;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "content_id", unique = true, nullable = false)
    private String contentId;

    @Column(name = "modified_time", nullable = false)
    private LocalDateTime modifiedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sigungu_code", nullable = false)
    private SigunguCode sigunguCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_people")
    private Integer maxPeople;

    @Column(name = "check_in")
    private String checkIn;

    @Column(name = "check_out")
    private String checkOut;

    @Column(name = "number")
    private String number;

    @Column(name = "refund_regulation", columnDefinition = "TEXT")
    private String refundRegulation;

    @Column(name = "is_embedded")
    private Boolean isEmbedded;

    @Column(name = "reservation_count")
    private int reservationCount;

    @Column(name = "average_rating")
    private double averageRating;

    public static Accommodation createEmpty() {
        return new Accommodation();
    }

    public void updateOrInit(AccommodationProcessorDto dto, SigunguCode sigunguCode) {
        this.mapX = dto.getMapX();
        this.mapY = dto.getMapY();
        this.description = dto.getDescription();
        this.address = dto.getAddress();
        this.maxPeople = dto.getMaxPeople();
        this.title = dto.getTitle();
        this.checkIn = dto.getCheckIn();
        this.checkOut = dto.getCheckOut();
        this.number = dto.getNumber();
        this.refundRegulation = dto.getRefundRegulation();
        this.modifiedTime = dto.getModifiedTime();
        this.contentId = dto.getContentId();
        this.sigunguCode = sigunguCode;
    }

    private Accommodation(Double mapX, Double mapY, String title, String address, String contentId, LocalDateTime modifiedTime, SigunguCode sigunguCode) {
        this.mapX = mapX;
        this.mapY = mapY;
        this.title = title;
        this.address = address;
        this.contentId = contentId;
        this.modifiedTime = modifiedTime;
        this.sigunguCode = sigunguCode;
    }
}