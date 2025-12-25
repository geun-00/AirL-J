package project.airbnb.clone;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.util.ReflectionTestUtils;
import project.airbnb.clone.entity.member.Member;
import project.airbnb.clone.fixtures.MemberFixture;
import project.airbnb.clone.model.AuthProviderUser;
import project.airbnb.clone.model.PrincipalUser;

import java.util.List;

public class WithTestAuthSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockMember annotation) {
        Member member = MemberFixture.create();
        ReflectionTestUtils.setField(member, "id", 1L);

        PrincipalUser principalUser = new PrincipalUser(new AuthProviderUser(member, "mock-principal"));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                principalUser, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        return context;
    }
}
