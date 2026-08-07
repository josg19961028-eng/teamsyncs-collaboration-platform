package kr.spring.member.service;

import java.util.List;
import java.util.Map;

import kr.spring.member.vo.MemberVO;

public interface MemberService {
	public MemberVO selectCheckMember(String id);
	public MemberVO selectMember(Long mem_num);
}
