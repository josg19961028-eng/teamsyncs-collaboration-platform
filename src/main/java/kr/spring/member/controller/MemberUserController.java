package kr.spring.member.controller;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import kr.spring.member.service.MemberService;
import kr.spring.member.vo.MemberVO;
import kr.spring.member.vo.PersonalTodoVO;
import kr.spring.notification.service.NotificationService;
import kr.spring.users.service.UsersService;
import kr.spring.users.vo.PrincipalDetails;
import kr.spring.users.vo.UsersVO;
import kr.spring.util.FileUtil;
import kr.spring.member.service.MyPageService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/member")
public class MemberUserController {
	@Autowired
	private UsersService usersService;
	
	@Autowired
	private MyPageService myPageService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	private static final String PASSWORD_PATTERN = "^[A-Za-z0-9!@#$%^&*]{8,20}$";

	//자바빈(VO) 초기화
	@ModelAttribute
	public UsersVO initCommand() {
		return new UsersVO();
	}

	/*============================================
	 * 회원로그인
	 *===========================================*/
	//로그인 폼
	@GetMapping("/login")
	public String formLogin() {
		return "thviews/member/memberLogin";
	}	
	
	/*============================================
	 * 마이페이지
	 *===========================================*/
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/myPage")
	public String myPage(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
	    long loginUserNum = principal.getUsersVO().getUser_num();

	    UsersVO user = usersService.selectByUserNum(loginUserNum);

	    model.addAttribute("user", user);
	    model.addAttribute("teamCount", myPageService.countMyTeams(loginUserNum));
	    model.addAttribute("kanbanCount", myPageService.countMyKanbanCards(loginUserNum));
	    model.addAttribute("minutesCount", myPageService.countMyMinutes(loginUserNum));

	    return "thviews/member/myPage";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/myPage")
	public String updateMyPage(@AuthenticationPrincipal PrincipalDetails principal,
	                           UsersVO userVO,
	                           RedirectAttributes redirectAttributes) {
	    long loginUserNum = principal.getUsersVO().getUser_num();

	    String userName = userVO.getUser_name() == null ? "" : userVO.getUser_name().trim();
	    String phone = userVO.getPhone() == null ? "" : userVO.getPhone().trim();
	    String intro = userVO.getIntro() == null ? "" : userVO.getIntro().trim();

	    if (userName.isEmpty() || phone.isEmpty()) {
	        redirectAttributes.addFlashAttribute("profileError", "닉네임과 휴대폰 번호를 입력해주세요.");
	        return "redirect:/member/myPage";
	    }

	    if (!phone.matches("^01[0-9]{8,9}$")) {
	        redirectAttributes.addFlashAttribute("profileError", "휴대폰 번호는 - 제외 숫자만 입력해주세요.");
	        return "redirect:/member/myPage";
	    }

	    if (usersService.isPhoneDuplicatedExceptUser(phone, loginUserNum)) {
	        redirectAttributes.addFlashAttribute("profileError", "이미 사용 중인 휴대폰 번호입니다.");
	        return "redirect:/member/myPage";
	    }

	    UsersVO updateUser = new UsersVO();
	    updateUser.setUser_num(loginUserNum);
	    updateUser.setUser_name(userName);
	    updateUser.setPhone(phone);
	    updateUser.setIntro(intro);

	    int updated = usersService.updateMyProfile(updateUser);

	    if (updated < 1) {
	        redirectAttributes.addFlashAttribute("profileError", "프로필 수정에 실패했습니다.");
	        return "redirect:/member/myPage";
	    }

	    principal.getUsersVO().setUser_name(userName);
	    principal.getUsersVO().setPhone(phone);
	    principal.getUsersVO().setIntro(intro);

	    redirectAttributes.addFlashAttribute("profileSuccess", "프로필이 수정되었습니다.");
	    return "redirect:/member/myPage";
	}
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/todos")
	public String todos(@AuthenticationPrincipal PrincipalDetails principal,
	                    @RequestParam(value = "sort", defaultValue = "deadline") String sort,
	                    Model model) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		String todoSort = normalizeTodoSort(sort);
		List<PersonalTodoVO> personalTodos = myPageService.getPersonalTodos(loginUserNum, todoSort);
		List<PersonalTodoVO> activeTodos = new ArrayList<>();
		List<PersonalTodoVO> completedTodos = new ArrayList<>();

		for (PersonalTodoVO todo : personalTodos) {
			if (todo.isCompleted()) {
				completedTodos.add(todo);
			} else {
				activeTodos.add(todo);
			}
		}

		addMyPageSummary(principal, model);
		model.addAttribute("todoCategories", myPageService.getTodoCategories(loginUserNum));
		model.addAttribute("activeTodos", activeTodos);
		model.addAttribute("completedTodos", completedTodos);
		model.addAttribute("todoSort", todoSort);
		model.addAttribute("todayText", LocalDate.now().toString());

		return "thviews/member/todo";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/add")
	public String addTodo(@AuthenticationPrincipal PrincipalDetails principal,
	                      @RequestParam("title") String title,
	                      @RequestParam(value = "content", required = false) String content,
	                      @RequestParam(value = "deadlineText", required = false) String deadlineText,
	                      @RequestParam(value = "todo_category_num", required = false) Long todoCategoryNum,
	                      @RequestParam(value = "priority", defaultValue = "2") int priority,
	                      @RequestParam(value = "sort", defaultValue = "deadline") String sort,
	                      RedirectAttributes redirectAttributes) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		String todoTitle = title == null ? "" : title.trim();
		String todoSort = normalizeTodoSort(sort);

		if (todoTitle.isEmpty()) {
			redirectAttributes.addFlashAttribute("todoError", "할 일 제목을 입력해주세요.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		if (priority < 1 || priority > 3) {
			priority = 2;
		}

		Date deadline = parseTodoDeadline(deadlineText);
		if (deadline != null && deadline.toLocalDate().isBefore(LocalDate.now())) {
			redirectAttributes.addFlashAttribute("todoError", "오늘보다 이전 날짜는 선택할 수 없습니다.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		PersonalTodoVO todo = new PersonalTodoVO();
		todo.setUser_num(loginUserNum);
		todo.setTitle(todoTitle);
		todo.setContent(content == null ? "" : content.trim());
		todo.setDeadline(deadline);
		todo.setTodo_category_num(todoCategoryNum);
		todo.setPriority(priority);

		myPageService.addPersonalTodo(todo);
		return "redirect:/member/todos?sort=" + todoSort;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/toggle")
	public String toggleTodo(@AuthenticationPrincipal PrincipalDetails principal,
	                         @RequestParam("todo_num") long todoNum,
	                         @RequestParam("complete") int complete,
	                         @RequestParam(value = "sort", defaultValue = "deadline") String sort) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		int nextComplete = complete == 2 ? 2 : 1;
		myPageService.updateTodoComplete(loginUserNum, todoNum, nextComplete);
		return "redirect:/member/todos?sort=" + normalizeTodoSort(sort);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/update")
	public String updateTodo(@AuthenticationPrincipal PrincipalDetails principal,
	                         @RequestParam("todo_num") long todoNum,
	                         @RequestParam("title") String title,
	                         @RequestParam(value = "content", required = false) String content,
	                         @RequestParam(value = "deadlineText", required = false) String deadlineText,
	                         @RequestParam(value = "todo_category_num", required = false) Long todoCategoryNum,
	                         @RequestParam(value = "priority", defaultValue = "2") int priority,
	                         @RequestParam(value = "sort", defaultValue = "deadline") String sort,
	                         RedirectAttributes redirectAttributes) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		String todoSort = normalizeTodoSort(sort);
		String todoTitle = title == null ? "" : title.trim();

		if (todoTitle.isEmpty()) {
			redirectAttributes.addFlashAttribute("todoError", "할 일 제목을 입력해주세요.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		if (priority < 1 || priority > 3) {
			priority = 2;
		}

		Date deadline = parseTodoDeadline(deadlineText);
		if (deadline != null && deadline.toLocalDate().isBefore(LocalDate.now())) {
			redirectAttributes.addFlashAttribute("todoError", "오늘보다 이전 날짜는 선택할 수 없습니다.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		PersonalTodoVO todo = new PersonalTodoVO();
		todo.setTodo_num(todoNum);
		todo.setUser_num(loginUserNum);
		todo.setTitle(todoTitle);
		todo.setContent(content == null ? "" : content.trim());
		todo.setDeadline(deadline);
		todo.setTodo_category_num(todoCategoryNum);
		todo.setPriority(priority);

		myPageService.updatePersonalTodo(todo);
		return "redirect:/member/todos?sort=" + todoSort;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/delete")
	public String deleteTodo(@AuthenticationPrincipal PrincipalDetails principal,
	                         @RequestParam("todo_num") long todoNum,
	                         @RequestParam(value = "sort", defaultValue = "deadline") String sort) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		myPageService.deletePersonalTodo(loginUserNum, todoNum);
		return "redirect:/member/todos?sort=" + normalizeTodoSort(sort);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/categories/add")
	public String addTodoCategory(@AuthenticationPrincipal PrincipalDetails principal,
	                              @RequestParam("category_name") String categoryName,
	                              @RequestParam(value = "color", defaultValue = "#7c5cff") String color,
	                              @RequestParam(value = "sort", defaultValue = "deadline") String sort,
	                              RedirectAttributes redirectAttributes) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		String name = categoryName == null ? "" : categoryName.trim();
		String todoSort = normalizeTodoSort(sort);

		if (name.isEmpty()) {
			redirectAttributes.addFlashAttribute("todoError", "카테고리명을 입력해주세요.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		if (myPageService.existsTodoCategoryName(loginUserNum, name)) {
			redirectAttributes.addFlashAttribute("todoError", "이미 존재하는 카테고리입니다.");
			return "redirect:/member/todos?sort=" + todoSort;
		}

		myPageService.addTodoCategory(loginUserNum, name, color);
		return "redirect:/member/todos?sort=" + todoSort;
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/todos/categories/delete")
	public String deleteTodoCategory(@AuthenticationPrincipal PrincipalDetails principal,
	                                 @RequestParam("todo_category_num") long todoCategoryNum,
	                                 @RequestParam(value = "sort", defaultValue = "deadline") String sort) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		myPageService.deleteTodoCategory(loginUserNum, todoCategoryNum);
		return "redirect:/member/todos?sort=" + normalizeTodoSort(sort);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/changePassword")
	public String changePasswordForm(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
		addMyPageSummary(principal, model);
		return "thviews/member/changePassword";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/notifications")
	public String notifications(@AuthenticationPrincipal PrincipalDetails principal, Model model) {
		long loginUserNum = principal.getUsersVO().getUser_num();

		addMyPageSummary(principal, model);
		model.addAttribute("syNotifications", notificationService.getNotificationListForPage(loginUserNum));
		model.addAttribute("syUnreadNotiCount", notificationService.getUnreadCount(loginUserNum));

		return "thviews/member/notificationManage";
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/changePassword")
	public String changePassword(@AuthenticationPrincipal PrincipalDetails principal,
	                             @RequestParam("currentPasswd") String currentPasswd,
	                             @RequestParam("newPasswd") String newPasswd,
	                             @RequestParam("confirmPasswd") String confirmPasswd,
	                             RedirectAttributes redirectAttributes) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		UsersVO dbUser = usersService.selectByUserNum(loginUserNum);

		String current = currentPasswd == null ? "" : currentPasswd.trim();
		String newPassword = newPasswd == null ? "" : newPasswd.trim();
		String confirmPassword = confirmPasswd == null ? "" : confirmPasswd.trim();

		if (dbUser == null || dbUser.getPasswd() == null || dbUser.getPasswd().isEmpty()) {
			redirectAttributes.addFlashAttribute("currentPasswordError", "비밀번호를 변경할 수 없는 계정입니다.");
			return "redirect:/member/changePassword";
		}

		if (current.isEmpty() || !passwordEncoder.matches(current, dbUser.getPasswd())) {
			redirectAttributes.addFlashAttribute("currentPasswordError", "현재 비밀번호가 틀렸습니다.");
			return "redirect:/member/changePassword";
		}

		if (!newPassword.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("confirmPasswordError", "새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.");
			return "redirect:/member/changePassword";
		}

		if (!newPassword.matches(PASSWORD_PATTERN)) {
			redirectAttributes.addFlashAttribute("newPasswordError", "비밀번호는 영문, 숫자, 특수문자(!@#$%^&*) 8~20자로 입력해주세요.");
			return "redirect:/member/changePassword";
		}

		if (passwordEncoder.matches(newPassword, dbUser.getPasswd())) {
			redirectAttributes.addFlashAttribute("newPasswordError", "새 비밀번호는 현재 비밀번호와 다르게 입력해주세요.");
			return "redirect:/member/changePassword";
		}

		String encodedPassword = passwordEncoder.encode(newPassword);
		int updated = usersService.updatePassword(dbUser.getEmail(), encodedPassword);
		if (updated < 1) {
			redirectAttributes.addFlashAttribute("currentPasswordError", "비밀번호 변경에 실패했습니다.");
			return "redirect:/member/changePassword";
		}

		principal.getUsersVO().setPasswd(encodedPassword);
		redirectAttributes.addFlashAttribute("passwordSuccess", "비밀번호 변경이 완료됐습니다.");
		return "redirect:/member/changePassword";
	}

	private void addMyPageSummary(PrincipalDetails principal, Model model) {
		long loginUserNum = principal.getUsersVO().getUser_num();
		UsersVO user = usersService.selectByUserNum(loginUserNum);

		model.addAttribute("user", user);
		model.addAttribute("teamCount", myPageService.countMyTeams(loginUserNum));
		model.addAttribute("kanbanCount", myPageService.countMyKanbanCards(loginUserNum));
		model.addAttribute("minutesCount", myPageService.countMyMinutes(loginUserNum));
	}

	private Date parseTodoDeadline(String deadlineText) {
		if (deadlineText == null || deadlineText.trim().isEmpty()) {
			return null;
		}
		try {
			return Date.valueOf(LocalDate.parse(deadlineText.trim()));
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private String normalizeTodoSort(String sort) {
		if ("priority".equals(sort) || "deadlineDesc".equals(sort) || "priorityAsc".equals(sort)) {
			return sort;
		}
		return "deadline";
	}
	
	/*============================================
	 * 프로필 사진 출력
	 *===========================================*/
	//프로필 사진 출력(로그인 전용)
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/photoView")
	public String getProfile(@AuthenticationPrincipal PrincipalDetails principal,HttpServletRequest request,Model model) {
		try {
			UsersVO user = principal.getUsersVO();
			log.debug("<<photoView>> : {}", user);
			UsersVO usersVO = usersService.selectByUserNum(1L);
			viewProfile(usersVO,request,model);
		}catch(Exception e) {
			getBasicProfileImage(request,model);
		}
		return "imageView";
	}

	//프로필 사진 출력(회원번호 지정)
	@GetMapping("/viewProfile")
	public String getProfileByMem_num(long user_num,
			HttpServletRequest request,
			Model model) {
		UsersVO usersVO = usersService.selectByUserNum(user_num);

		viewProfile(usersVO,request,model);

		return "imageView";
	}

	//프로필 사진 처리를 위한 공통 코드
	public void viewProfile(UsersVO usersVO,HttpServletRequest request, Model model) {
		if(usersVO==null || usersVO.getPhoto_name()==null) {
			//DB에 저장된 프로필 이미지가 없기 때문에 기본 이미지 호출
			getBasicProfileImage(request,model);
		}else {//업로드한 프로필 이미지 읽기
			//속성명       속성값(byte[]의 데이터)
			model.addAttribute("imageFile", usersVO.getPhoto());
			model.addAttribute("filename", usersVO.getPhoto_name());
		}
	}
	//기본 이미지 읽기
	public void getBasicProfileImage(HttpServletRequest request,Model model) {
		byte[] readbyte = FileUtil.getBytes(request.getServletContext().getRealPath("/assets/image_bundle/face.png"));
		//속성명       속성값(byte[]의 데이터)
		model.addAttribute("imageFile", readbyte);
		model.addAttribute("filename", "face.png");
	}
}
