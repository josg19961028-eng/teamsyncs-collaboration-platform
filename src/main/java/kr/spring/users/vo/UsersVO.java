package kr.spring.users.vo;

import java.io.Serializable;
import java.sql.Date;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"photo"})
public class UsersVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long user_num;       // PK

    @Email
    @NotBlank
    private String email;        // 로그인 이메일(UK)

    @Pattern(regexp = "^[A-Za-z0-9!@#$%^&*]{8,20}$") //영문,숫자,특수문자 전부 가능
    private String passwd;       // 암호화 비밀번호

    @NotBlank
    private String user_name;    // 닉네임
    private String real_name;    // 실명 -> 회원가입,이메일찾기,비밀번호재설정에 사용
    private String phone;        // 전화번호
    private Date birth; 		//DB저장용
    private String birthText; // 생년월일 8자리 입력값  (화면입력용)
    private String intro;
    private byte[] photo;
    private String photo_name;
    private String auth = "USER_MEMBER";    // USER_MEMBER, USER_ADMIN
    private int login_type = 1;              // 1:일반, 2:구글, 3:구글연동
    private String google_id;
    private int status = 1;                  // 1:정상, 2:정지, 3:탈퇴
    private Date withdraw_date;
    private Date reg_date;
    private Date modify_date;

    private String confirm_passwd;           // 비밀번호 확인용(DB 컬럼 아님)

    // 이름 첫 글자 (헤더 아바타용)
    public String getFirstChar() {
        if (user_name == null || user_name.isEmpty()) return "U";
        return String.valueOf(user_name.charAt(0));
    }
}