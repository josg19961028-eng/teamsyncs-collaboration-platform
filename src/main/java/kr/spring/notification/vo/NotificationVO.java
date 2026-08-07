package kr.spring.notification.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NotificationVO {

	private long noti_num;
	private long sender_num;
	private long receiver_num;
	private long team_num;
	private int noti_type;
	private String title;
	private String content;
	private Date reg_date;
	private int read_status;
	private String link;
	// ===== 화면 표시용(DB 컬럼 아님) =====
	private String icon;         // noti_type 별 아이콘
	private String timeText;     // "5분 전" 같은 상대 시간 텍스트
	private String senderName;   // 발신자 이름(조인 결과)
	private String teamName;     // 팀 이름(조인 결과)
	private String typeLabel;    // noti_type 별 표시 텍스트(일정/채팅/칸반/팀/공지)
	private String typeClass;    // noti_type 별 뱃지 CSS 클래스(type-1 ~ type-5)
	private String dateLabel;    // 목록 그룹핑용 날짜 텍스트(오늘/어제/yyyy-MM-dd)
	private String fullDateText; // 상세 팝업용 전체 날짜/시간 텍스트(yyyy.MM.dd HH:mm)

	public boolean isRead() {
		return read_status == 2;
	}
}