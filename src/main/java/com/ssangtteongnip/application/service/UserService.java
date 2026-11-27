package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.NicknameResponse;
import com.ssangtteongnip.domain.user.Nickname;
import com.ssangtteongnip.domain.user.User;
import com.ssangtteongnip.domain.user.UserRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void changeNickname(Long userId, String rawNickname) {
        Nickname nickname = new Nickname(rawNickname);
        User user = findOrThrow(userId);
        user.changeNickname(nickname);
        flushUniqueNickname();
    }

    public NicknameResponse findNickname(Long userId) {
        return NicknameResponse.convert(findOrThrow(userId));
    }

    // 닉네임이 겹치는지는 읽고 판단하지 않고 DB 유니크 제약에 맡긴다.
    // 다른 요청이 먼저 가져갔다면 반영하는 이 시점에 제약 위반이 나므로 중복 오류로 바꾼다.
    private void flushUniqueNickname() {
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
