package project.airbnb.clone.entity.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.airbnb.clone.consts.Role;
import project.airbnb.clone.consts.SocialType;
import project.airbnb.clone.entity.BaseEntity;

import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "members")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "number", length = 11)
    private String number;

    @Column(name = "email", nullable = false, length = 50)
    private String email;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "about_me")
    private String aboutMe;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type")
    private SocialType socialType;

    @Column(name = "is_email_verified", nullable = false)
    private Boolean isEmailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.GUEST;

    public static Member createAdmin(String adminEmail, String password) {
        return new Member("ADMIN-USER", null, null, adminEmail, password, SocialType.NONE, Role.ADMIN, true);
    }

    public static Member createForRest(String name, String email, String number, LocalDate birthDate, String password) {
        return new Member(name, birthDate, number, email, password, SocialType.NONE, Role.GUEST, false);
    }

    public static Member createForSocial(String name, String email, String number, LocalDate birthDate, String password, SocialType socialType) {
        return new Member(name, birthDate, number, email, password, socialType, Role.GUEST, false);
    }

    private Member(String name, LocalDate birthDate, String number, String email, String password,
                   SocialType socialType, Role role, boolean isEmailVerified) {
        this.name = name;
        this.birthDate = birthDate;
        this.number = number;
        this.email = email;
        this.password = password;
        this.socialType = socialType;
        this.role = role;
        this.isEmailVerified = isEmailVerified;
    }

    public void updateProfile(String name, String aboutMe) {
        this.name = name;
        this.aboutMe = aboutMe;
    }

    public void verifyEmail() {
        this.isEmailVerified = true;
    }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }
}