package kr.spring.member.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class TodoCategoryVO {
    private long todo_category_num;
    private long user_num;
    private String category_name;
    private String color;

    public String getDisplayColor() {
        if (color == null || color.isBlank()) {
            return "#7c5cff";
        }
        return color;
    }
}
