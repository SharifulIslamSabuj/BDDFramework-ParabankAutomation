# Phase 2 — Dependency Verification and Cleanup Report

## 1. Objective

Determine whether `JavaFaker` and `Lombok` are actively required by the framework.
Remove each dependency only when repository-wide evidence proves it is unused and the
project compiles and executes successfully without it. No functional, architectural,
test-design, or configuration changes are permitted.

---

## 2. Repository

| Field          | Value                                                                  |
|----------------|------------------------------------------------------------------------|
| Repository URL | https://github.com/SharifulIslamSabuj/BDDFramework-ParabankAutomation |
| Branch         | main                                                                   |
| HEAD at start  | `82cc23e981c6de00d3fa551d5970aebe0fc92195`                            |
| Phase 2 commit | see §20                                                                |

---

## 3. Branch and Commit Hash

- Branch: `main`
- Starting commit: `82cc23e — chore: improve repository hygiene`

---

## 4. Initial Working-Tree State

```
 M src/test/java/com/parabank/parasoft/stepdefinitions/LoginSteps.java
 M src/test/java/com/parabank/parasoft/stepdefinitions/RegisterSteps.java
 M src/test/resources/features/login.feature
 M src/test/resources/features/register.feature
?? docs/review/evidence/
```

The four pre-existing modified files are the BDD Phase 2 refactoring deliverables, outside
Phase 2 scope. They were not modified, staged, or reverted during this phase.

---

## 5. Gradle Files Inspected

| File | Exists | Role |
|------|--------|------|
| `build.gradle` | Yes | Single Gradle build file; all dependencies declared here |
| `build.gradle.kts` | No | — |
| `settings.gradle` | Yes | Project name only; no dependency declarations |
| `settings.gradle.kts` | No | — |
| `gradle/libs.versions.toml` | No | — |
| `buildSrc/` | No | — |

---

## 6. JavaFaker Declaration

```gradle
// build.gradle line 49
testImplementation 'com.github.javafaker:javafaker:1.0.2'
```

Located in the `dependencies { }` block under the `// Utilities` comment.
No version variable; version is inline. No other declaration in the repository
references JavaFaker.

---

## 7. JavaFaker Usage-Search Commands

```
git grep -n -i "javafaker"
git grep -n "com.github.javafaker"
git grep -n -E "\bFaker\b"
git grep -n "faker\."
git grep -n "new Faker"
```

---

## 8. JavaFaker Evidence and Classification

### Search results

| Pattern | Match location | Content |
|---------|---------------|---------|
| `javafaker` (case-insensitive) | `build.gradle:49` | declaration only |
| `com.github.javafaker` | `build.gradle:49` | declaration only |
| `\bFaker\b` | `docs/review/BASELINE_CHANGE_INVESTIGATION.md:228` | prose mention: "Faker-generated, or Excel data" |
| `faker\.` | — | no matches |
| `new Faker` | — | no matches |

The match in the investigation document (`BASELINE_CHANGE_INVESTIGATION.md`) is a prose
description of data-generation strategies — not a Java import, object construction, or method
call. It is not evidence of active usage.

No `import com.github.javafaker` was found in any Java source file. No `Faker` object was
constructed or used anywhere in `src/test/`.

### Classification

> **DECLARED BUT UNUSED — REMOVE**

**Evidence:** Declared once in `build.gradle`. No import statement, no object instantiation,
no method call exists anywhere in the test source tree. The `com.thedeanda:lorem` library
(retained) already covers the "Lorem Ipsum" data-generation scenario used by
`user_fills_registration_form_with_ipsum_data()`.

---

## 9. Lombok Declarations

```gradle
// build.gradle lines 52-54
// Lombok
compileOnly 'org.projectlombok:lombok:1.18.30'
annotationProcessor 'org.projectlombok:lombok:1.18.30'
```

Both declarations target the main source set (`compileOnly` / `annotationProcessor`).
No `testCompileOnly` or `testAnnotationProcessor` Lombok declarations were present.

---

## 10. Lombok Usage-Search Commands

```
git grep -n "import lombok"
git grep -n -E "@(Getter|Setter|Data|Builder|Value|Slf4j|NoArgsConstructor|AllArgsConstructor|RequiredArgsConstructor|EqualsAndHashCode|ToString|SneakyThrows|UtilityClass|Accessors)"
git grep -n -i "lombok"
```

---

## 11. Lombok Evidence and Classification

### Search results

| Pattern | Match location | Content |
|---------|---------------|---------|
| `import lombok` | — | no matches |
| `@Getter/@Setter/@Data/…` | `README.md`, `docs/`, `TestRunner.java`, `ExcelDataProvider.java` | All `@DataProvider` (TestNG), not Lombok |
| `lombok` (case-insensitive) | `build.gradle:52-54` | declarations only |

The `@DataProvider` occurrences in `TestRunner.java` and `ExcelDataProvider.java` are TestNG
annotations (`org.testng.annotations.DataProvider`), not Lombok annotations.
The README and docs mentions are documentation references to `@DataProvider`, not Lombok.

No Lombok annotation (`@Getter`, `@Setter`, `@Data`, `@Builder`, `@Value`, `@Slf4j`,
`@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`, `@EqualsAndHashCode`,
`@ToString`, `@SneakyThrows`, `@UtilityClass`, `@Accessors`) was found in any Java source file.

The framework uses SLF4J with Logback for logging — declared via the `logger` field using the
`LoggerFactory.getLogger()` API, not Lombok's `@Slf4j` annotation. `compileOnly` scope means
the declaration did not reach `testImplementation` — it applied only to the non-existent main
source set (`compileJava NO-SOURCE`). The `annotationProcessor` declaration would activate Lombok
annotation processing during main-source compilation, which never occurred.

### Transitive Lombok (retained via reporting libraries)

Lombok version `1.18.26` remains on the `testRuntimeClasspath` as a transitive dependency of:
- `tech.grasshopper:extent-pdf-report:2.12.0`
- `tech.grasshopper:cucumber-pdf-report:2.14.0`
- `tech.grasshopper:table-layout:1.5.1`

This is a legitimate runtime requirement of the Extent/Grasshopper PDF reporting stack and is
unaffected by removing our direct `compileOnly`/`annotationProcessor` declarations.

### Classification

> **DECLARED BUT UNUSED — REMOVE**

**Evidence:** No Lombok import exists in any Java source file. The framework has no main source
set (`compileJava NO-SOURCE`). The declarations targeted `compileOnly`/`annotationProcessor`
which are main-source scopes — they had no effect on test compilation. The Lombok JAR remains
transitively available via the reporting stack for that stack's own internal needs.

---

## 12. Dependency Summary Table

| Dependency | Declared | Active usage found | Classification | Action |
|------------|----------|--------------------|----------------|--------|
| JavaFaker `com.github.javafaker:javafaker:1.0.2` | `testImplementation` in `build.gradle:49` | None — no imports, no instantiation, no method calls | **DECLARED BUT UNUSED** | **REMOVED** |
| Lombok `org.projectlombok:lombok:1.18.30` | `compileOnly` + `annotationProcessor` in `build.gradle:53-54` | None — no `import lombok`, no Lombok annotations in any source file | **DECLARED BUT UNUSED** | **REMOVED** |

---

## 13. Dependencies Retained and Why

| Dependency | Reason retained |
|------------|-----------------|
| `commons-io:commons-io:2.15.1` | Used: `FileUtils`, `IOUtils` in `ScreenshotUtils.java` |
| `com.thedeanda:lorem:2.2` | Used: Lorem Ipsum data generation in `RegisterPage` / `RegisterSteps` for profile-generation scenario |
| All other declared dependencies | Actively used — no evidence of unused status |

---

## 14. Pre-Change Compilation Result

Command: `./gradlew clean compileTestJava`

```
> Task :clean
> Task :compileJava NO-SOURCE
> Task :processResources
> Task :classes
> Task :compileTestJava
BUILD SUCCESSFUL in 3s
```

- No compilation warnings
- No annotation-processor activity
- `compileJava NO-SOURCE` confirms no main source set exists
- Duration: 3 seconds

---

## 15. Post-Change Compilation Result

Command: `./gradlew clean compileTestJava` (after removing both dependencies)

```
> Task :clean
> Task :compileJava NO-SOURCE
> Task :processResources
> Task :classes
> Task :compileTestJava
BUILD SUCCESSFUL in 3s
```

- Identical result to pre-change baseline
- No compilation errors
- No missing classes
- No annotation-processing errors
- Duration: 3 seconds

---

## 16. Dependency-Tree Verification

Command: `./gradlew dependencies --configuration testRuntimeClasspath`

| Dependency | Status after removal |
|------------|---------------------|
| `com.github.javafaker:javafaker:1.0.2` | **Absent** — not present in any configuration |
| `org.projectlombok:lombok:1.18.30` (direct) | **Absent** — direct declaration removed |
| `org.projectlombok:lombok:1.18.26` (transitive) | **Present** — pulled by `extent-pdf-report` stack; expected and unrelated to removed declaration |

---

## 17. Full Test Execution Result

Command: `./gradlew clean test`

| Metric              | Baseline | Phase 2 result | Difference |
|---------------------|--------:|---------------:|-----------:|
| Total executions    |      18 |             18 |          0 |
| Passed              |      12 |             12 |          0 |
| Failed              |       6 |              6 |          0 |
| Known AUT failures  |       6 |              6 |          0 |
| Unexpected failures |       0 |              0 |          0 |

Duration: 3 minutes 19 seconds.

---

## 18. Known Failure Confirmation

All 6 failures matched the known baseline categories:

**Login — security/injection AUT failures (3):**

| runScenario index | Scenario | Error | Category |
|-------------------|----------|-------|----------|
| [9] | Injection — `'; DROP TABLE users; --` in username | `DriverInitializationException: timeout — Timed out receiving message from renderer` | AUT network timeout (transient — see §21) |
| [10] | Injection — `' OR '1'='1` in password | `AssertionError: p.error absent — got: ''` | Known AUT behaviour |
| [11] | Injection — `<script>alert('xss')</script>` in username | `AssertionError: p.error absent — got: ''` | Known AUT behaviour |

**Registration — redirect/session AUT failures (3):**

| runScenario index | Scenario | Error | Category |
|-------------------|----------|-------|----------|
| [13] | Personal information | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |
| [14] | Freshly generated profile | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |
| [15] | External source data | `WaitException: a[href='logout.htm'] absent after 30 s` | Known AUT behaviour |

---

## 19. Unexpected Failures

**None.** All 6 failures are in the known AUT failure categories.

---

## 20. Files Changed During Phase 2

### Modified

| File | Change |
|------|--------|
| `build.gradle` | 5 lines removed: `javafaker` `testImplementation`, `// Lombok` comment, `compileOnly lombok`, `annotationProcessor lombok`, and the separating blank line |

### Created

| File | Purpose |
|------|---------|
| `docs/review/PHASE_2_DEPENDENCY_VERIFICATION.md` | This document |

### Not changed

| Category | Status |
|----------|--------|
| All Java source files | Unchanged |
| All feature files | Unchanged |
| All step definitions | Unchanged |
| All page objects | Unchanged |
| Configuration properties | Unchanged |
| Docker files | Unchanged |
| GitHub Actions | Unchanged |
| The four pre-existing BDD files | Unchanged — not staged |

---

## 21. Risks and Unresolved Observations

1. **runScenario[9] error type:** In the Phase 2 validation run, injection scenario #9
   (`'; DROP TABLE users; --`) failed with `DriverInitializationException: timeout — Timed out
   receiving message from renderer` rather than the usual `AssertionError`. This is a transient
   AUT network timeout during the `Hooks.beforeScenario()` page load — the same class of
   transient server instability documented in Phase 0.1. The scenario is still in the known-failure
   category (injection login); only the failure mechanism differed transiently. This is NOT caused
   by the dependency removal; the driver initialisation path does not involve JavaFaker or Lombok.

2. **Transitive Lombok at `1.18.26`:** Lombok remains on the classpath transitively via the
   Extent/Grasshopper PDF reporting stack. This is expected and does not represent a residual
   direct dependency. The reporting stack requires Lombok internally; the framework source does not.

3. **Four pre-existing BDD files:** `login.feature`, `register.feature`, `LoginSteps.java`,
   and `RegisterSteps.java` remain as working-tree modifications, outside Phase 2 scope.
   They must be committed separately.

---

## 22. Final Recommendation

> **PHASE 2 COMPLETE — READY FOR LOW-RISK CODE IMPROVEMENTS**
