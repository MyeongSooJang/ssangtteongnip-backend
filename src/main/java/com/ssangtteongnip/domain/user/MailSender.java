package com.ssangtteongnip.domain.user;

public interface MailSender {
    // 실패하면 MAIL_SEND_FAILED BusinessException을 던진다.
    void sendVerificationCode(Email email, VerificationCode code);
}
