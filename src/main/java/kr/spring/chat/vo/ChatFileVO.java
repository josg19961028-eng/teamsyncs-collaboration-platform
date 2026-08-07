package kr.spring.chat.vo;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ChatFileVO {
	private long file_num;
	private long message_num;
	private Long channel_num;
	private String origin_name;
	private String save_name;
	private long file_size;
	private String file_type;
	private long uploader_num;
	private Timestamp upload_date;
	private long team_num;
	
	private String file_path;       // 추가: 서버 저장 경로
    private String mime_type;       // 추가: 썸네일/다운로드용
    private String file_source;
    private Long folder_num;
    
    private String uploaderName;	//업로더 이름
}
