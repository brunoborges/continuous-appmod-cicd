package com.example.todo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TodoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TodoService service;

    @BeforeEach
    void clearTasks() {
        for (Todo todo : service.findAll()) {
            service.delete(todo.getId());
        }
    }

    @Test
    void rendersEmptyPageAndStylesheet() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("No tasks yet.")));
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk());
    }

    @Test
    void addsCompletesUndoesAndDeletesTask() throws Exception {
        mvc.perform(post("/todos").param("title", "  Buy milk  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
        Todo todo = service.findAll().get(0);
        assertThat(todo.getTitle()).isEqualTo("Buy milk");

        mvc.perform(get("/")).andExpect(content().string(containsString("Buy milk")));
        mvc.perform(post("/todos/{id}/toggle", todo.getId()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(service.findAll().get(0).isCompleted()).isTrue();
        mvc.perform(get("/")).andExpect(content().string(containsString(">Undo</button>")));
        mvc.perform(post("/todos/{id}/toggle", todo.getId()))
                .andExpect(status().is3xxRedirection());
        assertThat(service.findAll().get(0).isCompleted()).isFalse();
        mvc.perform(post("/todos/{id}/delete", todo.getId()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"));
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void showsValidationErrorsWithoutAddingTasks() throws Exception {
        mvc.perform(post("/todos").param("title", "   "))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("todoForm", "title"))
                .andExpect(content().string(containsString("Enter a task title.")));
        char[] longTitle = new char[201];
        java.util.Arrays.fill(longTitle, 'a');
        mvc.perform(post("/todos").param("title", new String(longTitle)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("todoForm", "title"))
                .andExpect(content().string(containsString("200 characters or fewer")));
        mvc.perform(post("/todos"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("todoForm", "title"));
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    void escapesTaskTitles() throws Exception {
        service.add("<script>alert('test')</script>");
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))));
    }

    @Test
    void rejectsUnknownIdsAndGetMutations() throws Exception {
        mvc.perform(post("/todos/999/toggle")).andExpect(status().isNotFound());
        mvc.perform(post("/todos/999/delete")).andExpect(status().isNotFound());
        mvc.perform(get("/todos/999/delete")).andExpect(status().isMethodNotAllowed());
    }
}
