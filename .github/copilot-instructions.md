# Repository context

`continuous-appmod` contains a sample Java 8 todo application built with Spring Boot 2.7.18, Spring MVC, Thymeleaf, Spring Data JPA, Hibernate, and an in-memory H2 database. This deliberately legacy stack provides a baseline for later modernization.

## Application commands and architecture

- Run locally: `mvn spring-boot:run`, then open `http://localhost:8080`.
- Build and run all tests: `mvn verify`.
- Run a single test class: `mvn -Dtest=TodoServiceTest test`.
- Run the packaged application: `java -jar target/todo-webapp-0.0.1-SNAPSHOT.jar`.

Use Java 8-compatible syntax and APIs and `javax.persistence` / `javax.validation` imports for the current Spring Boot 2.x baseline.

`TodoController` validates `TodoForm` input and delegates to transactional `TodoService` methods. `TodoRepository` persists the `Todo` JPA entity; modifying JPQL queries perform atomic completion toggles and deletion and flush/clear the persistence context. Thymeleaf renders `templates/index.html`, with CSS under `static/css/`.

Hibernate generates the H2 schema with `ddl-auto=create-drop`; tasks disappear on restart. Open EntityManager in View is disabled. Tests use isolated in-memory databases and roll back changes, except the commit test, which explicitly cleans up its task.

Update these instructions as the implementation evolves. Use the implementation and project documentation as the source of truth.

## Copilot cloud agent environment

`.github/workflows/copilot-setup-steps.yml` uses Maven 3 preinstalled on the GitHub-hosted Ubuntu runner and installs Eclipse Temurin JDKs 8, 11, 17, 21, and 25. Java 8 is the default `JAVA_HOME` and `PATH` JDK for the current baseline. All five JDKs are registered in `~/.m2/toolchains.xml` with vendor `temurin`, major-version values (including `8`, not `1.8`), and IDs such as `temurin_21`.

Use toolchain-aware Maven plugins when selecting a different build JDK. To change Maven's own runtime, set `JAVA_HOME` to the selected toolchain's `jdkHome` and prepend `$JAVA_HOME/bin` to `PATH`. Do not change the application's Java target just because newer JDKs are installed.
