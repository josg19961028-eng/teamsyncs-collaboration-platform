package kr.spring.member.service;

import java.util.List;

import kr.spring.member.vo.PersonalTodoVO;
import kr.spring.member.vo.TodoCategoryVO;

public interface MyPageService {

    int countMyTeams(long userNum);

    int countMyKanbanCards(long userNum);

    int countMyMinutes(long userNum);

    List<TodoCategoryVO> getTodoCategories(long userNum);

    boolean existsTodoCategoryName(long userNum, String categoryName);

    List<PersonalTodoVO> getPersonalTodos(long userNum, String sort);

    void addTodoCategory(long userNum, String categoryName, String color);

    int deleteTodoCategory(long userNum, long todoCategoryNum);

    void addPersonalTodo(PersonalTodoVO todo);

    int updateTodoComplete(long userNum, long todoNum, int complete);

    int updatePersonalTodo(PersonalTodoVO todo);

    int deletePersonalTodo(long userNum, long todoNum);
}
