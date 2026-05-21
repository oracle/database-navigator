# DBN Shared Utilities

The DBN shared utility layer is mostly under `com.dbn.common`. There is no obvious top-level `shard` package in this repo; if a user says "shard utilities", interpret it as "shared utilities" unless the task introduces real shard-specific code.

## Service And Component Access

- `Components.projectService(project, ServiceClass.class)` and `Components.applicationService(ServiceClass.class)` centralize IntelliJ service lookup and eager-service compatibility.
- `ProjectComponentBase` stores a `ProjectRef`, exposes `getProject()`, and integrates with DBN disposal/notifications.
- `ApplicationComponentBase` is the application-service counterpart.
- `PersistentState` is DBN's local persistent-state contract used with IntelliJ `@State`.

## Disposal, Validity, And Failsafe

- `Checks.isValid(...)` / `Checks.isNotValid(...)`: use before accessing projects, disposables, PSI, DB objects, or UI state that may be gone.
- `Failsafe.nn(value)`: require non-null or throw DBN's disposed-style exception.
- `Failsafe.nd(value)`: require non-disposed/non-invalid value.
- `Failsafe.guarded(...)`: wrap code that may hit cancellation, invalid state, or disposed objects; returns defaults where appropriate.
- `Disposer.register(parent, child)`: register with IntelliJ disposer through DBN safeguards.
- `Disposer.dispose(...)`: dispose individual objects, collections, maps, arrays, timers, and DB virtual files safely.
- `StatefulDisposableBase`: common base for DBN disposables with disposed state.

## References And IDs

- `WeakRef<T>`: generic weak reference with `of`, `get`, and `ensure`.
- `ProjectRef`: cached project reference in project user data.
- `ConnectionRef`: stable reference by `ConnectionId` that can lazily re-resolve a connection handler.
- `DBObjectRef<T>`: serializable/reference form for database objects; prefer it over holding DB object instances in long-lived state.
- `PseudoConstant<T>`: enum-like values with dynamic IDs, used by connection/session/schema IDs and other persisted constants.
- `Constant` and `PseudoConstantConverter`: use for persistence and conversion of DBN constants.

## Threading And Modality

- `Background.run(...)`: submit work to DBN background executor with copied thread context.
- `Dispatch.run(...)`: invoke later on EDT, with overloads for `Component`, `DataContext`, `ModalityState`, and conditional current-thread execution.
- `Dispatch.execute(...)`: invoke and wait on EDT with modality awareness.
- `Dispatch.async(component, supplier, consumer)`: compute off the EDT and render back when the component is showing.
- `Progress.background(...)`, `Progress.prompt(...)`, `Progress.modal(...)`: schedule IntelliJ progress tasks with DBN thread context.
- `Progress.cancelCallback(...)`, `installThreadInterrupter(...)`, `installProgressListener(...)`: connect cancellation to running work.
- `Write.run(project, runnable)` and `Write.compute(...)`: use for write actions and undo-aware write commands.

## Events

- `ProjectEvents.subscribe(project, parentDisposable, topic, listener)`: subscribe project-scoped listeners tied to disposal.
- `ProjectEvents.notify(project, topic, callback)`: notify project topic listeners.
- `ApplicationEvents.subscribe(...)`: application-scoped equivalent.
- DBN listener wrappers such as `DBNFileEditorManagerListener` provide narrower callback hooks.

## Collections, Strings, And Defaults

- `Commons.nvl`, `Commons.nvln`, `Commons.coalesce`: DBN defaulting and fallback helpers.
- `Commons.match` and `matchArrays`: null-safe equality patterns used across settings/state.
- `Strings`: string checks, tokenization, case caching, identifier checks, HTML stripping, wrapping, parsing.
- `Lists`: filtering, conversion, first/last matching, count/all/any/none, bounds-safe access, CSV helpers.
- `Maps`, `CollectionUtil`, and DBN collection classes: check before adding generic collection glue.
- `Conditional.when(...)`: local conditional execution/readability helper.
- `Safe` and `Unsafe`: centralized guarded casts or operations. Use carefully and only when adjacent code expects them.

## Settings XML

- Use `Settings` for JDOM persistence:
  - Read: `stringAttribute`, `booleanAttribute`, `integerAttribute`, `enumAttribute`, `constantAttribute`, `connectionIdAttribute`, `schemaIdAttribute`, `sessionIdAttribute`.
  - Write: `setStringAttribute`, `setBooleanAttribute`, `setIntegerAttribute`, `setEnumAttribute`, `setConstantAttribute`.
  - Structure: `newElement`, `newStateElement`, `childrenOf`.
  - Text: `readCdata`, `writeCdata`, `needsCdataWrapping`.
- Avoid ad hoc `element.getAttributeValue(...)` unless surrounding code already does and no typed helper exists.

## UI And UX Helpers

- `DBNFormBase`, `DBNForm`, `DBNHeaderForm`, `DBNHintForm`: standard form lifecycle, validation, accessibility, field availability, and binding.
- `DBNDialog`: standard dialog wrapping, action initialization, title signing, sizing persistence, form validation, project/connection user data, and disposal.
- `DBNComponent` / `DBNComponentBase`: component lifecycle and parent/project linkage.
- `TextFields`, `ComboBoxes`, `Accessibility`, `Borders`, `UserInterface`, `Decorators`, `Popups`, `Trees`: prefer these over hand-rolled Swing wiring when available.
- `Icons`: central icon registry used heavily by actions and UI.

## User-Facing Text And Notifications

- `NlsResources.txt(...)`: primary NLS accessor. Use static import when nearby code does.
- `Messages`: DBN message dialogs and options.
- `Dialogs`: dialog callbacks and helper APIs.
- `NotificationSupport` and `NotificationCategory`: notification integration.
- `MessageBundle`, `TitledMessageBundle`, and collectors: structured multi-message reporting.

## Files, Editors, PSI, And Navigation

- `Files`, `VirtualFiles`, `FileSearchRequest`, `Documents`, `Editors`, `FileEditors`: local wrappers for file/editor/document operations.
- `ContextLookup`: resolve connection/session/schema/project context from IDE data contexts and editors.
- `NavigationInstructions` and related navigation helpers: use for navigation behavior instead of direct editor jumps when adjacent code does.

## Database-Oriented Helpers

- `DatabaseContext`, `DatabaseContextBase`, `ConnectionId`, `SessionId`, `SchemaId`: pass database context explicitly where possible.
- `AuthenticationInfo`, `DatabaseInfo`, `DatabaseOperation`: common value/operation abstractions.
- `DBObject`, `DBSchemaObject`, `DBObjectBundle`, `DBObjectList`: follow existing object model rather than introducing detached representations.

## Import Selection Cheat Sheet

- Need project/application service: `Components`.
- Need safe object access: `Failsafe`, `Checks`, `Disposer`.
- Need async work: `Background`, `Dispatch`, `Progress`, `Write`.
- Need weak/stable identity: `WeakRef`, `ProjectRef`, `ConnectionRef`, `DBObjectRef`.
- Need XML persistence: `Settings`.
- Need strings/lists/defaults: `Strings`, `Lists`, `Commons`.
- Need UI form/dialog behavior: `DBNFormBase`, `DBNDialog`, UI util package.
- Need user-visible text: `NlsResources.txt`.
