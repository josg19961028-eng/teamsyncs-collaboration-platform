package kr.spring.member.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import kr.spring.member.vo.PersonalTodoVO;
import kr.spring.member.vo.TodoCategoryVO;

@Mapper
public interface MyPageMapper {

    int countMyTeams(long userNum);

    int countMyKanbanCards(long userNum);

    int countMyMinutes(long userNum);

    List<TodoCategoryVO> selectTodoCategories(long userNum);

    int countTodoCategoryByName(@Param("userNum") long userNum,
                                @Param("categoryName") String categoryName);

    List<PersonalTodoVO> selectPersonalTodos(@Param("userNum") long userNum,
                                             @Param("sort") String sort);

    void insertTodoCategory(TodoCategoryVO category);

    int deleteTodoCategory(@Param("todoCategoryNum") long todoCategoryNum,
                           @Param("userNum") long userNum);

    void insertPersonalTodo(PersonalTodoVO todo);

    int updateTodoComplete(@Param("todoNum") long todoNum,
                           @Param("userNum") long userNum,
                           @Param("complete") int complete);

    int updatePersonalTodo(PersonalTodoVO todo);

    int deletePersonalTodo(@Param("todoNum") long todoNum,
                           @Param("userNum") long userNum);
}
