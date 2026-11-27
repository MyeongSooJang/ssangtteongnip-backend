package com.ssangtteongnip.infrastructure;

import com.ssangtteongnip.domain.FakePasswordHasher;
import com.ssangtteongnip.domain.user.Email;
import com.ssangtteongnip.domain.user.Nickname;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 이메일이 임베디드 값 객체라서, 값 객체로 조회하는 파생 쿼리와 email 컬럼의 유니크 제약이 그대로 동작하는지 확인한다.
@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired EntityManager em;

    private User register(String rawEmail) {
        return register(rawEmail, "길동이");
    }

    private User register(String rawEmail, String rawNickname) {
        return User.register(new Email(rawEmail), "홍길동", new Nickname(rawNickname), "password1", new FakePasswordHasher());
    }

    @Test
    void 대소문자가_다르게_입력한_이메일로도_가입한_사용자를_찾는다() {
        User saved = userRepository.save(register("hong@example.com"));
        em.flush();
        em.clear();

        assertThat(userRepository.findByEmail(new Email(" HONG@example.com "))).get()
                .extracting(User::getId).isEqualTo(saved.getId());
    }

    @Test
    void 가입한_이메일만_존재한다고_답한다() {
        userRepository.save(register("hong@example.com"));

        assertThat(userRepository.existsByEmail(new Email("kim@example.com"))).isFalse();
        assertThat(userRepository.existsByEmail(new Email("hong@example.com"))).isTrue();
    }

    @Test
    void 같은_이메일로_두_번_가입할_수_없다() {
        userRepository.save(register("hong@example.com"));
        em.flush();

        assertThatThrownBy(() -> {
            userRepository.save(register("Hong@Example.com"));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_닉네임으로_두_명이_가입할_수_없다() {
        userRepository.save(register("hong@example.com", "길동이"));
        em.flush();

        assertThatThrownBy(() -> {
            userRepository.save(register("kim@example.com", "길동이"));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 닉네임이_없는_기존_사용자는_여럿이어도_된다() {
        User first = register("hong@example.com", "길동이");
        User second = register("kim@example.com", "길순이");
        ReflectionTestUtils.setField(first, "nickname", null);
        ReflectionTestUtils.setField(second, "nickname", null);

        userRepository.save(first);
        userRepository.save(second);
        em.flush();

        assertThat(em.createQuery("select count(u) from User u", Long.class).getSingleResult()).isEqualTo(2L);
    }

    @Test
    void 사용_중인_닉네임만_존재한다고_답한다() {
        userRepository.save(register("hong@example.com", "길동이"));

        assertThat(userRepository.existsByNickname(new Nickname("길동이"))).isTrue();
        assertThat(userRepository.existsByNickname(new Nickname("길순이"))).isFalse();
    }
}
