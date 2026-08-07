package kr.spring.users.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.users.dao.UsersMapper;
import kr.spring.users.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

import java.sql.Date;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@Transactional
public class UsersServiceImpl implements UsersService{

	
	@Autowired
	private UsersMapper usersMapper;
	
	/*******************
	 * 회원 번호로 사용자 조회
	 * - USER_NUM 기준으로 사용자 정보를 조회
	 *******************/
	@Override
	public UsersVO selectByUserNum(long userNum) {
		return usersMapper.selectByUserNum(userNum);
	}

	/*******************
	 * 이메일로 사용자 조회
	 * - 로그인 및 계정 확인에 사용할 사용자 정보를 EMAIL 기준으로 조회
	 *******************/
	@Override
	public UsersVO selectByEmail(String email) {
		return usersMapper.selectByEmail(normalizeEmail(email));
	}

	/*******************
	 * 이메일 중복 여부 확인
	 * - 같은 이메일을 사용하는 계정이 존재하는지 확인
	 *******************/
	@Override
	public boolean isEmailDuplicated(String email) {
		return usersMapper.countByEmail(normalizeEmail(email)) > 0;
	}
	
	/*******************
	 * 휴대폰 번호 중복 여부 확인
	 * - 탈퇴하지 않은 사용자(status != 3) 기준으로 같은 번호가 있는지 확인
	 *******************/
	@Override
	public boolean isPhoneDuplicated(String phone) {
	    return usersMapper.countByPhone(phone) > 0;
	}

	/*******************
	 * 본인을 제외한 휴대폰 번호 중복 여부 확인
	 * - 마이페이지 수정 시 현재 로그인 사용자는 제외하고 확인
	 *******************/
	@Override
	public boolean isPhoneDuplicatedExceptUser(String phone, long userNum) {
	    return usersMapper.countByPhoneExceptUser(phone, userNum) > 0;
	}

	/*******************
	 * 마이페이지 프로필 수정
	 * - 닉네임, 전화번호, 자기소개만 수정
	 *******************/
	@Override
	public int updateMyProfile(UsersVO userVO) {
	    return usersMapper.updateMyProfile(userVO);
	}
	
	/*******************
	 * 이메일 찾기용 사용자 조회
	 * - 실명, 휴대폰번호, 생년월일이 모두 일치하는 사용자 목록 조회
	 *******************/
	@Override
	public List<UsersVO> findEmailsByUserInfo(String realName, String phone, Date birth) {
	    return usersMapper.findEmailsByUserInfo(realName, phone, birth);
	}

	/*******************
	 * 회원가입
	 * - 입력받은 사용자 정보를 USERS 테이블에 등록
	 *******************/
	@Override
	public void insertUser(UsersVO userVO) {
		userVO.setEmail(normalizeEmail(userVO.getEmail()));
		usersMapper.insertUser(userVO);
	}

	/*******************
	 * 비밀번호 변경
	 * - 이메일 기준으로 암호화된 새 비밀번호를 저장
	 *******************/
	@Override
	public int updatePassword(String email, String passwd) {
		return usersMapper.updatePassword(normalizeEmail(email), passwd);
	}

	private String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

}
