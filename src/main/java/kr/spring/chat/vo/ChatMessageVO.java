package kr.spring.chat.vo;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatMessageVO {
	private long message_num;
	private long channel_num;
	private long user_num;
	private String content;
	private Long parent_message;
	private Timestamp send_date;
	
	private String userName;
	private String userPhoto;
	
	private long file_num;
	private String origin_name;
	private String file_type;
	private long file_size;
	
	//답장 하기
	private String parentContent;
	private String parentUserName;
}
