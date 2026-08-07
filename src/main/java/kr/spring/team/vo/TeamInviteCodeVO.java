package kr.spring.team.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TeamInviteCodeVO {
	private long invite_code_num;
	private long team_num;
	private String code;	//UUID 기반 초대 코드
	private int status;		//1:VALID, 2:EXPIRED, 3:DISABLED
	private Date issued_at;
	private Date expired_at;
	private long issuer_num;	//발급자(팀장)
}
