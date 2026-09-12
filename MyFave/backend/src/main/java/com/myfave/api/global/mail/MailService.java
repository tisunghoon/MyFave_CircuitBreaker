package com.myfave.api.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

// 메일 양식을 정하는곳
//HTML 형식으로 꾸미려면 여기 바꿔야함
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Async("emailExecutor")
    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[MyFave] 비밀번호 재설정 인증코드");
        message.setText("인증코드: " + code + "\n\n5분 내로 입력해 주세요.");
        mailSender.send(message);
    }

    @Async("emailExecutor")
    public void sendSignUpCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[MyFave] 회원가입 이메일 인증코드");
        message.setText("인증코드: " + code + "\n\n5분 내로 입력해 주세요.");
        mailSender.send(message);
    }

    // 임시 비밀번호 메일은 ★동기★ 전송이어야 한다.
    // AuthService.sendTempPassword 는 @Transactional 안에서 user.updatePassword(...) 로 영속 비밀번호를
    // 먼저 바꾸기 때문에, 메일 발송이 비동기(@Async) 면 별도 스레드에서 발생한 예외가 호출자
    // 트랜잭션에 전파되지 않아 메일이 실패해도 비밀번호 변경이 그대로 커밋된다 → 사용자가 새 임시
    // 비밀번호도 알지 못한 채 계정 접근 차단. 동기로 두어 메일 실패 시 트랜잭션이 롤백되도록 보장.
    // (PR#185 CR Major — sendPasswordResetCode/sendSignUpCode 는 DB 변경 없는 코드 발송이므로 @Async 유지)
    public void sendTempPassword(String to, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[마이페이브] 임시 비밀번호 안내");
        message.setText(
                "임시 비밀번호: " + tempPassword + "\n\n"
                        + "로그인 후 즉시 비밀번호를 변경해주세요."
        );
        mailSender.send(message);
    }
}
