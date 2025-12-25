package project.airbnb.clone.service.accommodation;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.dto.wishlist.AddAccToWishlistReqDto;
import project.airbnb.clone.dto.wishlist.MemoUpdateReqDto;
import project.airbnb.clone.dto.wishlist.WishlistCreateReqDto;
import project.airbnb.clone.dto.wishlist.WishlistCreateResDto;
import project.airbnb.clone.dto.wishlist.WishlistDetailResDto;
import project.airbnb.clone.dto.wishlist.WishlistUpdateReqDto;
import project.airbnb.clone.dto.wishlist.WishlistsResDto;
import project.airbnb.clone.entity.accommodation.Accommodation;
import project.airbnb.clone.entity.accommodation.AccommodationImage;
import project.airbnb.clone.entity.area.AreaCode;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.entity.area.SigunguCode;
import project.airbnb.clone.entity.wishlist.Wishlist;
import project.airbnb.clone.entity.wishlist.WishlistAccommodation;
import project.airbnb.clone.fixtures.AccommodationFixture;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.repository.jpa.WishlistAccommodationRepository;
import project.airbnb.clone.repository.jpa.WishlistRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WishlistServiceTest extends TestContainerSupport {

    @Autowired EntityManager em;
    @Autowired WishlistService wishlistService;
    @Autowired WishlistRepository wishlistRepository;
    @Autowired WishlistAccommodationRepository wishlistAccommodationRepository;

    Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.create();
        em.persist(member);
    }

    @Test
    @DisplayName("위시리시트 요청 DTO를 받아서 Wishlist 데이터를 저장한다.")
    void createWishlist() {
        //given
        WishlistCreateReqDto reqDto = new WishlistCreateReqDto("my-wishlist");

        //when
        WishlistCreateResDto resDto = wishlistService.createWishlist(reqDto, member.getId());

        em.flush();
        em.clear();

        //then
        Wishlist wishlist = wishlistRepository.findById(resDto.wishlistId()).get();
        assertThat(wishlist).isNotNull();
        assertThat(resDto.wishlistId()).isEqualTo(wishlist.getId());
        assertThat(resDto.wishlistName()).isEqualTo(wishlist.getName());
        assertThat(wishlist.getMember().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("위시리스트에 숙소를 추가한다.")
    void addAccommodationToWishlist() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        Accommodation accommodation = saveAndGetAccommodation();
        AddAccToWishlistReqDto reqDto = new AddAccToWishlistReqDto(accommodation.getId());

        //when
        wishlistService.addAccommodationToWishlist(wishlist.getId(), reqDto, member.getId());
        em.flush();
        em.clear();

        //then
        List<WishlistAccommodation> savedList = wishlistAccommodationRepository.findAll();

        assertThat(savedList).hasSize(1);

        WishlistAccommodation result = savedList.get(0);
        assertThat(result.getWishlist().getId()).isEqualTo(wishlist.getId());
        assertThat(result.getAccommodation().getId()).isEqualTo(accommodation.getId());
    }

    @Test
    @DisplayName("위시리스트에서 숙소를 제거한다.")
    void removeAccommodationFromWishlist() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        Accommodation accommodation = saveAndGetAccommodation();

        //when
        wishlistService.removeAccommodationFromWishlist(wishlist.getId(), accommodation.getId(), member.getId());
        em.flush();
        em.clear();

        //then
        List<WishlistAccommodation> result = em.createQuery("SELECT wa FROM WishlistAccommodation wa WHERE wa.accommodation.id = :accommodationId AND wa.wishlist.id = :wishlistId", WishlistAccommodation.class)
                                               .setParameter("accommodationId", accommodation.getId())
                                               .setParameter("wishlistId", wishlist.getId())
                                               .getResultList();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("위시리스트의 이름을 변경한다.")
    void updateWishlistName() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        WishlistUpdateReqDto reqDto = new WishlistUpdateReqDto("test-new-wishlist-name");

        //when
        wishlistService.updateWishlistName(wishlist.getId(), reqDto, member.getId());
        em.flush();
        em.clear();

        //then
        Wishlist result = wishlistRepository.findById(wishlist.getId()).get();
        assertThat(result.getName()).isEqualTo(reqDto.wishlistName());
    }

    @Test
    @DisplayName("특정 위시리스트를 삭제한다.")
    void removeWishlist() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        Accommodation acc1 = saveAndGetAccommodation();
        Accommodation acc2 = saveAndGetAccommodation();

        WishlistAccommodation wa1 = saveAndGetWishlistAccommodation(wishlist, acc1);
        WishlistAccommodation wa2 = saveAndGetWishlistAccommodation(wishlist, acc2);

        //when
        wishlistService.removeWishlist(wishlist.getId(), member.getId());
        em.flush();
        em.clear();

        //then
        WishlistAccommodation waResult1 = wishlistAccommodationRepository.findById(wa1.getId()).orElse(null);
        WishlistAccommodation waResult2 = wishlistAccommodationRepository.findById(wa2.getId()).orElse(null);
        assertThat(waResult1).isNull();
        assertThat(waResult2).isNull();

        Wishlist result = wishlistRepository.findById(wishlist.getId()).orElse(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("특정 위시리스트에 있는 숙소 목록을 조회한다.")
    void getAccommodationsFromWishlist() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        Accommodation acc1 = saveAndGetAccommodation();
        Accommodation acc2 = saveAndGetAccommodation();

        saveAccommodationImage(AccommodationImage.normalOf(acc1, "https://image1.com"));
        saveAccommodationImage(AccommodationImage.normalOf(acc1, "https://image2.com"));
        saveAccommodationImage(AccommodationImage.normalOf(acc2, "https://image3.com"));

        saveAndGetWishlistAccommodation(wishlist, acc1);
        saveAndGetWishlistAccommodation(wishlist, acc2);

        //when
        List<WishlistDetailResDto> result = wishlistService.getAccommodationsFromWishlist(wishlist.getId(), member.getId());
        em.flush();
        em.clear();

        //then
        assertThat(result).hasSize(2);

        WishlistDetailResDto dto1 = result.stream()
                                          .filter(dto -> dto.accommodationId().equals(acc1.getId()))
                                          .findFirst()
                                          .orElseThrow();
        assertThat(dto1.accommodationId()).isEqualTo(acc1.getId());
        assertThat(dto1.title()).isEqualTo(acc1.getTitle());
        assertThat(dto1.mapX()).isEqualTo(acc1.getMapX());
        assertThat(dto1.mapY()).isEqualTo(acc1.getMapY());
        assertThat(dto1.imageUrls()).hasSize(2).containsExactlyInAnyOrder("https://image1.com", "https://image2.com");

        WishlistDetailResDto dto2 = result.stream()
                                          .filter(dto -> dto.accommodationId().equals(acc2.getId()))
                                          .findFirst()
                                          .orElseThrow();
        assertThat(dto2.accommodationId()).isEqualTo(acc2.getId());
        assertThat(dto2.title()).isEqualTo(acc2.getTitle());
        assertThat(dto2.mapX()).isEqualTo(acc2.getMapX());
        assertThat(dto2.mapY()).isEqualTo(acc2.getMapY());
        assertThat(dto2.imageUrls()).hasSize(1).containsExactlyInAnyOrder("https://image3.com");
    }

    @Test
    @DisplayName("위시리스트 내에 있는 숙소에 메모를 수정(저장)한다.")
    void updateMemo() {
        //given
        Wishlist wishlist = savedAndGetWishlist();
        Accommodation acc = saveAndGetAccommodation();
        saveAndGetWishlistAccommodation(wishlist, acc);
        MemoUpdateReqDto reqDto = new MemoUpdateReqDto("new-memo");

        //when
        wishlistService.updateMemo(wishlist.getId(), acc.getId(), member.getId(), reqDto);
        em.flush();
        em.clear();

        //then
        WishlistAccommodation result = wishlistAccommodationRepository.findByAllIds(wishlist.getId(), acc.getId(), member.getId()).get();
        assertThat(result.getMemo()).isEqualTo(reqDto.memo());
    }

    @Test
    @DisplayName("위시리스트 전체 목록을 조회한다.")
    void getAllWishlists() {
        //given
        Wishlist wishlist1 = savedAndGetWishlist();
        Wishlist wishlist2 = savedAndGetWishlist();

        Accommodation acc1 = saveAndGetAccommodation();
        Accommodation acc2 = saveAndGetAccommodation();

        saveAccommodationImage(AccommodationImage.normalOf( acc1, "https://image1.com"));
        saveAccommodationImage(AccommodationImage.thumbnailOf(acc1, "https://image2.com"));
        saveAccommodationImage(AccommodationImage.thumbnailOf(acc2, "https://image3.com"));

        saveAndGetWishlistAccommodation(wishlist1, acc1);
        saveAndGetWishlistAccommodation(wishlist2, acc2);

        //when
        List<WishlistsResDto> result = wishlistService.getAllWishlists(member.getId());
        em.flush();
        em.clear();

        //then
        assertThat(result).hasSize(2);

        assertThat(result.get(0).wishlistId()).isEqualTo(wishlist1.getId());
        assertThat(result.get(0).name()).isEqualTo(wishlist1.getName());
        assertThat(result.get(0).thumbnailUrl()).isEqualTo("https://image2.com");
        assertThat(result.get(0).savedAccommodations()).isEqualTo(1);

        assertThat(result.get(1).wishlistId()).isEqualTo(wishlist2.getId());
        assertThat(result.get(1).name()).isEqualTo(wishlist2.getName());
        assertThat(result.get(1).thumbnailUrl()).isEqualTo("https://image3.com");
        assertThat(result.get(1).savedAccommodations()).isEqualTo(1);
    }

    private void saveAccommodationImage(AccommodationImage accommodationImage) {
        em.persist(accommodationImage);
    }

    private WishlistAccommodation saveAndGetWishlistAccommodation(Wishlist wishlist, Accommodation acc) {
        return wishlistAccommodationRepository.save(WishlistAccommodation.create(wishlist, acc));
    }

    private Wishlist savedAndGetWishlist() {
        return wishlistRepository.save(Wishlist.create(member, "test-wishlist"));
    }

    private Accommodation saveAndGetAccommodation() {
        AreaCode areaCode = AreaCode.create(UUID.randomUUID().toString(), "test-codeName");
        SigunguCode sigunguCode = SigunguCode.create(UUID.randomUUID().toString(), "test-codeName", areaCode);
        em.persist(areaCode);
        em.persist(sigunguCode);

        Accommodation accommodation = AccommodationFixture.create("test-title", sigunguCode, 1.0,1.0);
        em.persist(accommodation);

        return accommodation;
    }
}