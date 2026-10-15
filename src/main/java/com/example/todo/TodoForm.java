package com.example.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TodoForm {

    @NotBlank(message = "Enter a task title.")
    @Size(max = 200, message = "Keep the task title to 200 characters or fewer.")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }
}
