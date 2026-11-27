package com.ssangtteongnip.domain.user;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    // 변경을 DB에 바로 반영한다. 유니크 제약 위반을 이 시점에 알아야 할 때 쓴다.
    void flush();
    Optional<User> findById(Long id);
    List<User> findAllById(Iterable<Long> ids);
    // 행 잠금(PESSIMISTIC_WRITE). 같은 사용자의 동시 요청을 직렬화할 때 쓴다.
    Optional<User> findByIdForUpdate(Long id);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
    boolean existsByNickname(Nickname nickname);
}
