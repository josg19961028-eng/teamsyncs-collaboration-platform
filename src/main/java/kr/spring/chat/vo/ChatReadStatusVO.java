package kr.spring.chat.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatReadStatusVO {
	private long channel_num;
	private long user_num;
	private long last_read_message_num;
}
