package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.user.User;

// 닉네임이 없는 기존 사용자는 nickname이 null이다. 프런트는 이 값으로 닉네임 설정을 안내한다.
public record NicknameResponse(String nickname) {

    public static NicknameResponse convert(User user) {
        return new NicknameResponse(user.getNickname() == null ? null : user.getNickname().value());
    }
}
