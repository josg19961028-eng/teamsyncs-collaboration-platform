package kr.spring.notice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import kr.spring.notice.dao.NoticeMapper;
import kr.spring.notice.vo.NoticeVO;

@Service
public class NoticeServiceImpl implements NoticeService {

    @Autowired
    private NoticeMapper noticeMapper;

    @Override
    public List<NoticeVO> selectNoticesByTeam(long teamNum) {
        return noticeMapper.selectNoticesByTeam(teamNum);
    }

    @Override
    public NoticeVO selectNoticeByNum(long noticeNum) {
        return noticeMapper.selectNoticeByNum(noticeNum);
    }

    @Override
    public void insertNotice(NoticeVO notice) {
        noticeMapper.insertNotice(notice);
    }

    @Override
    public void updateNotice(NoticeVO notice) {
        noticeMapper.updateNotice(notice);
    }

    @Override
    public void deleteNotice(long noticeNum) {
        noticeMapper.deleteNotice(noticeNum);
    }

    @Override
    public void updateViewCount(long noticeNum) {
        noticeMapper.updateViewCount(noticeNum);
    }

    @Override
    public void togglePin(long noticeNum) {
        noticeMapper.togglePin(noticeNum);
    }
}