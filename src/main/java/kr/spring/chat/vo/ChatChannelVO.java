package kr.spring.chat.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatChannelVO {
	private long channel_num;
	private long team_num;
	private String channel_name;
	private String is_default;
	private Date create_date;
	private long create_by;
	private String channel_desc;
	private int category;
	
	//join해서 가져올 데이터
	private int unreadCount;		//내가 안 읽은 메시지 개수
	private String lastMessage;		//채널 목록에 보여줄 마지막 채팅 내용
	private String lastTime;		//마지막 채팅 시간
}
