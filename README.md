# Spring Boot Todo Webapp

A sample todo web application using **Spring Boot 2.7.18**, **Java 8**,
**Spring MVC**, **Thymeleaf**, and **Spring Data JPA / Hibernate** with an
embedded **H2** database. It renders HTML on the server without a JavaScript
framework.

## Requirements

- JDK 8 (or a newer JDK capable of compiling Java 8 source)
- Maven 3.6.3 or newer

## Copilot cloud agent environment

`.github/workflows/copilot-setup-steps.yml` installs Eclipse Temurin JDKs
**8, 11, 17, 21, and 25** and uses Maven **3** preinstalled on the
GitHub-hosted Ubuntu runner. Java 8 is the
default `JAVA_HOME` and `PATH` JDK to preserve the application's legacy baseline.
The setup verifies Maven's major version, every registered JDK's `java` and
`javac`, and the default JDK.

`actions/setup-java` registers all five JDKs in `~/.m2/toolchains.xml`, with
vendor `temurin`, versions `8`, `11`, `17`, `21`, and `25`, and IDs such as
`temurin_21`. Toolchain-aware Maven plugins can select these JDKs using the
Maven Toolchains Plugin; installing them does not change this application's
Java target or automatically enable toolchain selection in `pom.xml`.
To run Maven itself on another installed JDK, set `JAVA_HOME` to that
toolchain's `jdkHome` and prepend `$JAVA_HOME/bin` to `PATH`.

Merge the workflow into `main` to enable it for cloud-agent sessions.
Workflow changes are checked on pull requests and pushes to `main`; it can
also be run manually from **Actions > Copilot Setup Steps** once merged.
See [GitHub's environment customization guide](https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/customize-cloud-agent/customize-the-agent-environment).

## Run

```sh
mvn spring-boot:run
```

Open <http://localhost:8080>. Add tasks, mark them complete, undo completion,
and delete them. Titles are required and limited to 200 characters.

Tasks are stored in an **in-memory H2 database**, shared by all visitors, and
reset when the application restarts. Spring Boot configures the data source
and connection pool; Hibernate generates the schema from the `Todo` entity
using `spring.jpa.hibernate.ddl-auto=create-drop`. No external database setup
is required. The JDBC URL and JPA settings are configured in
`src/main/resources/application.properties`.

Open EntityManager in View is disabled: database operations run inside service
transactions rather than during template rendering. Schema creation/drop is
appropriate for this ephemeral sample, not for a persistent production database.

## Test and package

```sh
mvn test
mvn package
java -jar target/todo-webapp-0.0.1-SNAPSHOT.jar
```

Tests run against H2 and Hibernate and cover the todo lifecycle, entity
persistence and reloads, committed service transactions, input validation,
missing tasks, escaped HTML, rendered Thymeleaf pages, and static CSS. Tests
roll back database changes, except the commit test, which deletes its task
afterward.

## Continuous integration

The GitHub Actions workflow in `.github/workflows/ci.yml` runs
`mvn --batch-mode --no-transfer-progress verify` on pushes and pull requests
to `main`. It uses Temurin 8 on Ubuntu, caches Maven dependencies, and builds
the executable JAR while running all tests. It can also be started manually
from the repository's Actions tab. No deployment is configured.

## Structure

- `TodoController`: MVC routes and form validation.
- `TodoService`: transactional todo operations and title validation.
- `TodoRepository`: Spring Data JPA repository with ordered reads and
  parameterized JPQL for atomic completion toggles and deletion. Modifying
  queries flush and clear the persistence context to avoid stale entity state.
- `Todo`: JPA entity with database-generated IDs.
- `TodoForm`: validated form input.
- `src/main/resources/templates/index.html`: Thymeleaf page.
- `src/main/resources/static/css/styles.css`: responsive styling.
- `src/main/resources/application.properties`: in-memory database and Hibernate
  schema-generation configuration.

All state-changing routes use POST and successful changes redirect to the
home page to avoid accidental resubmission on refresh.

This intentionally uses the requested legacy stack. Spring Boot 2.x and Java 8
are not recommended defaults for new production systems. This sample has no
authentication or CSRF protection and is intended for local learning, not
public deployment.