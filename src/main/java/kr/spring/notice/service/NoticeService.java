package kr.spring.notice.service;

import java.util.List;

import kr.spring.notice.vo.NoticeVO;

public interface NoticeService {

    public List<NoticeVO> selectNoticesByTeam(long teamNum);

    public NoticeVO selectNoticeByNum(long noticeNum);

    public void insertNotice(NoticeVO notice);

    public void updateNotice(NoticeVO notice);

    public void deleteNotice(long noticeNum);

    public void updateViewCount(long noticeNum);

    public void togglePin(long noticeNum);
}