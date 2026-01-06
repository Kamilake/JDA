# JDA (Java Discord API) - Copilot Instructions

## Project Overview
JDA is a Java library for building Discord bots using the Discord Gateway and REST API. The codebase follows a clear separation between **public API** (`net.dv8tion.jda.api`) and **internal implementation** (`net.dv8tion.jda.internal`).

## Architecture

### Package Structure
- `api/` - Public interfaces and classes for library consumers (documented in Javadocs)
- `internal/` - Implementation details, not exposed to users (excluded from Javadocs)
- `annotations/` - Custom annotations like `@ReplaceWith`, `@Incubating`

### Key Design Patterns

**Entity Mixins** ([internal/entities/channel/mixin/README.md](src/main/java/net/dv8tion/jda/internal/entities/channel/mixin/README.md)):
- Code reuse through composition via `*Mixin` interfaces
- Expose internal state setters without leaking to public API
- Mixins provide `default` implementations using State Accessors and Mixin Hooks

**RestAction Pattern** ([api/requests/RestAction.java](src/main/java/net/dv8tion/jda/api/requests/RestAction.java)):
- All Discord API calls return `RestAction<T>` for async execution
- Execute with: `queue()` (async), `complete()` (blocking), or `submit()` (CompletableFuture)
- Always use `@CheckReturnValue` on methods returning RestAction

**Event System**:
- Extend `ListenerAdapter` and override specific `on*` methods
- Events flow: Gateway → `SocketHandler` → `Event` subclass → `IEventManager` → listeners

## Code Conventions

### Nullability Annotations
Use JSR-305 annotations consistently:
```java
@Nonnull                    // Parameter/return cannot be null
@Nullable                   // May be null
@CheckReturnValue           // Return value must not be ignored (RestActions)
```

### Input Validation
Use `Checks` utility class for parameter validation:
```java
Checks.notNull(param, "ParamName");
Checks.notEmpty(collection, "CollectionName");
Checks.isSnowflake(id, "ID");
Checks.check(condition, "Error message");
```

### Data Handling
Use `DataObject` and `DataArray` for JSON serialization (not raw Jackson):
```java
DataObject.empty().put("key", value);
DataObject.fromJson(jsonString);
```

## Build & Development

### Commands
```bash
./gradlew build              # Full build with tests
./gradlew format             # Run spotless + rewrite formatting
./gradlew test               # Run tests
./gradlew updateTestSnapshots # Update test snapshots
./gradlew javadoc            # Generate documentation
```

### Java Version Requirements
- **Compile target**: Java 8 (bytecode version 52)
- **Toolchain**: Java 25 with Adoptium vendor
- Java 8 compatibility tests run in `src/test-java8/`

### Testing Patterns
- Tests use JUnit 5 + Mockito + AssertJ
- `IntegrationTest` base class provides JDA mocking infrastructure
- Snapshot testing: Use `assertThatRequestFrom(action).hasBodyMatchingSnapshot()`
- Run tests with GMT timezone, en_US locale

### Formatting
- Palantir Java format for code
- Apache 2.0 license header required (see `gradle/copyright-header.txt`)
- OpenRewrite recipes for automated refactoring (`rewrite.yml`)

## Key Integration Points

### Entry Points
- `JDABuilder` - Single bot instance creation
- `DefaultShardManagerBuilder` - Sharded bot setup
- Configure with `GatewayIntent`, `CacheFlag`, `MemberCachePolicy`

### Examples
Reference implementations in `src/examples/java/`:
- `SlashBotExample.java` - Slash commands and buttons
- `AudioEchoExample.java` - Voice/audio handling
- `MessageLoggerExample.java` - Message events

## Important Notes
- Audio features require `opus-java` and `tink` dependencies
- Rate limiting handled automatically by `SequentialRestRateLimiter`
- All internal classes may change without notice - never expose them in public API
