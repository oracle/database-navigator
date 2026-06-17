# DBN Codebase Practices

## Repository Shape

- Product: Oracle© Database Navigator (DBN), a database development and management plugin for JetBrains IDEs.
- Main code: `src/main/java/com/dbn`.
- Main resources: `src/main/resources`, especially `META-INF/plugin.xml`, file templates, icons, text attributes, and language resources.
- Public extension modules: `modules/dbn-api` and `modules/dbn-spi`.
- For repository shape, build tooling, and validation command choices, use `dbn-development-guide`.
- Generated or bulky parser artifacts exist in language dialect packages. Avoid hand-editing generated flex/parser output unless the task explicitly targets it.

## File And Package Naming

- Root package is always `com.dbn`.
- Feature packages are nouns or feature areas: `assistant`, `connection`, `execution`, `editor`, `object`, `language`, `common`.
- Reusable infrastructure lives under `com.dbn.common`.
- Domain-specific shared abstractions often live in a `shared` package, for example `generator/code/shared` or `prerequisite/shared`.
- Managers are named `...Manager` and usually extend `ProjectComponentBase` or `ApplicationComponentBase`.
- Actions are named `...Action`, `...ToggleAction`, `...ActionGroup`, or a more specific local base. Prefer `ProjectAction`, `BasicAction`, `ToggleAction`, and package-specific abstract actions.
- UI classes are paired as `...Dialog` extending `DBNDialog<...Form>` and `...Form` extending `DBNFormBase`; IntelliJ GUI designer forms are `.form` files next to Java forms.
- Settings classes are named `...Settings`, `...SettingsForm`, `...SettingsDialog`, and extend the local configuration base classes.
- Persistent/dynamic identifiers that are not closed enums often use `PseudoConstant`, e.g. `ConnectionId`, `SchemaId`, `SessionId`.
- Database object classes use `DB...` prefixes, e.g. `DBObject`, `DBSchemaObject`, `DBTable`, `DBView`.

## Class Style

- Preserve the Oracle Apache 2.0 copyright header in Java/Kotlin/Gradle/properties files when adding new source files.
- Use JetBrains annotations heavily, especially `@NotNull` and `@Nullable`; match adjacent code for specialized annotations.
- Lombok is normal in this repo: `@Getter`, `@Setter`, `@Slf4j`, `@EqualsAndHashCode`, `@SneakyThrows`, and `@UtilityClass` are common. Match nearby usage.
- Prefer early returns over deeply nested branches.
- Prefer local DBN null/default helpers such as `nvl`, `nvln`, `coalesce`, and `Strings.isNotEmpty`.
- Static imports are common for DBN helpers and enum constants. Follow the imports in adjacent files.
- Use `var` sparingly and only where surrounding code already does or the type is obvious.

## Services And Components

- Register application and project services in `src/main/resources/META-INF/plugin.xml`.
- Project services usually:
  - extend `ProjectComponentBase`,
  - define `public static final String COMPONENT_NAME`,
  - use a private constructor accepting `Project`,
  - expose `getInstance(@NotNull Project project)` returning `projectService(project, ClassName.class)`.
- Application services usually extend `ApplicationComponentBase` and expose `applicationService(ClassName.class)`.
- Persistent services implement DBN `PersistentState` and use IntelliJ `@State`/`@Storage`, often with `DatabaseNavigator.STORAGE_FILE`.
- Subscribe to IDE or DBN events with `ProjectEvents.subscribe(...)` or `ApplicationEvents.subscribe(...)` so listeners are tied to disposal.

## Disposal And References

- Use `Checks.isValid/isNotValid`, `Failsafe.nn/nd/guarded`, and `Disposer` rather than open-coded disposal checks.
- Long-lived holders should use refs:
  - `ProjectRef` for `Project`.
  - `ConnectionRef` for `ConnectionHandler`.
  - `DBObjectRef<T>` for database objects.
  - `WeakRef<T>` for generic weak references.
- Prefer `ensure()` only when the object must exist; use nullable `get()` when absence is acceptable.
- Register child disposables with DBN `Disposer.register(parent, child)` or pass a parent into DBN base classes.

## Threading And Progress

- Use DBN thread wrappers:
  - `Background.run(...)` for pooled background work.
  - `Dispatch.run(...)` or `Dispatch.execute(...)` for UI dispatch work with modality awareness.
  - `Progress.background`, `Progress.prompt`, or `Progress.modal` for cancellable IntelliJ progress tasks.
  - `Write.run(...)` or `Write.compute(...)` for write actions.
  - `Read` helpers where adjacent code uses DBN read wrappers.
- When background work updates UI, run the compute phase in `Background`/`Progress`, then update UI through `Dispatch`.
- Always consider cancellation and disposed projects before doing expensive database or UI work.

## Exceptions And Diagnostics

- When an exception is intentionally handled without user-visible reporting, still call `Diagnostics.conditionallyLog(exception)` unless adjacent code uses a more specific DBN diagnostic helper.
- Keep cancellation handling quiet for users, but preserve diagnostic logging for troubleshooting.

## UI Forms, Dialogs, And Actions

- Use `DBNDialog<F extends DBNForm>` for modal dialogs and implement `createForm()` plus `initializeActions()`.
- Use `DBNFormBase` for forms and override hooks such as `initValidation`, `initAccessibility`, `initFieldAvailability`, `initEventListeners`, `getPreferredFocusedComponent`, and `getMainComponent`.
- Use DBN validation helpers from `DBNFormBase`/`DBNFormValidator`; do not wire ad hoc validation if the base form can handle it.
- Use `DBNHeaderForm` for forms that need a standard connection/object header.
- Use `com.dbn.common.icon.Icons` or existing IntelliJ icons in actions.
- Action `update(...)` should set presentation text/icon/enabled state and should avoid heavy work.

## Settings And Persistence

- Use local configuration base classes such as `BasicProjectConfiguration`, `CompositeProjectConfiguration`, and `ConfigurationEditorForm`.
- Read/write XML with `com.dbn.common.options.setting.Settings` helpers:
  - `newElement`, `newStateElement`, `childrenOf`.
  - `stringAttribute`, `booleanAttribute`, `enumAttribute`, `connectionIdAttribute`.
  - `setStringAttribute`, `setBooleanAttribute`, `setEnumAttribute`.
  - `readCdata` and `writeCdata` for text content that may need CDATA.
- Prefer interned IDs and DBN constant converters for persistent IDs.

## IntelliJ Plugin XML

- Keep service registrations near existing `<applicationService>` and `<projectService>` blocks.
- Extension points use DBN package-specific names: object providers, factories, management adapters, prerequisites, assistant adapters/tools, etc.
- Action IDs use `DBNavigator.Actions...` and action groups use `DBNavigator.ActionGroup...`.
- Prefer existing icon constants in XML when possible.
