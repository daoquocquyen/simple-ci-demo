# simple-ci-demo

A simple Spring Boot demo project.

## Prerequisites

- Java 17 (JDK)
- Maven 3.9.6+

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Test
- AssertJ (via Spring Boot Starter Test)

## Build

```sh
mvn clean package
```

## Run

```sh
mvn spring-boot:run
```
or
```sh
java -jar target/simple-ci-demo-0.1.0-SNAPSHOT.jar
```

## Code Quality Checks
```bash
mvn pmd:check spotbugs:check checkstyle:check
```

## Test

### Unit Tests

```sh
mvn test
```

### Unit, Integration Tests and Code Coverage

```sh
mvn verify
```
Note:
* It will fail if code coverage < 80%.
* Test reports are generated in `target/surefire-reports/` and `target/failsafe-reports/`.
* JaCoCo code coverage report is available

## Project Structure

- `src/main/java` - Application source code
- `src/test/java` - Unit and integration tests
- `pom.xml` - Maven build configuration

## API Endpoints

- `GET /hello?name=Bob`
  Returns: `{"message": "Hello, Bob!"}`

- `POST /hello/sum`
  Request body: `{"a": 2, "b": 5}`
  Returns: `{"result": 7}`

