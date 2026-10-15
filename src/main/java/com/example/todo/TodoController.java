package com.example.todo;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("todoForm", new TodoForm());
        return showTodos(model);
    }

    @PostMapping("/todos")
    public String add(@Valid @ModelAttribute("todoForm") TodoForm form,
                      BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return showTodos(model);
        }
        todoService.add(form.getTitle());
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/toggle")
    public String toggle(@PathVariable long id) {
        todoService.toggle(id);
        return "redirect:/";
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@PathVariable long id) {
        todoService.delete(id);
        return "redirect:/";
    }

    private String showTodos(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "index";
    }
}
