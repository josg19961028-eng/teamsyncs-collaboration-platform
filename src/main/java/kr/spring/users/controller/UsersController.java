package kr.spring.users.controller;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.spring.users.service.EmailService;
import kr.spring.users.service.GoogleOAuthService;
import kr.spring.users.service.UsersService;
import kr.spring.users.security.CustomOAuth2UserService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UsersVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/users")
public class UsersController {

    private final PasswordEncoder passwordEncoder;

    @Autowired
    private UsersService usersService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GoogleOAuthService googleOAuthService;

 	// 이메일 인증코드 유효 시간(분)
    private static final int EMAIL_CODE_EXPIRE_MINUTES = 5;
    
	// 일반 회원가입/비밀번호 재설정에서 사용하는 비밀번호 형식
	// 영문, 숫자, 지정 특수문자(!@#$%^&*)만 허용하며 8~20자
    private static final String PASSWORD_PATTERN = "^[A-Za-z0-9!@#$%^&*]{8,20}$";

    UsersController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    
    /*******************
     * 회원가입 이메일 중복 확인
     * - 입력받은 이메일이 USERS 테이블에 이미 존재하는지 확인
     * - JSON 형태로 duplicated, available 값을 반환
     *******************/
    @GetMapping("/checkEmail")
    @ResponseBody
    public Map<String, Object> checkEmail(@RequestParam("email") String email) {
        boolean duplicated = false;

        if (email != null && !email.trim().isEmpty()) {
            duplicated = usersService.isEmailDuplicated(normalizeEmail(email));
        }

        return Map.of(
            "duplicated", duplicated,
            "available", !duplicated
        );
    }
    
    /*******************
     * 휴대폰 중복 확인
     *******************/
    @GetMapping("/checkPhone")
    @ResponseBody
    public Map<String, Object> checkPhone(@RequestParam("phone") String phone) {
        String inputPhone = phone == null ? "" : phone.trim();

        if (inputPhone.isEmpty()) {
            return Map.of(
                "result", "empty",
                "message", "휴대폰 번호를 입력해주세요."
            );
        }

        if (!inputPhone.matches("^01[0-9]{8,9}$")) {
            return Map.of(
                "result", "invalidPhone",
                "message", "휴대폰 번호는 - 제외 숫자만 입력해주세요."
            );
        }

        boolean duplicated = usersService.isPhoneDuplicated(inputPhone);

        return Map.of(
            "result", duplicated ? "duplicated" : "available",
            "duplicated", duplicated,
            "available", !duplicated,
            "message", duplicated ? "이미 사용 중인 휴대폰 번호입니다." : "사용 가능한 휴대폰 번호입니다."
        );
    }
    
    
    
    

    
    /*******************
     * 이메일 찾기
     * - 실명, 휴대폰번호, 생년월일로 가입 계정을 조회
     * - 일치하는 계정이 1개일 때 마스킹된 이메일을 반환
     *******************/
    @PostMapping("/findEmail")
    @ResponseBody
    public Map<String, Object> findEmail(@RequestParam("real_name") String realName,
                                         @RequestParam("phone") String phone,
                                         @RequestParam("birthText") String birthText) {
        String inputRealName = realName == null ? "" : realName.trim();
        String inputPhone = phone == null ? "" : phone.trim();
        String inputBirthText = birthText == null ? "" : birthText.trim();

        if (inputRealName.isEmpty() || inputPhone.isEmpty() || inputBirthText.isEmpty()) {
            return Map.of(
                "result", "empty",
                "message", "이름, 휴대폰번호, 생년월일을 모두 입력해주세요."
            );
        }

        if (!inputPhone.matches("^01[0-9]{8,9}$")) {
            return Map.of(
                "result", "invalidPhone",
                "message", "휴대폰번호는 - 제외 숫자만 입력해주세요."
            );
        }

        Date birth = parseBirth(inputBirthText);
        if (birth == null) {
            return Map.of(
                "result", "invalidBirth",
                "message", "올바른 생년월일을 8자리 숫자로 입력해주세요."
            );
        }

        List<UsersVO> users = usersService.findEmailsByUserInfo(inputRealName, inputPhone, birth);

        if (users.isEmpty()) {
            return Map.of(
                "result", "notFound",
                "message", "입력한 정보와 일치하는 계정을 찾을 수 없습니다."
            );
        }

        if (users.size() > 1) {
            return Map.of(
                "result", "multiple",
                "message", "입력한 정보와 일치하는 계정이 여러 개 있습니다. 관리자에게 문의해주세요."
            );
        }

        return Map.of(
            "result", "success",
            "email", maskEmail(users.get(0).getEmail()),
            "message", "가입된 이메일을 찾았습니다."
        );
    }

    
    
    /*******************
     * 회원가입 이메일 인증코드 발송
     * - 중복되지 않은 이메일에 6자리 인증코드를 전송
     * - 인증 이메일, 코드, 만료 시간, 인증 여부를 세션에 저장
     *******************/
    @PostMapping("/sendEmailCode")
    @ResponseBody
    public Map<String, Object> sendEmailCode(@RequestParam("email") String email,
                                             HttpSession session) {
        String targetEmail = normalizeEmail(email);

        if (targetEmail.isEmpty()) {
            return Map.of(
                "result", "empty",
                "message", "이메일을 입력해주세요."
            );
        }

        if (usersService.isEmailDuplicated(targetEmail)) {
            return Map.of(
                "result", "duplicated",
                "message", "이미 가입한 이메일입니다."
            );
        }

        String code = createEmailCode();

        session.setAttribute("signupEmail", targetEmail);
        session.setAttribute("signupEmailCode", code);
        session.setAttribute("signupEmailExpireTime", LocalDateTime.now().plusMinutes(EMAIL_CODE_EXPIRE_MINUTES));
        session.setAttribute("signupEmailVerified", false);

        emailService.sendSignupVerificationCode(targetEmail, code);

        return Map.of(
            "result", "success",
            "message", "인증번호를 이메일로 전송했습니다.",
            "expireSeconds", EMAIL_CODE_EXPIRE_MINUTES * 60
        );
    }

    
    /*******************
     * 회원가입 이메일 인증코드 확인
     * - 세션에 저장된 인증코드와 사용자가 입력한 코드를 비교
     * - 이메일과 만료 시간을 함께 검증한 뒤 인증 완료 상태로 변경
     *******************/
    @PostMapping("/verifyEmailCode")
    @ResponseBody
    public Map<String, Object> verifyEmailCode(@RequestParam("email") String email,
                                               @RequestParam("code") String code,
                                               HttpSession session) {
        String targetEmail = normalizeEmail(email);
        String inputCode = code == null ? "" : code.trim();

        String sessionEmail = (String) session.getAttribute("signupEmail");
        String sessionCode = (String) session.getAttribute("signupEmailCode");
        LocalDateTime expireTime = (LocalDateTime) session.getAttribute("signupEmailExpireTime");

        if (sessionEmail == null || sessionCode == null || expireTime == null) {
            return Map.of(
                "result", "none",
                "message", "인증번호를 먼저 발송해주세요."
            );
        }

        if (LocalDateTime.now().isAfter(expireTime)) {
            session.removeAttribute("signupEmailCode");
            session.removeAttribute("signupEmailExpireTime");
            session.setAttribute("signupEmailVerified", false);

            return Map.of(
                "result", "expired",
                "message", "인증 시간이 만료되었습니다. 인증번호를 다시 발송해주세요."
            );
        }

        if (!sessionEmail.equals(targetEmail)) {
            return Map.of(
                "result", "emailChanged",
                "message", "인증을 요청한 이메일과 현재 이메일이 다릅니다."
            );
        }

        if (!sessionCode.equals(inputCode)) {
            return Map.of(
                "result", "invalid",
                "message", "인증번호가 일치하지 않습니다."
            );
        }

        session.setAttribute("signupEmailVerified", true);

        return Map.of(
            "result", "success",
            "message", "이메일 인증이 완료되었습니다."
        );
    }

    
    
    /*******************
     * 비밀번호 재설정 인증코드 발송
     * - 가입된 일반 계정 이메일에만 재설정 인증코드를 전송
     * - 정지/탈퇴/소셜 로그인 계정은 재설정을 차단
     *******************/
    @PostMapping("/sendResetPasswordCode")
    @ResponseBody
    public Map<String, Object> sendResetPasswordCode(@RequestParam("email") String email,
                                                     HttpSession session) {
        String targetEmail = normalizeEmail(email);

        if (targetEmail.isEmpty()) {
            return Map.of(
                "result", "empty",
                "message", "이메일을 입력해주세요."
            );
        }

        UsersVO user = usersService.selectByEmail(targetEmail);

        if (user == null) {
            return Map.of(
                "result", "notFound",
                "message", "가입된 이메일을 찾을 수 없습니다."
            );
        }

        if (user.getStatus() != 1) {
            return Map.of(
                "result", "unavailable",
                "message", "비밀번호를 재설정할 수 없는 계정입니다."
            );
        }

        if (user.getLogin_type() == 2) {
            return Map.of(
                "result", "socialAccount",
                "message", "소셜 로그인 계정은 해당 서비스 로그인을 이용해주세요."
            );
        }

        String code = createEmailCode();

        session.setAttribute("resetPasswordEmail", targetEmail);
        session.setAttribute("resetPasswordCode", code);
        session.setAttribute("resetPasswordExpireTime", LocalDateTime.now().plusMinutes(EMAIL_CODE_EXPIRE_MINUTES));
        session.setAttribute("resetPasswordVerified", false);

        emailService.sendResetPasswordVerificationCode(targetEmail, code);

        return Map.of(
            "result", "success",
            "message", "인증번호를 이메일로 전송했습니다.",
            "expireSeconds", EMAIL_CODE_EXPIRE_MINUTES * 60
        );
    }

    
    /*******************
     * 비밀번호 재설정 인증코드 확인
     * - 재설정용 세션에 저장된 인증코드와 사용자가 입력한 코드를 비교
     * - 이메일과 만료 시간을 함께 검증한 뒤 재설정 인증 완료 상태로 변경
     *******************/
    @PostMapping("/verifyResetPasswordCode")
    @ResponseBody
    public Map<String, Object> verifyResetPasswordCode(@RequestParam("email") String email,
                                                       @RequestParam("code") String code,
                                                       HttpSession session) {
        String targetEmail = normalizeEmail(email);
        String inputCode = code == null ? "" : code.trim();

        String sessionEmail = (String) session.getAttribute("resetPasswordEmail");
        String sessionCode = (String) session.getAttribute("resetPasswordCode");
        LocalDateTime expireTime = (LocalDateTime) session.getAttribute("resetPasswordExpireTime");

        if (sessionEmail == null || sessionCode == null || expireTime == null) {
            return Map.of(
                "result", "none",
                "message", "인증번호를 먼저 발송해주세요."
            );
        }

        if (LocalDateTime.now().isAfter(expireTime)) {
            clearResetPasswordSession(session);

            return Map.of(
                "result", "expired",
                "message", "인증 시간이 만료되었습니다. 인증번호를 다시 발송해주세요."
            );
        }

        if (!sessionEmail.equals(targetEmail)) {
            return Map.of(
                "result", "emailChanged",
                "message", "인증을 요청한 이메일과 현재 이메일이 다릅니다."
            );
        }

        if (!sessionCode.equals(inputCode)) {
            return Map.of(
                "result", "invalid",
                "message", "인증번호가 일치하지 않습니다."
            );
        }

        session.setAttribute("resetPasswordVerified", true);

        return Map.of(
            "result", "success",
            "message", "이메일 인증이 완료되었습니다."
        );
    }

    
    /*******************
     * 비밀번호 재설정
     * - 이메일 인증이 완료된 세션인지 확인
     * - 새 비밀번호 형식과 확인값 일치 여부를 검증
     * - 비밀번호를 암호화하여 USERS.PASSWD에 반영
     *******************/
    @PostMapping("/resetPassword")
    @ResponseBody
    public Map<String, Object> resetPassword(@RequestParam("email") String email,
                                             @RequestParam("passwd") String passwd,
                                             @RequestParam("confirm_passwd") String confirmPasswd,
                                             HttpSession session) {
        String targetEmail = normalizeEmail(email);
        String newPasswd = passwd == null ? "" : passwd.trim();
        String newConfirmPasswd = confirmPasswd == null ? "" : confirmPasswd.trim();

        Boolean verified = (Boolean) session.getAttribute("resetPasswordVerified");
        String verifiedEmail = (String) session.getAttribute("resetPasswordEmail");
        LocalDateTime expireTime = (LocalDateTime) session.getAttribute("resetPasswordExpireTime");

        if (!Boolean.TRUE.equals(verified) || !targetEmail.equals(verifiedEmail)) {
            return Map.of(
                "result", "notVerified",
                "message", "이메일 인증을 완료해주세요."
            );
        }

        if (expireTime == null || LocalDateTime.now().isAfter(expireTime)) {
            clearResetPasswordSession(session);

            return Map.of(
                "result", "expired",
                "message", "인증 시간이 만료되었습니다. 다시 인증해주세요."
            );
        }

        if (newPasswd.isEmpty() || newConfirmPasswd.isEmpty()) {
            return Map.of(
                "result", "empty",
                "message", "새 비밀번호를 입력해주세요."
            );
        }

        if (!newPasswd.matches(PASSWORD_PATTERN)) {
            return Map.of(
                "result", "invalidPassword",
                "message", "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*) 8~20자로 입력해주세요."
            );
        }

        if (!newPasswd.equals(newConfirmPasswd)) {
            return Map.of(
                "result", "passwordMismatch",
                "message", "비밀번호와 비밀번호 확인이 일치하지 않습니다."
            );
        }

        UsersVO user = usersService.selectByEmail(targetEmail);

        if (user == null || user.getStatus() != 1 || user.getLogin_type() == 2) {
            clearResetPasswordSession(session);

            return Map.of(
                "result", "unavailable",
                "message", "비밀번호를 재설정할 수 없는 계정입니다."
            );
        }

        int updated = usersService.updatePassword(targetEmail, passwordEncoder.encode(newPasswd));

        if (updated < 1) {
            return Map.of(
                "result", "fail",
                "message", "비밀번호 변경에 실패했습니다."
            );
        }

        clearResetPasswordSession(session);

        return Map.of(
            "result", "success",
            "message", "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요."
        );
    }

    
    /*******************
     * 일반 회원가입 처리
     * - 필수 입력값과 형식을 검증
     * - 이메일 인증 완료 여부를 확인
     * - 비밀번호를 암호화한 뒤 신규 사용자를 등록
     *******************/
    @PostMapping("/signup")
    public String signup(UsersVO userVO, RedirectAttributes redirectAttributes, HttpSession session) {
        String email = normalizeEmail(userVO.getEmail());
        String passwd = userVO.getPasswd() == null ? "" : userVO.getPasswd().trim();
        String confirmPasswd = userVO.getConfirm_passwd() == null ? "" : userVO.getConfirm_passwd().trim();
        String userName = userVO.getUser_name() == null ? "" : userVO.getUser_name().trim();
        String realName = userVO.getReal_name() == null ? "" : userVO.getReal_name().trim();
        String phone = userVO.getPhone() == null ? "" : userVO.getPhone().trim();
        String birthText = userVO.getBirthText() == null ? "" : userVO.getBirthText().trim();
        String intro = userVO.getIntro() == null ? "" : userVO.getIntro().trim();

        if (email.isEmpty() || passwd.isEmpty() || userName.isEmpty()
                || realName.isEmpty() || phone.isEmpty() || birthText.isEmpty() || intro.isEmpty()) {
            redirectAttributes.addFlashAttribute("signupError", "이메일, 비밀번호, 닉네임, 실명, 휴대폰번호, 생년월일은 필수입니다.");
            return "redirect:/member/login";
        }

        if (!phone.matches("^01[0-9]{8,9}$")) {
            redirectAttributes.addFlashAttribute("signupError", "휴대폰번호는 - 제외 숫자만 입력해주세요.");
            return "redirect:/member/login";
        }
        
        if (usersService.isPhoneDuplicated(phone)) {
            redirectAttributes.addFlashAttribute("signupError", "이미 사용 중인 휴대폰 번호입니다.");
            return "redirect:/member/login";
        }

        Date birth = parseBirth(birthText);
        if (birth == null) {
            redirectAttributes.addFlashAttribute("signupError", "올바른 생년월일을 8자리 숫자로 입력해주세요.");
            return "redirect:/member/login";
        }

        if (!passwd.matches(PASSWORD_PATTERN)) {
            redirectAttributes.addFlashAttribute("signupError", "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*) 8~20자로 입력해주세요.");
            return "redirect:/member/login";
        }

        if (!passwd.equals(confirmPasswd)) {
            redirectAttributes.addFlashAttribute("signupError", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            return "redirect:/member/login";
        }

        if (usersService.isEmailDuplicated(email)) {
            redirectAttributes.addFlashAttribute("signupError", "이미 사용 중인 이메일입니다.");
            return "redirect:/member/login";
        }

        Boolean emailVerified = (Boolean) session.getAttribute("signupEmailVerified");
        String verifiedEmail = (String) session.getAttribute("signupEmail");
        LocalDateTime expireTime = (LocalDateTime) session.getAttribute("signupEmailExpireTime");

        if (!Boolean.TRUE.equals(emailVerified) || !email.equals(verifiedEmail)) {
            redirectAttributes.addFlashAttribute("signupError", "이메일 인증을 완료해주세요.");
            return "redirect:/member/login";
        }

        if (expireTime == null || LocalDateTime.now().isAfter(expireTime)) {
            session.removeAttribute("signupEmailCode");
            session.removeAttribute("signupEmailExpireTime");
            session.setAttribute("signupEmailVerified", false);

            redirectAttributes.addFlashAttribute("signupError", "이메일 인증 시간이 만료되었습니다. 다시 인증해주세요.");
            return "redirect:/member/login";
        }

        userVO.setEmail(email);
        userVO.setPasswd(passwordEncoder.encode(passwd));
        userVO.setUser_name(userName);
        userVO.setReal_name(realName);
        userVO.setPhone(phone);
        userVO.setBirth(birth);
        userVO.setIntro(intro);
        userVO.setAuth("USER_MEMBER");
        userVO.setLogin_type(1);
        userVO.setGoogle_id(null);
        userVO.setStatus(1);

        usersService.insertUser(userVO);
        session.removeAttribute("signupEmail");
        session.removeAttribute("signupEmailCode");
        session.removeAttribute("signupEmailExpireTime");
        session.removeAttribute("signupEmailVerified");

        redirectAttributes.addFlashAttribute("signupSuccess", "회원가입이 완료되었습니다. 로그인해주세요.");
        return "redirect:/member/login";
    }

    
    /*******************
     * 생년월일 문자열 변환
     * - yyyyMMdd 형식의 8자리 문자열을 java.sql.Date로 변환
     * - 형식이 맞지 않거나 존재하지 않는 날짜면 null 반환
     *******************/
    @GetMapping("/google/additional-info")
    public String googleAdditionalInfoForm(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
        if (principal == null) {
            return "redirect:/member/login";
        }

        UsersVO user = principal.getUsersVO();
        if (!googleOAuthService.requiresAdditionalInfo(user)) {
            return "redirect:/main/home";
        }

        model.addAttribute("user", user);
        return "thviews/member/googleAdditionalInfo";
    }

    @GetMapping("/google/link")
    public String startGoogleLink(@AuthenticationPrincipal PrincipalDetails principal,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/member/login";
        }

        UsersVO loginUser = principal.getUsersVO();
        if (loginUser.getLogin_type() != 1 || loginUser.getGoogle_id() != null) {
            redirectAttributes.addFlashAttribute("profileError", "이미 Google 계정이 연동되어 있습니다.");
            return "redirect:/member/myPage";
        }

        session.setAttribute(CustomOAuth2UserService.GOOGLE_LINK_USER_NUM, loginUser.getUser_num());
        session.setAttribute(CustomOAuth2UserService.GOOGLE_LINK_EMAIL, loginUser.getEmail());
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/google/unlink")
    public String unlinkGoogle(@AuthenticationPrincipal PrincipalDetails principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/member/login";
        }

        UsersVO loginUser = principal.getUsersVO();
        if (loginUser.getLogin_type() == 2) {
            redirectAttributes.addFlashAttribute("profileError", "Google 전용 계정은 연동을 해제할 수 없습니다.");
            return "redirect:/member/myPage";
        }

        if (loginUser.getLogin_type() != 3 || loginUser.getGoogle_id() == null || loginUser.getGoogle_id().isBlank()) {
            redirectAttributes.addFlashAttribute("profileError", "연동된 Google 계정이 없습니다.");
            return "redirect:/member/myPage";
        }

        UsersVO updatedUser = googleOAuthService.unlinkGoogleAccount(loginUser.getUser_num());
        if (updatedUser == null) {
            redirectAttributes.addFlashAttribute("profileError", "Google 연동 해제에 실패했습니다.");
            return "redirect:/member/myPage";
        }

        loginUser.setGoogle_id(null);
        loginUser.setLogin_type(1);
        redirectAttributes.addFlashAttribute("profileSuccess", "Google 연동이 해제되었습니다.");
        return "redirect:/member/myPage";
    }

    @PostMapping("/google/additional-info")
    public String saveGoogleAdditionalInfo(@AuthenticationPrincipal PrincipalDetails principal,
                                           @RequestParam("real_name") String realName,
                                           @RequestParam("phone") String phone,
                                           @RequestParam("birthText") String birthText,
                                           RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/member/login";
        }

        UsersVO loginUser = principal.getUsersVO();
        String inputRealName = realName == null ? "" : realName.trim();
        String inputPhone = phone == null ? "" : phone.trim();
        String inputBirthText = birthText == null ? "" : birthText.trim();

        if (inputRealName.isEmpty() || inputPhone.isEmpty() || inputBirthText.isEmpty()) {
            redirectAttributes.addFlashAttribute("googleInfoError", "실명, 휴대폰번호, 생년월일을 모두 입력해주세요.");
            return "redirect:/users/google/additional-info";
        }

        if (!inputPhone.matches("^01[0-9]{8,9}$")) {
            redirectAttributes.addFlashAttribute("googleInfoError", "휴대폰번호는 - 제외 숫자만 10~11자리로 입력해주세요.");
            return "redirect:/users/google/additional-info";
        }

        if (googleOAuthService.isPhoneDuplicatedExceptUser(inputPhone, loginUser.getUser_num())) {
            redirectAttributes.addFlashAttribute("googleInfoError", "이미 사용 중인 휴대폰번호입니다.");
            return "redirect:/users/google/additional-info";
        }

        Date birth = parseBirth(inputBirthText);
        if (birth == null) {
            redirectAttributes.addFlashAttribute("googleInfoError", "생년월일은 8자리 숫자로 올바르게 입력해주세요.");
            return "redirect:/users/google/additional-info";
        }

        int updated = googleOAuthService.updateAdditionalInfo(loginUser.getUser_num(), inputRealName, inputPhone, birth);
        if (updated < 1) {
            redirectAttributes.addFlashAttribute("googleInfoError", "추가정보 저장에 실패했습니다.");
            return "redirect:/users/google/additional-info";
        }

        loginUser.setReal_name(inputRealName);
        loginUser.setPhone(inputPhone);
        loginUser.setBirth(birth);

        return "redirect:/main/home";
    }

    private Date parseBirth(String birthText) {
        if (birthText == null || !birthText.matches("^[0-9]{8}$")) {
            return null;
        }

        try {
            LocalDate birthDate = LocalDate.parse(birthText, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return Date.valueOf(birthDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    
    
    /*******************
     * 이메일 인증코드 생성
     * - 100000~999999 범위의 6자리 숫자 문자열 생성
     *******************/
    private String createEmailCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

    
    /*******************
     * 비밀번호 재설정 세션 정보 삭제
     * - 재설정 이메일, 인증코드, 만료 시간, 인증 여부 세션 값을 제거
     * - 인증 만료/성공/계정 오류 상황에서 재사용을 방지
     *******************/
    private void clearResetPasswordSession(HttpSession session) {
        session.removeAttribute("resetPasswordEmail");
        session.removeAttribute("resetPasswordCode");
        session.removeAttribute("resetPasswordExpireTime");
        session.removeAttribute("resetPasswordVerified");
    }

    
    
    /*******************
     * 이메일 마스킹
     * - 이메일 찾기 결과에서 개인정보 노출을 줄이기 위해 일부만 표시
     * - 아이디 앞 2자리만 노출하고 나머지는 *** 처리
     *******************/
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }

        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }

        return local.substring(0, 2) + "***@" + domain;
    }
}
