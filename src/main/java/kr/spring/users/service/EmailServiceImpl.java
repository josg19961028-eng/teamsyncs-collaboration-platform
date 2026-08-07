package kr.spring.users.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;

	
	
	/*******************
	 * 회원가입 이메일 인증코드 발송
	 * - 회원가입 시 입력한 이메일로 6자리 인증코드 전송
	 *******************/
	@Override
	public void sendSignupVerificationCode(String toEmail, String code) {
		SimpleMailMessage message = new SimpleMailMessage();

		message.setTo(toEmail);
		message.setSubject("[TeamSync] 회원가입 이메일 인증번호");
		message.setText(
			"TeamSync 회원가입 이메일 인증번호입니다.\n\n"
			+ "인증번호: " + code + "\n\n"
			+ "인증번호는 5분 동안 유효합니다."
		);

		mailSender.send(message);
	}

	
	
	/*******************
	 * 비밀번호 재설정 이메일 인증코드 발송
	 * - 비밀번호 재설정을 요청한 이메일로 6자리 인증코드 전송
	 *******************/
	@Override
	public void sendResetPasswordVerificationCode(String toEmail, String code) {
		SimpleMailMessage message = new SimpleMailMessage();

		message.setTo(toEmail);
		message.setSubject("[TeamSync] 비밀번호 재설정 인증번호");
		message.setText(
			"TeamSync 비밀번호 재설정 인증번호입니다.\n\n"
			+ "인증번호: " + code + "\n\n"
			+ "인증번호는 5분 동안 유효합니다."
		);

		mailSender.send(message);
	}
}
