package project.airbnb.clone.service.jwt;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import project.airbnb.clone.TestContainerSupport;
import project.airbnb.clone.common.exceptions.BusinessException;
import project.airbnb.clone.common.jwt.JwtProvider;
import project.airbnb.clone.dto.jwt.TokenResponse;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.repository.dto.redis.RefreshToken;
import project.airbnb.clone.repository.jpa.MemberRepository;
import project.airbnb.clone.repository.redis.BlacklistedTokenRepository;
import project.airbnb.clone.repository.redis.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest extends TestContainerSupport {

    @Autowired JwtProvider jwtProvider;
    @Autowired TokenService tokenService;
    @Autowired MemberRepository memberRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired BlacklistedTokenRepository blacklistedTokenRepository;

    Member member;

    @BeforeEach
    void setUp() {
        member = MemberFixture.create();
        memberRepository.saveAndFlush(member);
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll();
        blacklistedTokenRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("토큰 생성 후 액세스 토큰은 헤더로 전달되고, 리프레시 토큰은 쿠키 전달과 함께 레디스에 저장된다.")
    void generateAndSendToken() {
        //given
        String email = member.getEmail();
        String principalName = "principal";
        MockHttpServletResponse response = new MockHttpServletResponse();

        //when
        TokenResponse tokenResponse = tokenService.generateAndSendToken(email, principalName, response);

        //then
        assertThat(tokenResponse).isNotNull();

        String accessToken = tokenResponse.accessToken();
        String refreshToken = tokenResponse.refreshToken();

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        RefreshToken savedRefreshToken = refreshTokenRepository.findById(refreshToken)
                                                               .orElseThrow(() -> new AssertionError("RefreshToken이 Redis에 저장되지 않았습니다."));
        assertThat(savedRefreshToken.getToken()).isEqualTo(refreshToken);
        assertThat(savedRefreshToken.getMemberId()).isEqualTo(member.getId());
        assertThat(savedRefreshToken.getTtl()).isGreaterThan(0L);

        String authHeader = response.getHeader("Authorization");
        assertThat(authHeader).isEqualTo("Bearer " + accessToken);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .contains("RefreshToken=" + refreshToken)
                .contains("HttpOnly")
                .contains("Path=/")
                .contains("SameSite=None")
                .contains("Secure");
    }

    @Nested
    @DisplayName("액세스 토큰 갱신")
    class refreshAccessToken {

        @Test
        @DisplayName("성공 - 레디스에 저장된 값과 일치하는 경우")
        void success() {
            //given
            String principalName = "a6025936-1554-4f45-8601-a107576eb9d8";
            String oldRefreshToken = jwtProvider.generateRefreshToken(member, principalName);

            refreshTokenRepository.save(RefreshToken.builder()
                                                    .token(oldRefreshToken)
                                                    .memberId(member.getId())
                                                    .ttl(600L)
                                                    .build());

            MockHttpServletResponse response = new MockHttpServletResponse();

            //when
            tokenService.refreshAccessToken(oldRefreshToken, response);

            //then
            //정상적으로 액세스 토큰과 리프레시 토큰이 전달된다.
            String authHeader = response.getHeader("Authorization");
            assertThat(authHeader).isNotBlank().startsWith("Bearer ");

            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie)
                    .isNotBlank()
                    .contains("RefreshToken=")
                    .contains("HttpOnly")
                    .contains("Path=/");

            //새로운 리프레시 토큰이 Redis에 저장되었는지 확인
            String newRefreshToken = extractRefreshTokenFromCookie(setCookie);
            RefreshToken newToken = refreshTokenRepository.findById(newRefreshToken).orElse(null);
            assertThat(newToken).isNotNull();
            assertThat(newToken.getMemberId()).isEqualTo(member.getId());

            //기존 리프레시 토큰이 Redis에서 삭제되었는지 확인
            assertThat(refreshTokenRepository.existsById(oldRefreshToken)).isFalse();
        }

        @Test
        @DisplayName("실패 - 레디스에 저장된 값과 일치하지 않는 경우")
        void fail() {
            //given
            String token = jwtProvider.generateRefreshToken(member, "5e21afe5-3e80-4f9d-9f37-a41d309dcca9");
            refreshTokenRepository.save(RefreshToken.builder()
                                                    .token("other-wrong-token")
                                                    .memberId(member.getId())
                                                    .ttl(600L)
                                                    .build());

            MockHttpServletResponse response = new MockHttpServletResponse();

            //when
            assertThatThrownBy(() -> tokenService.refreshAccessToken(token, response))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("유효하지 않은 토큰입니다");
        }

        /**
         * Set-Cookie 헤더에서 RefreshToken 값을 추출하는 헬퍼 메서드
         */
        private String extractRefreshTokenFromCookie(String setCookie) {
            String prefix = "RefreshToken=";
            int start = setCookie.indexOf(prefix) + prefix.length();
            int end = setCookie.indexOf(";", start);
            if (end == -1) {
                end = setCookie.length();
            }
            return setCookie.substring(start, end);
        }
    }

    @Test
    @DisplayName("로그아웃 처리 시 액세스 토큰 블랙리스트 추가, 리프레시 토큰이 삭제된다.")
    void logoutProcess() {
        // given
        String principalName = "principal";
        String accessToken = "Bearer " + jwtProvider.generateAccessToken(member, principalName);
        String refreshToken = jwtProvider.generateRefreshToken(member, principalName);

        // Redis에 refresh token 저장
        refreshTokenRepository.save(RefreshToken.builder()
                                                .token(refreshToken)
                                                .memberId(member.getId())
                                                .ttl(600L)
                                                .build());

        // when
        tokenService.logoutProcess(accessToken, refreshToken);

        // then
        // 1. 액세스 토큰이 블랙리스트에 추가됐는지 확인
        boolean isBlackListed = tokenService.containsBlackList(accessToken.substring("Bearer ".length()));
        assertThat(isBlackListed).isTrue();

        // 2. 리프레시 토큰이 삭제됐는지 확인
        assertThat(refreshTokenRepository.existsById(refreshToken)).isFalse();
    }
}