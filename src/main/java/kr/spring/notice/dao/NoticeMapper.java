package kr.spring.notice.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import kr.spring.notice.vo.NoticeVO;

@Mapper
public interface NoticeMapper {

    /** 팀 공지 목록 (CONTENT 제외, 고정 우선·최신순) */
    public List<NoticeVO> selectNoticesByTeam(long teamNum);

    /** 공지 상세 (CONTENT 포함) */
    public NoticeVO selectNoticeByNum(long noticeNum);

    /** 공지 작성 */
    public void insertNotice(NoticeVO notice);

    /** 공지 수정 (제목/내용/고정여부) */
    public void updateNotice(NoticeVO notice);

    /** 공지 삭제 */
    public void deleteNotice(long noticeNum);

    /** 조회수 +1 */
    public void updateViewCount(long noticeNum);

    /** 고정 토글 (Y↔N) */
    public void togglePin(long noticeNum);
}