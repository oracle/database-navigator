# Connection Management Settings

## Summary

Connection Management settings define how DBN connects to databases, which drivers it loads, how credentials are supplied, and how each connection behaves inside the IDE.

| Setting area | Scope | Purpose |
| --- | --- | --- |
| Connection list | Project | Creates, orders, activates, duplicates, imports, copies, pastes, and removes connections. |
| Database | Connection | Defines database type, URL, authentication, and driver settings. |
| SSH Tunnel | Connection | Routes a basic connection through an SSH tunnel. |
| Properties | Connection | Adds JDBC properties and controls auto-commit. |
| Details | Connection | Controls environment, encoding, pooling, workspace restore, logging, and timeouts. |
| Debugger | Oracle connection | Controls Oracle debugging behavior. |
| Filters | Connection | Controls object visibility for the connection. |

> **Note:** Some tabs appear only when they apply to the selected connection type or database type. For example, the `Debugger` tab is available for Oracle connections.

## Access Paths

- **Connection settings:** `DB Navigator` -> `Settings` -> `Connections`
- **New connection:** `DB Browser` toolbar -> `New Connection`
- **Edit selected connection:** DBN settings -> `Connections` -> select connection
- **Test selected connection:** Connection editor header -> `Test Connection`
- **Connection information:** Connection editor header -> `Info`
- **Connection object filters:** Connection editor -> `Filters`

## Connection List Settings

The connection list controls the project connection inventory.

- **`Add`:** Creates a new connection. Available templates include Oracle, MySQL, PostgreSQL, SQLite, Custom Connection, and Import TNS Names.
- **`Duplicate`:** Creates a copy of the selected connection with a new internal id and adjusted name.
- **`Remove`:** Removes the selected connection definition.
- **`Move Up` / `Move Down`:** Changes the display order of connections.
- **`Activate` / `Deactivate`:** Enables or disables a connection without deleting it.
- **`Copy`:** Copies selected connection definitions to the clipboard.
- **`Paste`:** Imports copied DBN connection definitions with new ids and unique names.
- **`Import TNS Names`:** Creates Oracle connection definitions from TNS profiles.

## Database Tab

The `Database` tab defines the connection identity, database type, URL, authentication, and JDBC driver.

### General Fields

| Setting | Default | Description |
| --- | --- | --- |
| `Name` | `Connection`, adjusted for uniqueness | Display name shown in DBN connection lists and the Database Browser. |
| `Description` | Empty | Optional free-text description for the connection. |
| `Database type` | Depends on creation action | Selects Oracle, MySQL, PostgreSQL, SQLite, or Generic. For native connection templates, the type is fixed. For custom connections, it is selectable. |

### Supported Database Types

| Database type | URL options | Default driver source | Authentication options |
| --- | --- | --- | --- |
| `Oracle` | Service, SID, EZConnect, TNS, Custom | Bundled | None, user, user/password, OS credentials, token |
| `MySQL` | Host/port/database, Custom | Bundled | None, user, user/password, OS credentials |
| `PostgreSQL` | Host/port/database, Custom | Bundled | None, user, user/password, OS credentials |
| `SQLite` | Database file, Custom | Bundled | None |
| `Generic` | Custom JDBC URL | External | None, user, user/password, OS credentials |

## URL Settings

URL fields change according to the selected database type and URL mode.

| URL mode | Fields | Description |
| --- | --- | --- |
| `Service` | Host, port, service name | Oracle service-name URL. |
| `SID` | Host, port, SID | Oracle SID URL. |
| `EZConnect` | Host, port, service name, protocol, server type, parameters | Oracle EZConnect URL with optional advanced parameters. |
| `TNS` | TNS folder, TNS profile | Oracle TNS URL based on `tnsnames.ora`. |
| `Database` | Host, port, database | MySQL and PostgreSQL host/database URL. |
| `File` | Database file | SQLite file URL. |
| `Custom` | Full JDBC URL | Manually entered JDBC URL. |

### URL Templates

- **Oracle service:** `jdbc:oracle:thin:@//<HOST>:<PORT>/<SERVICE_NAME>`
- **Oracle SID:** `jdbc:oracle:thin:@<HOST>:<PORT>:<SID>`
- **Oracle TNS:** `jdbc:oracle:thin:@<TNS_PROFILE>?TNS_ADMIN="<TNS_FOLDER>"`
- **MySQL:** `jdbc:mysql://<HOST>:<PORT>/<DATABASE>`
- **PostgreSQL:** `jdbc:postgresql://<HOST>:<PORT>/<DATABASE>`
- **SQLite:** `jdbc:sqlite:<FILE>`
- **Generic:** `jdbc:<VENDOR>://<HOST>:<PORT>/<DATABASE>`

### URL Validation

- **Connection URL is required:** All database types
- **Host, port, service, SID, database, or file fields are required:** Non-custom URL modes where those fields are part of the selected URL template
- **TNS profile must be available:** Oracle TNS URL mode
- **Custom URL must be a complete JDBC URL:** Custom and Generic URL modes

## Authentication Settings

Authentication fields change with the selected authentication type.

| Authentication type | Fields | Available for |
| --- | --- | --- |
| `None` | No credentials | Oracle, MySQL, PostgreSQL, SQLite, Generic |
| `User` | User | Oracle, MySQL, PostgreSQL, Generic |
| `User / Password` | User, password | Oracle, MySQL, PostgreSQL, Generic |
| `OS Credentials` | Operating-system based credentials | Oracle, MySQL, PostgreSQL, Generic |
| `Token` | Token provider fields | Oracle |

### Oracle Token Authentication

When `Token` authentication is selected for Oracle, the token type controls the fields shown in the connection editor.

- **`OCI API Key`:** OCI config file, profile, compartment OCID, database OCID
- **`OCI Interactive`:** OCI profile and interactive browser-based login context
- **`Azure Service Principal Certificate`:** App ID URI, client ID, tenant ID, certificate file, certificate password
- **`Azure Service Principal Token`:** App ID URI, client ID, tenant ID, client secret
- **`Azure Interactive`:** App ID URI, client ID, tenant ID and interactive browser-based login context

> **Note:** OCI interactive authentication uses a local callback port. Azure token authentication may warn when JVM HTTP proxy system properties are configured.

## Driver Settings

Driver settings control which JDBC driver library and class DBN loads for the connection.

| Setting | Default | Description |
| --- | --- | --- |
| `Driver Source` | `Bundled` for native database types, `External` for Generic | Selects bundled DBN driver libraries or an external library path. |
| `Driver Library` | Empty for external source | JAR file or folder containing JDBC driver classes. Required for external drivers. |
| `Driver` | Database default for bundled source; selected discovered class for external source | JDBC driver class used by the connection. |
| `Reload Drivers` | Action | Rescans the selected external driver library and refreshes the driver class list. |
| `Download` | Action | Opens available driver package downloads and fills the external driver library path when a package is selected. |

### Bundled Driver Defaults

- **Oracle:** `oracle.jdbc.driver.OracleDriver`
- **MySQL:** `com.mysql.cj.jdbc.Driver`
- **PostgreSQL:** `org.postgresql.Driver`
- **SQLite:** `org.sqlite.JDBC`

Generic connections do not have a bundled driver default.

### External Driver Rules

- **Library required:** External driver source requires a selected JAR or folder.
- **Driver class required:** DBN must discover or receive a JDBC driver class.
- **Native database match:** Oracle, MySQL, PostgreSQL, and SQLite connections require an external driver that matches the selected database type.
- **Generic database flexibility:** Generic connections accept the selected external JDBC driver class.
- **Reload after changes:** Use `Reload Drivers` after replacing a driver file or changing the driver folder contents.

## SSH Tunnel Tab

The `SSH Tunnel` tab is available for basic connection configurations and routes the database connection through an SSH server.

| Setting | Default | Description |
| --- | --- | --- |
| `Active` | Disabled | Enables SSH tunneling for the connection. |
| `Host` | Empty | SSH server host. Required when the tunnel is active. |
| `Port` | `22` | SSH server port. |
| `User` | Empty | SSH user name. |
| `Authentication type` | `Password` | Selects password or key-pair authentication. |
| `Password` | Stored as secret | SSH password for password authentication. |
| `Key file` | Empty | Private key file for key-pair authentication. Required when key-pair authentication is active. |
| `Key passphrase` | Stored as secret | Optional private key passphrase. |

## Properties Tab

The `Properties` tab controls JDBC-level properties for the connection.

| Setting | Default | Description |
| --- | --- | --- |
| `Auto-commit` | Disabled | Enables JDBC auto-commit for the connection when selected. |
| JDBC properties | Empty | Key/value properties passed to the JDBC driver. |

Use JDBC properties for driver-specific options that are not represented by dedicated DBN fields.

## Details Tab

The `Details` tab controls connection behavior inside DBN.

| Setting | Default | Valid range | Description |
| --- | --- | --- | --- |
| `Content encoding` | `UTF-8` | Valid JVM charset | Character set used for connection content handling. |
| `Environment type` | `Default` | Configured environment types | Environment classification used by DBN UI coloring and behavior. |
| `Enable session management` | Enabled | Enabled/disabled | Enables DBN session management for the connection. |
| `Enable project DDL file lookup` | Enabled | Enabled/disabled | Allows DBN to bind database objects to project DDL files. |
| `Enable database logging` | Enabled | Enabled/disabled | Enables DBN database logging for the connection. |
| `Connect automatically` | Enabled | Enabled/disabled | Allows DBN to connect automatically when a connection is needed. |
| `Restore workspace` | Enabled | Enabled/disabled | Restores DBN workspace state for the connection. |
| `Deep restore workspace` | Disabled | Enabled/disabled | Restores deeper workspace state when workspace restore is enabled. |
| `Connectivity timeout` | `30` seconds | `0` to `90` | Maximum time to wait for connectivity operations. |
| `Idle time to disconnect` | `30` minutes | `0` to `60` | Disconnects idle main connections after the configured time. |
| `Idle time to disconnect pool` | `5` minutes | `1` to `60` | Disconnects idle pooled connections after the configured time. |
| `Credential expiry` | `10` minutes | `0` to `60` | Controls how long requested credentials remain valid. |
| `Maximum connection pool size` | `7` | `3` to `20` | Limits the connection pool size for the connection. |
| `Alternative statement delimiter` | Empty | Text value | Optional statement delimiter used by SQL execution workflows. |

If `Restore workspace` is enabled but `Connect automatically` is disabled, DBN cannot restore the workspace automatically until the connection is opened.

## Debugger Tab

The `Debugger` tab is available for Oracle connections.

| Setting | Default | Description |
| --- | --- | --- |
| `Compile dependencies` | Enabled | Compiles dependent database objects as part of supported debugging workflows. |
| `Debugger type` | `Ask` when JDWP is supported, otherwise JDBC | Selects JDWP, JDBC, or asks at debug time. |
| `JDWP tunnel type` | `None` | Selects `TCP driver tunnel`, `SSH reverse tunnel`, or `None` for JDWP debugging. |
| `TCP host address` | Empty | Host address used for JDWP callback when required by the selected tunnel type. |
| `TCP port range` | `4000` to `4999` | Port range used for JDWP debugging. |

### Reverse SSH Tunnel Settings

Reverse SSH tunnel fields appear when JDWP debugging uses `SSH reverse tunnel`.

| Setting | Default | Description |
| --- | --- | --- |
| `Host` | Empty | SSH host used for the reverse tunnel. |
| `Port` | `22` | SSH port used for the reverse tunnel. |
| `User` | Empty | SSH user name. |
| `Authentication type` | `Password` | Selects password or key-pair authentication. |
| `Password` | Stored as secret | SSH password for password authentication. |
| `Key file` | Empty | Private key file for key-pair authentication. |
| `Key passphrase` | Stored as secret | Optional private key passphrase. |
| `Bind host` | `127.0.0.1` | Remote bind host for the reverse tunnel. |
| `Bind port` | `0` | Remote bind port. `0` lets the tunnel choose an available port. |

## Filters Tab

The `Filters` tab controls connection-level object visibility. It can inherit project-level Database Browser filters or define connection-specific overrides.

- **Object type filters:** Shows or hides supported database object types for this connection.
- **Basic filters:** Hides empty schemas, audit columns, or pseudo columns when supported.
- **Custom filters:** Adds persistent object filters for schemas, tables, views, columns, indexes, constraints, triggers, procedures, functions, packages, types, synonyms, Java classes, and database links.

For the full filter reference, see [Database Browser Settings](./database-browser-settings.md).

## Validation Messages

| Message area | Common cause | Resolution |
| --- | --- | --- |
| URL validation | Missing URL or missing required URL fields | Complete the selected URL mode fields or switch to `Custom` and enter a full JDBC URL. |
| External driver library | Missing or invalid driver library path | Select a valid JAR file or folder. |
| Driver class | No driver class selected | Reload the driver library and select a discovered JDBC driver class. |
| Driver mismatch | External driver does not match selected native database type | Select a matching driver or use `Generic`. |
| SSH tunnel | Active tunnel missing host, port, user, or key file | Complete required SSH tunnel fields. |
| Detail settings | Numeric setting outside allowed range | Enter a value within the range shown in the `Details` tab. |

## Related Documentation

- [Connection Management](./connection-management.md): Guide to creating and managing database connections, supported databases, and driver workflows.
- [Environment Types Settings](./environment-types-settings.md): Settings reference for environment definitions, colors, read-only safeguards, and connection assignment.
- [Workspace Integration Settings](./workspace-integration-settings.md): Settings reference for project DDL lookup and file connection mappings.
- [Transaction Management Settings](./transaction-management-settings.md): Settings reference for auto-commit, transaction prompts, Resource Monitor, and Session Browser.
- [Debugging Engine Settings](./debugging-engine-settings.md): Settings reference for debugger type, JDWP tunnel behavior, reverse SSH, and debug timeouts.
- [Database Browser](./database-browser.md): Guide to browsing configured connections and database objects.
- [Database Browser Settings](./database-browser-settings.md): Settings reference for browser layout, filters, sorting, default editors, and toolbar state.
