package kr.spring.member.vo;

import java.io.IOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
/*
정규표현식

한글 한 글자 이상 가능
/^[가-힣]+$/

한글, 띄어쓰기 한 글자 이상 가능
/^[가-힣\s]+$/

한글, 영문 한 글자 이상 가능
/^[가-힣a-zA-Z]+$

숫자 한 글자 이상 가능
/^[0-9]+$

문자, 숫자만 허용 최소6자 최대 12자
^[A-Za-z\d]{6,12}$
^[A-Za-z0-9]{6,12}$

문자, 숫자, 특수 문자 모두 무조건 1개 이상, 최소 6자 최대 12자
^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&])[A-Za-z\d!@#$%^&]{6,12}$
 */
@Getter
@Setter
@ToString(exclude = {"photo"})
public class MemberVO{

	public MemberVO() {}
	private long mem_num;
	@Pattern(regexp="^[A-Za-z0-9]{4,14}$")
	private String id;
	private String nick_name;
	private String authority;
	private String social_name;
	@NotBlank
	private String name;
	//@Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&])[A-Za-z\\d!@#$%^&]{4,10}$")
	@Pattern(regexp="^[A-Za-z0-9]{4,12}$")
	private String passwd;
	@NotBlank
	private String phone;
	@Email
	@NotBlank
	private String email;
	@Size(min=5,max=5)
	private String zipcode;
	@NotBlank
	private String address1;
	@NotBlank
	private String address2;
	private String hobby;
	//외래키 제약 조건이 있을 경우 Integer (null 처리) 아닐 경우 int (0처리)
	private Integer gender;
	private byte[] photo;
	private String photo_name;
	private Date reg_date;
	private Date modify_date;

	//비밀번호 변경시에만 조건체크
	@Pattern(regexp="^[A-Za-z0-9]+$")
	private String captcha_chars;

	//비밀번호 변경시 현재 비밀번호를 저장하는 용도로 사용
	//회원 가입, 회원 정보 수정 폼에서 데이터 전송시 now_passwd를 표시하지 않기 때문에 name이 전송되지 않아 에러가 발생하지 않음
	//@Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&])[A-Za-z\\d!@#$%^&]{4,10}$")
	@Pattern(regexp="^[A-Za-z0-9]{4,12}$")
	private String now_passwd;
	
	//답글(대댓글) 작성시 부모글 아이디/별명
	private String parent_id;
	private String pnick_name;
	
	public String getParentName() {
		if(pnick_name==null) return parent_id;
		return pnick_name;
	}

	//별명이 미등록되어 있으면 id 반환하고 별명이 등록되어 있으면 별명 반환
	public String getUserName() {
		if(nick_name==null) return id;
		return nick_name;
	}
	
	public int getAuthorityOrdinal() {
		if(authority == null) return -1;
		
		if(authority.equals(UserRole.INACTIVE.getValue())) {
			return UserRole.INACTIVE.ordinal();//0
		}else if(authority.equals(UserRole.SUSPENDED.getValue())) {
			return UserRole.SUSPENDED.ordinal();//1
		}else if(authority.equals(UserRole.USER.getValue())) {
			return UserRole.USER.ordinal();//2
		}else if(authority.equals(UserRole.ADMIN.getValue())) {
			return UserRole.ADMIN.ordinal();//3
		}else {
			return -1;
		}
	}
	
	//===========비밀번호 일치 여부 체크====================//

	//============이미지 BLOB 처리=====================//
	//(주의)폼에서 파일업로드 파라미터네임은 반드시 upload로 지정해야 함
	public void setUpload(MultipartFile upload) throws IOException {
		//MultipartFile -> byte[]
		setPhoto(upload.getBytes());
		//파일 이름
		setPhoto_name(upload.getOriginalFilename());
	}
	//============이미지 BLOB 처리=====================//

	//===================checkbox===========================//
	//form:checkbox에서 사용할 수 있도록 String -> String[]로 변환 
	public String[] getF_hobby() {
		String[] f_hobby = null;
		if(hobby!=null) f_hobby = hobby.split(",");
		return f_hobby;
	}
	//String[] -> String
	public void setF_hobby(String[] f_hobby) {
		if(f_hobby!=null) {
			this.hobby = "";
			for(int i=0;i<f_hobby.length;i++) {
				if(i>0) this.hobby += ",";
				this.hobby += f_hobby[i];
			}
		}
	}
	//===================checkbox===========================//


}



