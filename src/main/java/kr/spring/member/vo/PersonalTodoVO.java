package kr.spring.member.vo;

import java.sql.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class PersonalTodoVO {
    private long todo_num;
    private long user_num;
    private Long todo_category_num;
    private String title;
    private String content;
    private Date deadline;
    private int priority = 2;
    private int complete = 1;

    private String category_name;
    private String category_color;

    public boolean isCompleted() {
        return complete == 2;
    }

    public String getPriorityLabel() {
        if (priority == 3) return "높음";
        if (priority == 1) return "낮음";
        return "보통";
    }

    public String getPriorityClass() {
        if (priority == 3) return "high";
        if (priority == 1) return "low";
        return "normal";
    }

    public String getDeadlineText() {
        return deadline == null ? "" : deadline.toString();
    }

    public String getCategoryColor() {
        if (category_color == null || category_color.isBlank()) {
            return "#7c5cff";
        }
        return category_color;
    }
}
