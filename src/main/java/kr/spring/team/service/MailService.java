package kr.spring.team.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MailService {

	@Autowired
	private JavaMailSender mailSender;

	/**
	 * 팀 초대 메일 발송 (TM-005)
	 * - 메일에는 "초대 확인하기" 링크 하나만 포함. 실제 수락/거절은 그 링크로 열리는
	 *   랜딩 페이지(invite-landing)에서 버튼으로 선택하도록 UX 개선.
	 * @param toEmail       받는사람 이메일
	 * @param teamName      팀 이름
	 * @param inviterName   초대한 사람 이름
	 * @param viewUrl       초대 확인 랜딩 페이지 링크 (/team/invite/email/view?invitationNum=...)
	 */
	public void sendTeamInviteMail(String toEmail, String teamName, String inviterName,
			String viewUrl) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

			helper.setTo(toEmail);
			helper.setSubject("[TeamSync] '" + teamName + "' 팀 초대");

			String html =
				"<div style='font-family:sans-serif;padding:20px'>"
				+ "<h2>TeamSync 팀 초대</h2>"
				+ "<p><b>" + inviterName + "</b>님이 <b>" + teamName + "</b> 팀에 초대했습니다.</p>"
				+ "<div style='margin:20px 0'>"
				+ "<a href='" + viewUrl + "' style='background:#6c5ce7;color:#fff;padding:10px 18px;"
				+ "border-radius:8px;text-decoration:none'>초대 확인하기</a>"
				+ "</div>"
				+ "<p style='color:#888;font-size:12px'>이 초대는 3일 후 만료됩니다.</p>"
				+ "</div>";

			helper.setText(html, true);
			mailSender.send(message);
			log.debug("<<초대 메일 발송 성공>> to={}", toEmail);
		} catch (MessagingException e) {
			log.error("<<초대 메일 발송 실패>> to={}, error={}", toEmail, e.toString());
			throw new RuntimeException("메일 발송 실패", e);
		}
	}
}