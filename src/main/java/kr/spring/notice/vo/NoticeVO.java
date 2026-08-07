package kr.spring.notice.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class NoticeVO {

    private long   notice_num;
    private long   team_num;
    private long   user_num;
    private String title;
    private String content;     // 목록 조회 시 null, 상세 조회(/notice/detail) 시에만 채워짐
    private String is_fixed;    // 'Y' / 'N'
    private Date   reg_date;    // DB 날짜값 (Mapper 전용)
    private int    view_count;

    // ===== DB 컬럼 아님, 가공 후 세팅 =====
    private String writer_name; // USERS.USER_NAME (조인)
    private String reg_date_str; // yyyy-MM-dd 형식, 컨트롤러에서 reg_date 기반으로 세팅
}