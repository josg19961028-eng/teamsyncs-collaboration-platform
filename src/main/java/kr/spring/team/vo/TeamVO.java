package kr.spring.team.vo;

import java.io.IOException;
import java.sql.Date;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"team_photo"})
public class TeamVO {
	private long team_num;
	private String team_name;
	private String description;
	private byte[] team_photo;
	private String team_photo_name;
	private String color;
	private int status;
	private Date created_at;
	private Date updated_at;
	private Date deleted_at;
	//팀을 만든 사람 팀장, 만약 위임 등 팀장이 바뀌면 여기도 변경 됨
	private long creator_num;

	//============팀 수정(TM-002) 이미지 처리 플래그=====================//
	// DB 컬럼 아님. updateTeam 매퍼에서 이미지 컬럼 분기용으로만 사용.
	// 0 = 기존 이미지 유지(사진 컬럼 미변경)
	// 1 = 새 이미지로 교체(team_photo / team_photo_name 세팅 필요)
	// 2 = 이미지 제거(사진 컬럼 NULL 처리)
	private int photoAction;
	//============팀 수정(TM-002) 이미지 처리 플래그=====================//

	//============이미지 BLOB 처리=====================//
	//(주의)폼에서 파일업로드 파라미터네임은 반드시 upload로 지정해야 함
	public void setUpload(MultipartFile upload) throws IOException {
		if (upload == null || upload.isEmpty()) return;
		//MultipartFile -> byte[]
		setTeam_photo(upload.getBytes());
		//파일 이름
		setTeam_photo_name(upload.getOriginalFilename());
	}
	//============이미지 BLOB 처리=====================//
}