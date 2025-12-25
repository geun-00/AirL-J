package project.airbnb.clone.service.member;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.consts.SocialType;
import project.airbnb.clone.dto.member.DefaultProfileResDto;
import project.airbnb.clone.dto.member.SignupRequestDto;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.model.ProviderUser;
import project.airbnb.clone.repository.jpa.MemberRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberServiceTest extends TestContainerSupport {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("OAuth 가입 성공")
    void oauthRegister() {
        //given
        ProviderUser providerUser = createProviderUser("google");

        //when
        memberService.register(providerUser);

        //then
        assertThat(memberRepository.existsByEmailAndSocialType(providerUser.getEmail(), SocialType.from(providerUser.getProvider()))).isTrue();

        Member member = memberRepository.findByEmail(providerUser.getEmail()).orElse(null);

        assertThat(member).isNotNull();
        assertThat(member.getPassword()).isNotEqualTo(providerUser.getPassword());
        assertThat(passwordEncoder.matches(providerUser.getPassword(), member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("OAuth 중복 가입 시도 시 저장되지 않는다.")
    void oauthRegister_duplicate() {
        //given
        ProviderUser providerUser = createProviderUser("google");
        memberService.register(providerUser);

        long first = memberRepository.count();

        //when
        memberService.register(providerUser);

        long second = memberRepository.count();

        //then
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("OAuth 중복 이메일 가입 시도 시 예외가 발생한다.")
    void oauthRegister_throws() {
        //given
        ProviderUser googleUser = createProviderUser("google");
        memberService.register(googleUser);

        ProviderUser kakaoUser = createProviderUser("kakao");

        //when
        //then
        assertThatThrownBy(() -> memberService.register(kakaoUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다");
    }

    @Test
    @DisplayName("REST 가입 성공")
    void restRegister() {
        //given
        SignupRequestDto requestDto = createRequestDto();

        //when
        memberService.register(requestDto);

        //then
        Member member = memberRepository.findByEmail(requestDto.email()).orElse(null);

        assertThat(member).isNotNull();
        assertThat(member.getPassword()).isNotEqualTo(requestDto.password());
        assertThat(passwordEncoder.matches(requestDto.password(), member.getPassword())).isTrue();
    }

    @Test
    @DisplayName("REST 중복 이메일 가입 시도 시 예외가 발생한다.")
    void restRegister_throws() {
        //given
        SignupRequestDto requestDto = createRequestDto();
        memberService.register(requestDto);

        //when
        //then
        assertThatThrownBy(() -> memberService.register(requestDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다");
    }

    @Test
    @DisplayName("사용자 기본 정보 조회")
    void getDefaultProfile() {
        //given
        Member member = saveAndGetMember();

        //when
        DefaultProfileResDto result = memberService.getDefaultProfile(member.getId());

        //then
        assertThat(result.name()).isEqualTo(member.getName());
        assertThat(result.profileImageUrl()).isEqualTo(member.getProfileUrl());
        assertThat(result.createdDate()).isEqualTo(member.getCreatedAt().toLocalDate());
        assertThat(result.aboutMe()).isEqualTo(member.getAboutMe());
    }

    private Member saveAndGetMember() {
        return memberRepository.save(MemberFixture.create());
    }

    private SignupRequestDto createRequestDto() {
        return new SignupRequestDto("Kamal Usman", "test@email.com", "01011223344", LocalDate.of(2002, 1, 1), "password");
    }

    private ProviderUser createProviderUser(String provider) {
        return new ProviderUser() {
            @Override
            public String getUsername() {
                return "Zin Xing";
            }

            @Override
            public String getPassword() {
                return "cddf211e-ec73-44e6-bb79-479565559089";
            }

            @Override
            public String getEmail() {
                return "test@email.com";
            }

            @Override
            public String getImageUrl() {
                return "186.50.16.55";
            }

            @Override
            public String getProvider() {
                return provider;
            }

            @Override
            public List<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority("ROLE_USER"));
            }
        };
    }
}