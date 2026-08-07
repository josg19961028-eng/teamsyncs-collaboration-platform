package kr.spring.users.service;

public interface EmailService {

	public void sendSignupVerificationCode(String toEmail, String code);

	public void sendResetPasswordVerificationCode(String toEmail, String code);
}
