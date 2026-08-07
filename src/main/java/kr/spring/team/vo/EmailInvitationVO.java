package kr.spring.team.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EmailInvitationVO {
	private long invitation_num;
	private long team_num;
	private long inviter_num;	//초대한 사람(팀장)
	private String invitee_email;	//초대받는 이메일
	private int status;	//1:PENDING, 2:ACCEPTED, 3:REJECTED, 4:EXPIRED, 5:CANCELED
	private Date sent_at;
	private Date responsed_at;
	private Date expired_at;
}
