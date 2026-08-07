package kr.spring.member.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.spring.member.dao.MyPageMapper;
import kr.spring.member.vo.PersonalTodoVO;
import kr.spring.member.vo.TodoCategoryVO;

@Service
@Transactional
public class MyPageServiceImpl implements MyPageService {

    @Autowired
    private MyPageMapper myPageMapper;

    @Override
    public int countMyTeams(long userNum) {
        return myPageMapper.countMyTeams(userNum);
    }

    @Override
    public int countMyKanbanCards(long userNum) {
        return myPageMapper.countMyKanbanCards(userNum);
    }

    @Override
    public int countMyMinutes(long userNum) {
        return myPageMapper.countMyMinutes(userNum);
    }

    @Override
    public List<TodoCategoryVO> getTodoCategories(long userNum) {
        return myPageMapper.selectTodoCategories(userNum);
    }

    @Override
    public boolean existsTodoCategoryName(long userNum, String categoryName) {
        return myPageMapper.countTodoCategoryByName(userNum, categoryName) > 0;
    }

    @Override
    public List<PersonalTodoVO> getPersonalTodos(long userNum, String sort) {
        return myPageMapper.selectPersonalTodos(userNum, sort);
    }

    @Override
    public void addTodoCategory(long userNum, String categoryName, String color) {
        TodoCategoryVO category = new TodoCategoryVO();
        category.setUser_num(userNum);
        category.setCategory_name(categoryName);
        category.setColor(color);
        myPageMapper.insertTodoCategory(category);
    }

    @Override
    public int deleteTodoCategory(long userNum, long todoCategoryNum) {
        return myPageMapper.deleteTodoCategory(todoCategoryNum, userNum);
    }

    @Override
    public void addPersonalTodo(PersonalTodoVO todo) {
        myPageMapper.insertPersonalTodo(todo);
    }

    @Override
    public int updateTodoComplete(long userNum, long todoNum, int complete) {
        return myPageMapper.updateTodoComplete(todoNum, userNum, complete);
    }

    @Override
    public int updatePersonalTodo(PersonalTodoVO todo) {
        return myPageMapper.updatePersonalTodo(todo);
    }

    @Override
    public int deletePersonalTodo(long userNum, long todoNum) {
        return myPageMapper.deletePersonalTodo(todoNum, userNum);
    }
}
