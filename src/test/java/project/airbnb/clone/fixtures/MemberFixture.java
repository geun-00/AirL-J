package project.airbnb.clone.fixtures;

import project.airbnb.clone.entity.member.Member;

import java.util.UUID;

public class MemberFixture {

    public static Member create() {
        return Member.createForRest("test-user", "test@email.com", null, null, UUID.randomUUID().toString());
    }
}
