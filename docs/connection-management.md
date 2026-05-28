# Connection Management

## Summary

Connection Management is the DBN area for creating, organizing, configuring, testing, and maintaining database connections. It covers native database connections, custom JDBC connections, driver selection, authentication, SSH tunneling, connection properties, and connection-specific browser filters.

- **Primary UI:** Connection settings in DBN project settings
- **Common entry points:** `DB Browser` toolbar, connection context menus, and DBN settings
- **Main topics:** Supported Databases and Driver Management
- **Common users:** Developers, DBAs, data engineers, and anyone connecting an IDE project to database resources

> **Note:** Available fields and actions depend on the selected database type, connection type, installed drivers, enabled DBN features, and database permissions.

## When To Use It

Use Connection Management when you need to:

- Create a new Oracle, MySQL, PostgreSQL, SQLite, or custom JDBC connection.
- Configure connection URL, authentication, and JDBC driver settings.
- Test a connection before using it in the Database Browser or SQL Console.
- Duplicate, reorder, activate, deactivate, copy, paste, or remove connection definitions.
- Import Oracle TNS names as connection definitions.
- Configure SSH tunneling, JDBC properties, workspace restore, connection pooling, and object filters.

## Access Paths

- **Create connection from browser:** `DB Browser` toolbar -> `New Connection`
- **Create connection from settings:** `DB Navigator` -> `Settings` -> `Connections` -> `Add`
- **Edit connection:** Open DBN settings, select a connection, then edit its tabs
- **Test connection:** Connection editor header -> `Test Connection`
- **Connection information:** Connection editor header -> `Info`
- **Connection filters:** Connection editor -> `Filters`, or `DB Browser` toolbar -> `Options` -> `Object Filters...`

## Connection List

The connection list is the project-level inventory of configured database connections. It is used by the Database Browser, SQL consoles, database editors, and DBN actions that require a connection context.

- **`Add`:** Creates a new connection definition.
- **`Duplicate`:** Copies the selected connection into a new connection definition with a new internal id.
- **`Remove`:** Deletes the selected connection definition from the project configuration.
- **`Move Up` / `Move Down`:** Changes connection order in DBN UI lists.
- **`Activate` / `Deactivate`:** Enables or disables a connection without deleting it.
- **`Copy`:** Copies selected connection definitions to the clipboard as DBN connection configuration.
- **`Paste`:** Imports copied DBN connection configuration and assigns new connection ids.
- **`Import TNS Names`:** Creates Oracle connections from TNS profiles.

New connections receive a default name and DBN adjusts names when needed to keep them unique.

## Supported Databases

DBN provides native connection support for Oracle, MySQL, PostgreSQL, and SQLite. It also supports custom JDBC connections through the Generic database type.

| Database type | Native support | Default driver source | Default driver class | URL patterns | Authentication |
| --- | --- | --- | --- | --- | --- |
| `Oracle` | Yes | Bundled | `oracle.jdbc.driver.OracleDriver` | Service, SID, EZConnect, TNS, Custom | None, user, user/password, OS credentials, token |
| `MySQL` | Yes | Bundled | `com.mysql.cj.jdbc.Driver` | Host/port/database, Custom | None, user, user/password, OS credentials |
| `PostgreSQL` | Yes | Bundled | `org.postgresql.Driver` | Host/port/database, Custom | None, user, user/password, OS credentials |
| `SQLite` | Yes | Bundled | `org.sqlite.JDBC` | Database file, Custom | None |
| `Generic` | Custom JDBC | External | Selected external driver | Custom JDBC URL | None, user, user/password, OS credentials |

### Oracle

Use Oracle connections for Oracle Database, Autonomous Database, and Oracle-compatible workflows supported by the selected driver and authentication method.

Common Oracle URL modes include:

- **`Service`:** You connect by host, port, and service name.
- **`SID`:** You connect by host, port, and SID.
- **`EZConnect`:** You need EZConnect protocol, server type, or additional URL parameters.
- **`TNS`:** You connect through a `tnsnames.ora` profile.
- **`Custom`:** You need to edit the full JDBC URL manually.

Oracle supports token authentication in addition to basic user/password and OS credential modes. Token fields depend on the selected token provider.

### MySQL

Use MySQL connections for MySQL-compatible JDBC drivers. DBN can recognize related MySQL-family database products from JDBC metadata, such as MariaDB, Percona, OurDelta, Drizzle, and MaxDB.

The standard MySQL URL mode uses host, port, and database name. Use `Custom` when a driver-specific JDBC URL is required.

### PostgreSQL

Use PostgreSQL connections for PostgreSQL-compatible JDBC drivers. DBN can recognize related PostgreSQL-family database products from JDBC metadata, such as Redshift, Greenplum, Netezza, Paraccel, Teradata, Yugabyte, and other PostgreSQL-derived drivers.

The standard PostgreSQL URL mode uses host, port, and database name. Use `Custom` when a driver-specific JDBC URL is required.

### SQLite

Use SQLite connections for file-based SQLite databases. SQLite connections use a database file path and do not require authentication.

### Generic JDBC

Use Generic connections when DBN does not provide a native database type for the target system or when you need a fully custom JDBC driver and URL.

Generic connections always use an external driver library. After the library is selected, DBN scans it for JDBC driver classes and lets you choose the class to load.

> **Tip:** If a Generic connection uses a driver DBN recognizes as Oracle, MySQL, PostgreSQL, or SQLite, DBN can suggest switching to the dedicated database type.

## Driver Management

Driver Management controls which JDBC driver DBN uses for a connection. The driver can come from the bundled DBN driver libraries or from an external JAR or folder.

### Driver Sources

| Driver source | Available for | Description |
| --- | --- | --- |
| `Bundled` | Native database types | Uses the JDBC driver packaged with DBN for the selected database type. |
| `External` | Native and Generic database types | Uses a user-selected JAR file or folder containing JDBC driver classes. |

Generic connections use `External` driver source because DBN cannot infer a bundled driver for an arbitrary JDBC vendor.

### Bundled Drivers

Bundled drivers are the simplest option for native database types. DBN selects the driver class for the chosen database type and loads the bundled library automatically.

Use bundled drivers when:

- The bundled driver supports your database version and authentication needs.
- You do not need a patched, vendor-specific, or organization-approved driver.
- You want the fastest setup path for Oracle, MySQL, PostgreSQL, or SQLite.

### External Drivers

External drivers let you point DBN to a JDBC driver JAR file or a folder containing driver libraries.

Use external drivers when:

- You need a specific JDBC driver version.
- Your organization distributes approved driver binaries separately.
- You are creating a Generic JDBC connection.
- A native bundled driver does not support a required database feature.

When an external library is selected, DBN scans it in an isolated driver loader and fills the driver class list with discovered JDBC drivers. Use `Reload Drivers` after replacing or changing the driver library.

### Download Driver Libraries

The driver settings area includes a download action for driver packages known to DBN. After a driver package is downloaded, DBN can fill the driver library path so it can be used as an external driver.

The downloaded library still behaves like an external driver for the connection. Review the selected driver class before saving the connection.

### Driver Validation

DBN validates driver settings before saving connection changes.

- **Driver library path:** External driver source requires a library path.
- **Library existence:** The selected JAR or folder must exist and be readable.
- **Driver class:** External driver source requires a selected driver class.
- **Database match:** For native database types, the external driver must match the selected database type.
- **Generic driver:** Generic connections accept the selected external JDBC driver class.

If a native connection uses an external library that does not match the selected database type, DBN reports that the driver library does not match the selected database type.

## Common Workflows

### Create A Native Connection

1. Open the `DB Browser` toolbar.
2. Click `New Connection`.
3. Select `Oracle`, `MySQL`, `PostgreSQL`, or `SQLite`.
4. Enter the connection name and URL fields.
5. Select the authentication mode and enter credentials when required.
6. Keep `Bundled` driver source unless you need a specific external driver.
7. Click `Test Connection`.
8. Save the connection.

### Create A Generic JDBC Connection

1. Create a new `Custom Connection`.
2. Set the database type to `Generic`.
3. Enter the full JDBC URL.
4. Select an external driver library JAR or folder.
5. Click `Reload Drivers` if the driver class list is empty or stale.
6. Select the driver class.
7. Configure authentication if the driver requires it.
8. Click `Test Connection`.
9. Save the connection.

### Import Oracle TNS Names

1. Open connection settings.
2. Choose `Import TNS Names`.
3. Select the TNS names source.
4. Review the generated Oracle connection definitions.
5. Test and adjust each imported connection before using it.

Imported TNS connections use the Oracle database type and bundled driver source by default.

### Duplicate A Connection

1. Select a connection in DBN settings.
2. Click `Duplicate`.
3. Rename the duplicated connection.
4. Change URL, credentials, filters, or other settings as needed.
5. Test and save the new connection.

Duplicating is useful when multiple environments share most settings but differ by host, service, credentials, or filters.

## Security Notes

- Passwords, SSH secrets, and token-related secret values are managed through DBN credential storage instead of being stored as ordinary connection settings.
- Clipboard copy and paste is intended for DBN connection configuration transfer. Review imported connections before saving or sharing them.
- External driver libraries execute JDBC driver code inside the IDE process. Use driver binaries from trusted sources.
- Token and OS credential authentication depend on the operating system, cloud provider, JDBC driver, and database configuration.

## Troubleshooting

| Symptom | Resolution |
| --- | --- |
| Connection URL is rejected | Confirm the selected URL mode and required fields, such as host, port, service, SID, database, file, or custom URL. |
| Driver library is invalid | Check that the selected external path exists and points to a JAR file or folder containing JDBC drivers. |
| Driver class list is empty | Click `Reload Drivers`, then verify the library contains a JDBC driver class. |
| Driver does not match database type | Use a driver for the selected native database type, or switch the connection to `Generic`. |
| SQLite authentication fields are unavailable | SQLite connections use `None` authentication. |
| TNS profile is missing | Verify the TNS folder and `tnsnames.ora` contents, or use a custom Oracle URL. |
| Workspace does not restore automatically | Check that the connection is set to connect automatically and that workspace restore is enabled. |

## Related Documentation

- [Connection Management Settings](./connection-management-settings.md): Settings reference for connection configuration, supported databases, drivers, and authentication.
- [Environment Types](./environment-types.md): Guide to classifying connections by environment, color, and read-only behavior.
- [Workspace Integration](./workspace-integration.md): Guide to mapping project files and folders to configured connections, schemas, and sessions.
- [Transaction Management](./transaction-management.md): Guide to auto-commit, pending transactions, Resource Monitor, and sessions.
- [Database Browser](./database-browser.md): Guide to browsing configured connections and database objects.
- [Database Browser Settings](./database-browser-settings.md): Settings reference for browser layout, filters, sorting, default editors, and toolbar state.
