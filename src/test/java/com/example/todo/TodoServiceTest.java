package com.example.todo;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TodoServiceTest {

    @Autowired
    private TodoService service;

    @Autowired
    private EntityManager entityManager;

    @Test
    void addsTrimsAndPreservesInsertionOrder() {
        Todo first = service.add("  First task  ");
        Todo second = service.add("Second task");

        assertThat(first.getTitle()).isEqualTo("First task");
        assertThat(first.isCompleted()).isFalse();
        assertThat(second.getId()).isGreaterThan(first.getId());
        assertThat(service.findAll()).extracting(Todo::getId)
                .containsExactly(first.getId(), second.getId());
        assertThat(service.findAll()).extracting(Todo::getTitle)
                .containsExactly("First task", "Second task");
    }

    @Test
    void togglesCompletionWithoutMutatingExistingSnapshots() {
        Todo original = service.add("Task");
        service.toggle(original.getId());
        assertThat(service.findAll().get(0).isCompleted()).isTrue();
        assertThat(original.isCompleted()).isFalse();

        service.toggle(original.getId());
        assertThat(service.findAll().get(0).isCompleted()).isFalse();
    }

    @Test
    void deletesTasksAndDoesNotReuseIds() {
        Todo first = service.add("First");
        service.delete(first.getId());
        assertThat(service.findAll()).isEmpty();
        assertThat(service.add("Second").getId()).isGreaterThan(first.getId());
    }

    @Test
    void rejectsInvalidTitlesAndAcceptsMaximumLength() {
        assertThatThrownBy(() -> service.add(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.add(repeat('a', 201))).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.findAll()).isEmpty();
        assertThat(service.add(repeat('a', 200)).getTitle()).hasSize(200);
    }

    @Test
    void storesTasksAndReloadsTheirStateThroughHibernate() {
        Todo todo = service.add("Don't concatenate SQL'); DROP TABLE todos; --");
        entityManager.flush();
        entityManager.clear();
        Todo reloaded = entityManager.find(Todo.class, todo.getId());
        assertThat(reloaded).isNotSameAs(todo);
        assertThat(reloaded.getTitle()).isEqualTo(todo.getTitle());
        assertThat(reloaded.isCompleted()).isFalse();

        service.toggle(todo.getId());
        assertThat(entityManager.find(Todo.class, todo.getId()).isCompleted()).isTrue();
        service.delete(todo.getId());
        assertThat(entityManager.find(Todo.class, todo.getId())).isNull();
        assertThat(service.findAll()).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void commitsChangesAcrossSeparateServiceTransactions() {
        Todo todo = service.add("Committed task");
        try {
            assertThat(entityManager.find(Todo.class, todo.getId()).getTitle())
                    .isEqualTo("Committed task");
            service.toggle(todo.getId());
            assertThat(entityManager.find(Todo.class, todo.getId()).isCompleted()).isTrue();
            service.toggle(todo.getId());
            assertThat(entityManager.find(Todo.class, todo.getId()).isCompleted()).isFalse();
        } finally {
            service.delete(todo.getId());
        }
        assertThat(entityManager.find(Todo.class, todo.getId())).isNull();
    }

    @Test
    void returnsNotFoundForUnknownTasks() {
        assertThatThrownBy(() -> service.toggle(99))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.delete(99))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private String repeat(char character, int count) {
        char[] characters = new char[count];
        java.util.Arrays.fill(characters, character);
        return new String(characters);
    }
}
