package kr.spring.chat.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatMentionVO {
	private long mention_num;
	private long message_num;
	private long mentioned_member_num;
	private String is_notified;
}
