package kr.spring.storage.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FileFolderVO {
	private long folder_num;
	private long team_num;
	private String folder_name;
	private Long parent_folder_num;
	private Date create_date;
	private Long channel_num;
	
	private int folder_level;		//깊이(1=최상위)
	private int is_leaf;			//자식 폴더 없으면 1
	private String folder_path;		//전체 경로
	
	private String is_chat_folder; // Y: 채팅방 연동 폴더(삭제불가), N: 일반 폴더
}
