package com.dbn.common.icon;

import com.dbn.common.latent.Latent;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.ScalableIcon;
import com.intellij.ui.RowIcon;
import com.intellij.util.IconUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.Icon;

import static com.dbn.common.icon.IconLoader.init;
import static com.dbn.common.icon.IconLoader.load;

@SuppressWarnings("unused")
@Slf4j
public class Icons {

    // lazy initialised tool window icons (proxied com.dbn.common.icon.LatentIcon is not supporting all tool-window icon variants)
    public static final Latent<Icon> WINDOW_DATABASE_BROWSER     = Latent.basic(()-> init("/img/window/DatabaseBrowser.svg"));
    public static final Latent<Icon> WINDOW_EXECUTION_CONSOLE    = Latent.basic(()-> init("/img/window/ExecutionConsole.svg"));
    public static final Latent<Icon> WINDOW_DATABASE_DIAGNOSTICS = Latent.basic(()-> init("/img/window/DatabaseDiagnostics.svg"));
    public static final Latent<Icon> WINDOW_DATABASE_ASSISTANT = Latent.basic(()-> init("/img/window/DatabaseAssistant.svg"));


    public static final Icon DBN_SPLASH = load("/img/DBN.png");
    public static final Icon DONATE = load("/img/Donate.png");
    public static final Icon DONATE_DISABLED = load("/img/DonateDisabled.png");

    public static final Icon COMMON_INFO          = AllIcons.General.Information;
    public static final Icon COMMON_INFO_DISABLED = AllIcons.General.Note;
    public static final Icon COMMON_WARNING       = AllIcons.General.Warning;
    public static final Icon COMMON_ERROR         = AllIcons.General.Error;
    public static final Icon COMMON_CHECK         = AllIcons.General.InspectionsOK;
    public static final Icon COMMON_RIGHT         = load("/img/common/SplitRight.png");
    public static final Icon COMMON_LEFT          = load("/img/common/SplitLeft.png");
    public static final Icon COMMON_UP            = load("/img/common/SplitUp.png");
    public static final Icon COMMON_DOWN          = load("/img/common/SplitDown.png");
    public static final Icon COMMON_ARROW_DOWN    = load("/img/ComboBoxArrow.png");
    public static final Icon COMMON_TIMER         = load("/img/Timer.png");
    public static final Icon COMMON_DATA_GRID     = load("/img/common/DataGrid.png");

    public static final Icon COMMON_FILTER_ACTIVE =    AllIcons.Actions.IntentionBulb;
    public static final Icon COMMON_FILTER_INACTIVE =  AllIcons.Actions.IntentionBulbGrey;

    public static final Icon ACTION_ADD                       = AllIcons.General.Add;
    public static final Icon ACTION_COPY                      = AllIcons.Actions.Copy;
    public static final Icon ACTION_CHECK                     = AllIcons.Actions.Checked;
    public static final Icon ACTION_REMOVE                    = AllIcons.General.Remove;
    public static final Icon ACTION_SORT_ALPHA                = AllIcons.ObjectBrowser.Sorted;
    public static final Icon ACTION_SORT_NUMERIC              = load("/img/action/SortNumerically.png");
    public static final Icon ACTION_SORT_ASC                  = load("/img/action/SortAscending.png");
    public static final Icon ACTION_SORT_DESC                 = load("/img/action/SortDescending.png");
    public static final Icon ACTION_ADD_MORE                  = load("/img/action/AddMore.png");
    public static final Icon ACTION_ADD_SPECIAL               = load("/img/action/AddSpecial.png");
    public static final Icon ACTION_MOVE_UP                   = load("/img/action/MoveUp.svg");
    public static final Icon ACTION_MOVE_DOWN                 = load("/img/action/MoveDown.svg");
    public static final Icon ACTION_EDIT                      = AllIcons.Actions.EditSource; //load("/img/action/EditSource.svg");
    public static final Icon ACTION_COLLAPSE_ALL              = AllIcons.Actions.Collapseall;
    public static final Icon ACTION_EXPAND_ALL                = AllIcons.Actions.Expandall;
    public static final Icon ACTION_GROUP                     = load("/img/action/Group.svg");
    public static final Icon ACTION_DELETE                    = AllIcons.Actions.GC;
    public static final Icon ACTION_HELP                      = AllIcons.Actions.Help;
    public static final Icon ACTION_EXECUTE                   = AllIcons.Actions.Execute;
    public static final Icon ACTION_DEBUG                     = AllIcons.Actions.StartDebugger;
    public static final Icon ACTION_CLOSE                     = AllIcons.Actions.Cancel;
    public static final Icon ACTION_CLOSE_SMALL               = load("/img/action/Close.svg");
    public static final Icon ACTION_UP_DOWN                   = load("/img/action/UpDown.svg");
    public static final Icon ACTION_REFRESH                   = AllIcons.Actions.Refresh;
    public static final Icon ACTION_RETRY                     = load("/img/action/Retry.svg");
    public static final Icon ACTION_TIMED_REFRESH             = load("/img/action/TimedSynchronize.png");
    public static final Icon ACTION_TIMED_REFRESH_INTERRUPTED = load("/img/action/TimedSynchronizeInterrupted.png");
    public static final Icon ACTION_TIMED_REFRESH_OFF         = load("/img/action/TimedSynchronizeOff.png");
    public static final Icon ACTION_WRAP_TEXT                 = load("/img/action/WrapText.png");
    public static final Icon ACTION_PIN                       = AllIcons.General.Pin_tab;
    public static final Icon ACTION_FIND                      = AllIcons.Actions.Find;
    public static final Icon ACTION_PREVIEW                   = AllIcons.Actions.Preview;
    public static final Icon ACTION_RERUN                     = AllIcons.Actions.Rerun;
    public static final Icon ACTION_RESUME                    = AllIcons.Actions.Resume;
    public static final Icon ACTION_STOP                      = AllIcons.Actions.Suspend;
    public static final Icon ACTION_RELOAD                    = AllIcons.Actions.Refresh;
    public static final Icon ACTION_REVERT                    = AllIcons.Actions.Rollback;
    public static final Icon ACTION_SAVE                      = load("/img/action/Save.svg");
    public static final Icon ACTION_SELECT_ALL                = AllIcons.Actions.Selectall;
    public static final Icon ACTION_OPTIONS                   = AllIcons.General.GearPlain;
    public static final Icon ACTION_NAVIGATE                  = load("/img/action/Navigate.png");
    public static final Icon ACTION_DISCONNECT_SESSION        = load("/img/action/DisconnectSession.png");
    public static final Icon ACTION_KILL_SESSION              = load("/img/action/KillSession.png");
    public static final Icon ACTION_TOGGLE_LOGGING            = load("/img/action/ToggleLogging.svg");

    public static final Icon FOLDER_CONNECTION_ASSOCIATION = load("/img/action/FolderConnectionLink.png");
    public static final Icon FOLDER_SCHEMA_ASSOCIATION = load("/img/action/FolderSchemaLink.png");


    public static final Icon DATABASE_NAVIGATOR = load("/img/project/DatabaseNavigator.svg");
    public static final Icon DATABASE_MODULE = load("/img/project/DatabaseModule.png");

    public static final Icon SQL_CONSOLE = load("/img/tools/SQLConsole.svg");
    public static final Icon SESSION_BROWSER = load("/img/tools/SessionBrowser.svg");

    public static final Icon FILE_SQL_CONSOLE = load("/img/file/SQLConsoleFile.svg");
    public static final Icon FILE_SQL_DEBUG_CONSOLE = load("/img/file/SQLDebugConsole.svg");
    public static final Icon FILE_SESSION_BROWSER = load("/img/file/SessionBrowserFile.png");
    public static final Icon FILE_SQL = load("/img/file/SQLFile.svg");
    public static final Icon FILE_PLSQL = load("/img/file/PLSQLFile.svg");
    public static final Icon FILE_BLOCK_PLSQL = load("/img/PLSQLBlock.png", "FILE_BLOCK_PLSQL");
    public static final Icon FILE_BLOCK_PSQL = load("/img/file/PSQLBlock.png", "FILE_BLOCK_PSQL");
    public static final Icon FILE_BLOCK_SQL = load("/img/file/SQLBlock.png", "FILE_BLOCK_SQL");


    public static final Icon DIALOG_INFORMATION = AllIcons.General.InformationDialog;
    public static final Icon DIALOG_WARNING     = AllIcons.General.WarningDialog;
    public static final Icon DIALOG_ERROR       = AllIcons.General.ErrorDialog;
    public static final Icon DIALOG_QUESTION    = AllIcons.General.QuestionDialog;


    public static final Icon METHOD_EXECUTION_RUN     = ACTION_EXECUTE;
    public static final Icon METHOD_EXECUTION_DEBUG   = ACTION_DEBUG;
    public static final Icon METHOD_EXECUTION_RERUN   = ACTION_RERUN;
    public static final Icon METHOD_EXECUTION_STOP    = ACTION_STOP;
    public static final Icon METHOD_EXECUTION_DIALOG  = load("/img/ExecuteMethodDialog.png");
    public static final Icon METHOD_EXECUTION_HISTORY = load("/img/MethodExecutionHistory.png");
    public static final Icon METHOD_CALL              = load("/img/MethodCall.png", "METHOD_CALL");
    public static final Icon SQL_STATEMENT            = load("/img/SQLStatement.png", "SQL_STATEMENT");


    public static final Icon KILL_PROCESS               = AllIcons.Debugger.KillProcess;
    public static final Icon EXECUTE_SQL_SCRIPT         = load("/img/action/ExecuteAsScript.png");

    public static final Icon STMT_EXECUTION_EXPLAIN       = load("/img/action/StatementExplain.png");
    public static final Icon STMT_EXECUTION_RUN           = ACTION_EXECUTE;
    public static final Icon STMT_EXECUTION_STOP          = ACTION_STOP;
    public static final Icon STMT_EXECUTION_STOP_QUEUED   = load("/img/action/ExecutionStopQueued.svg");
    public static final Icon STMT_EXECUTION_DEBUG         = ACTION_DEBUG;
    public static final Icon STMT_EXECUTION_RERUN         = ACTION_RERUN;
    public static final Icon STMT_EXECUTION_RESUME        = ACTION_RESUME;
    public static final Icon STMT_EXECUTION_REFRESH       = AllIcons.Actions.Refresh;
    public static final Icon STMT_EXECUTION_ERROR         = AllIcons.General.Error;
    public static final Icon STMT_EXECUTION_WARNING       = AllIcons.General.Warning;
    public static final Icon STMT_EXECUTION_ERROR_RERUN   = load("/img/action/ExecutionError.svg");
    public static final Icon STMT_EXECUTION_WARNING_RERUN = load("/img/action/ExecutionWarning.svg");
    public static final Icon STMT_EXECUTION_INFO_RERUN    = load("/img/action/ExecutionInfo.svg");
    public static final Icon STMT_EXECUTION_NAVIGATE      = load("/img/action/NavigateToResult.png");

    public static final Icon EXPLAIN_PLAN_RESULT        = load("/img/ExplainPlanResult.png");
    public static final Icon STMT_EXEC_RESULTSET        = load("/img/ExecutionResultSet.png");
    public static final Icon STMT_EXEC_RESULTSET_RERUN  = load("/img/ExecutionResultSetRerun.png");
    public static final Icon STMT_EXEC_RESULTSET_ORPHAN = load("/img/ExecutionResultSetOrphan.png");

    public static final Icon EXEC_RESULT_OPEN_EXEC_DIALOG   = load("/img/ExecuteMethodDialog.png");
    public static final Icon EXEC_RESULT_RERUN              = ACTION_RERUN;
    public static final Icon EXEC_RESULT_RESUME             = ACTION_RESUME;
    public static final Icon EXEC_RESULT_STOP               = ACTION_STOP;
    public static final Icon EXEC_RESULT_OPTIONS            = ACTION_OPTIONS;
    public static final Icon EXEC_RESULT_CLOSE              = ACTION_CLOSE;
    public static final Icon EXEC_RESULT_VIEW_STATEMENT     = ACTION_PREVIEW;
    public static final Icon EXEC_RESULT_VIEW_RECORD        = load("/img/tools/RecordViewer.svg");
    public static final Icon EXEC_RESULT_MESSAGES           = load("/img/common/Messages.png");
    public static final Icon EXEC_CONFIG                    = load("/img/DBProgram.png");
    public static final Icon EXEC_METHOD_CONFIG             = load("/img/DBMethodExecution.png");
    public static final Icon EXEC_STATEMENT_CONFIG          = load("/img/DBStatementExecution.png");
    public static final Icon EXEC_LOG_OUTPUT_CONSOLE        = load("/img/LogOutputConsole.png");
    public static final Icon EXEC_LOG_OUTPUT_CONSOLE_UNREAD = load("/img/LogOutputConsoleUnread.png");
    public static final Icon EXEC_LOG_OUTPUT_ENABLE         = load("/img/LogOutputEnable.png");
    public static final Icon EXEC_LOG_OUTPUT_DISABLE        = load("/img/LogOutputDisable.png");

    public static final Icon NAVIGATION_GO_TO_SPEC       = load("/img/GoToSpec.png");
    public static final Icon NAVIGATION_GO_TO_BODY       = load("/img/GoToBody.png");

    public static final Icon BROWSER_BACK = AllIcons.Actions.Back;
    public static final Icon BROWSER_NEXT = AllIcons.Actions.Forward;
    public static final Icon BROWSER_OBJECT_PROPERTIES = load("/img/tools/ObjectProperties.svg");


    public static final Icon DATA_EDITOR_ROW_DEFAULT = load("/img/DefaultRow.png");
    public static final Icon DATA_EDITOR_ROW_INSERT = load("/img/InsertRow.png");
    public static final Icon DATA_EDITOR_ROW_INSERTED = load("/img/NewRow.png");
    public static final Icon DATA_EDITOR_ROW_MODIFIED = load("/img/ModifiedRow.png");
    public static final Icon DATA_EDITOR_ROW_DELETED = load("/img/DeletedRow.png");

    public static final Icon DATA_EDITOR_DUPLICATE_RECORD       = ACTION_COPY;
    public static final Icon DATA_EDITOR_INSERT_RECORD          = ACTION_ADD;
    public static final Icon DATA_EDITOR_DELETE_RECORD          = ACTION_REMOVE;
    public static final Icon DATA_EDITOR_FETCH_NEXT_RECORDS     = ACTION_RESUME;
    public static final Icon DATA_EDITOR_EDIT_RECORD            = load("/img/tools/RecordViewer.svg");
    public static final Icon DATA_EDITOR_NEXT_RECORD            = AllIcons.Actions.Play_forward;
    public static final Icon DATA_EDITOR_PREVIOUS_RECORD        = AllIcons.Actions.Play_back;
    public static final Icon DATA_EDITOR_FIRST_RECORD           = AllIcons.Actions.Play_first;
    public static final Icon DATA_EDITOR_LAST_RECORD            = AllIcons.Actions.Play_last;
    public static final Icon DATA_EDITOR_LOCKED                 = AllIcons.Ide.Readonly;
    public static final Icon DATA_EDITOR_UNLOCKED               = AllIcons.Ide.Readwrite;
    public static final Icon DATA_EDITOR_SORT_ASC               = load("/img/action/DataEditorSortAscending.png");
    public static final Icon DATA_EDITOR_SORT_DESC              = load("/img/action/DataEditorSortDescending.png");
    public static final Icon DATA_EDITOR_STOP_LOADING           = ACTION_STOP;
    public static final Icon DATA_EDITOR_RELOAD_DATA            = ACTION_RELOAD;
    public static final Icon DATA_EDITOR_BROWSE                 = load("/img/ButtonBrowse.png");
    public static final Icon DATA_EDITOR_CALENDAR               = load("/img/ButtonCalendar.png");
    public static final Icon DATA_EDITOR_LIST                   = load("/img/ButtonList.png");

    public static final Icon DATA_EXPORT =    load("/img/action/DataExport.png");
    public static final Icon DATA_IMPORT =    load("/img/action/DataImport.png");
    public static final Icon DATA_COLUMNS =    load("/img/action/ColumnSetup.png");
    public static final Icon DATA_SORTING =    load("/img/action/DataSorting.png");
    public static final Icon DATA_SORTING_ASC =    load("/img/action/DataSortingAsc.png");
    public static final Icon DATA_SORTING_DESC =    load("/img/action/DataSortingDesc.png");

    public static final Icon TOP_LEVEL_FILTER =    load("/img/TopLevelFilter.png");
    public static final Icon DATASET_FILTER =    load("/img/filter/DatasetFilter.png");
    public static final Icon DATASET_FILTER_CLEAR =    load("/img/filter/DatasetFilterClear.png");
    public static final Icon DATASET_FILTER_NEW =    load("/img/filter/DatasetFilterNew.png");
    public static final Icon DATASET_FILTER_EDIT =    load("/img/filter/DatasetFilterEdit.png");
    public static final Icon DATASET_FILTER_BASIC =    load("/img/filter/DatasetFilterBasic.png");
    public static final Icon DATASET_FILTER_BASIC_ERR =    load("/img/filter/DatasetFilterBasicErr.png");
    public static final Icon DATASET_FILTER_BASIC_TEMP =    load("/img/filter/DatasetFilterBasicTemp.png");
    public static final Icon DATASET_FILTER_BASIC_TEMP_ERR =    load("/img/filter/DatasetFilterBasicTempErr.png");
    public static final Icon DATASET_FILTER_CUSTOM =    load("/img/filter/DatasetFilterCustom.png");
    public static final Icon DATASET_FILTER_CUSTOM_ERR =    load("/img/filter/DatasetFilterCustomErr.png");
    public static final Icon DATASET_FILTER_GLOBAL =    load("/img/filter/DatasetFilterGlobal.png");
    public static final Icon DATASET_FILTER_GLOBAL_ERR =    load("/img/filter/DatasetFilterGlobalErr.png");
    public static final Icon DATASET_FILTER_EMPTY =    load("/img/filter/DatasetFilterEmpty.png");

    public static final Icon DATASET_FILTER_CONDITION_NEW =    load("/img/NewFilterCondition.png");


    public static final Icon CONDITION_JOIN_TYPE =    load("/img/JoinTypeSwitch.png");

    public static final Icon TEXT_CELL_EDIT_ACCEPT = load("/img/CellEditAccept.png");
    public static final Icon TEXT_CELL_EDIT_REVERT = load("/img/CellEditRevert.png");
    public static final Icon TEXT_CELL_EDIT_DELETE = load("/img/CellEditDelete.png");
    public static final Icon ARRAY_CELL_EDIT_ADD    = load("/img/CellEditAdd.png");
    public static final Icon ARRAY_CELL_EDIT_REMOVE = load("/img/CellEditRemove.png");

    public static final Icon CALENDAR_NEXT_MONTH = load("/img/CalendarNextMonth.png");
    public static final Icon CALENDAR_NEXT_YEAR = load("/img/CalendarNextYear.png");
    public static final Icon CALENDAR_PREVIOUS_MONTH = load("/img/CalendarPreviousMonth.png");
    public static final Icon CALENDAR_PREVIOUS_YEAR = load("/img/CalendarPreviousYear.png");
    public static final Icon CALENDAR_CLEAR_TIME = load("/img/CalendarResetTime.png");

    public static final Icon EXEC_MESSAGES_INFO             = AllIcons.General.Information;
    public static final Icon EXEC_MESSAGES_INFO_INACTIVE    = AllIcons.General.Note;
    public static final Icon EXEC_MESSAGES_WARNING          = AllIcons.General.Warning;
    public static final Icon EXEC_MESSAGES_ERROR            = AllIcons.General.Error;
    public static final Icon EXEC_MESSAGES_WARNING_INACTIVE = load("/img/common/WarningInactive.svg");
    public static final Icon EXEC_MESSAGES_ERROR_INACTIVE   = load("/img/common/ErrorInactive.svg");

    public static final Icon FILE_CONNECTION_MAPPING = load("/img/FileConnection.png");
    public static final Icon FILE_SCHEMA_MAPPING = load("/img/FileSchema.png");
    public static final Icon FILE_SESSION_MAPPING = load("/img/FileSession.png");

    public static final Icon CODE_EDITOR_SAVE_TO_DATABASE = ACTION_SAVE;
    public static final Icon CODE_EDITOR_SAVE_TO_FILE     = ACTION_SAVE;
    public static final Icon CODE_EDITOR_RESET            = ACTION_REVERT;
    public static final Icon CODE_EDITOR_RELOAD           = ACTION_RELOAD;
    public static final Icon CODE_EDITOR_DIFF             = load("/img/action/ShowDiff.svg");
    public static final Icon CODE_EDITOR_DIFF_DB          = load("/img/action/ShowDbDiff.svg");
    public static final Icon CODE_EDITOR_DDL_FILE         = load("/img/DDLFile.png");
    public static final Icon CODE_EDITOR_DDL_FILE_NEW     = load("/img/DDLFileNew.png");
    public static final Icon CODE_EDITOR_SPEC             = load("/img/CodeSpec.png");
    public static final Icon CODE_EDITOR_BODY             = load("/img/CodeBody.png");

    public static final Icon OBJECT_COMPILE = load("/img/action/Compile.png");
    public static final Icon OBJECT_COMPILE_DEBUG = load("/img/action/CompileDebug.png");
    //public static final Icon OBJECT_COMPILE_KEEP = load("/main.resources.img/CompileKeep.png");
    public static final Icon OBJECT_COMPILE_ASK = load("/img/action/CompileAsk.png");
    public static final Icon OBJECT_EDIT_SOURCE = load("/img/action/EditSource.svg");
    public static final Icon OBJECT_EDIT_DATA = load("/img/action/EditData.svg");
    public static final Icon OBJECT_VIEW_DATA = load("/img/ViewData.png");

    public static final Icon CONNECTION_COMMIT   = load("/img/action/ConnectionCommit.svg", "CONNECTION_COMMIT");
    public static final Icon CONNECTION_ROLLBACK = load("/img/action/ConnectionRollback.svg", "CONNECTION_ROLLBACK");
    public static final Icon CONNECTION_DUPLICATE = load("/img/action/DuplicateConnection.png");
    public static final Icon CONNECTION_COPY = load("/img/action/CopyConnection.png");
    public static final Icon CONNECTION_PASTE = load("/img/action/PasteConnection.png");

    public static final Icon COMMON_DIRECTION_IN = load("/img/common/DirectionIn.png");
    public static final Icon COMMON_DIRECTION_OUT = load("/img/common/DirectionOut.png");
    public static final Icon COMMON_DIRECTION_IN_OUT = load("/img/common/DirectionInOut.png");

    public static final Icon CONNECTION_VIRTUAL       = load("/img/connection/ConnectionVirtual.svg");
    public static final Icon CONNECTION_CONNECTED     = load("/img/connection/ConnectionConnected.svg");
    public static final Icon CONNECTION_ACTIVE        = load("/img/connection/ConnectionActive.svg");
    public static final Icon CONNECTION_BUSY          = load("/img/connection/ConnectionBusy.svg");
    public static final Icon CONNECTION_CONNECTED_NEW = load("/img/connection/ConnectionConnectedNew.svg");
    public static final Icon CONNECTION_INACTIVE      = load("/img/connection/ConnectionInactive.svg");
    public static final Icon CONNECTION_DISABLED      = load("/img/connection/ConnectionDisabled.svg");
    public static final Icon CONNECTION_NEW           = load("/img/connection/ConnectionNew.svg");
    public static final Icon CONNECTION_INVALID       = load("/img/connection/ConnectionInvalid.svg");
    public static final Icon CONNECTIONS              = load("/img/connection/Connections.svg");

    public static final Icon SESSION_CUSTOM               = load("/img/connection/SessionCustom.png");
    public static final Icon SESSION_CUSTOM_CONNECTED     = load("/img/connection/SessionCustomConnected.png");
    public static final Icon SESSION_CUSTOM_TRANSACTIONAL = load("/img/connection/SessionCustomTransactional.png");
    public static final Icon SESSION_MAIN                 = load("/img/connection/SessionMain.png");
    public static final Icon SESSION_MAIN_CONNECTED       = load("/img/connection/SessionMainConnected.png");
    public static final Icon SESSION_MAIN_TRANSACTIONAL   = load("/img/connection/SessionMainTransactional.png");
    public static final Icon SESSION_POOL                 = load("/img/connection/SessionPool.png");
    public static final Icon SESSION_POOL_CONNECTED       = load("/img/connection/SessionPoolConnected.png");
    public static final Icon SESSION_POOL_TRANSACTIONAL   = load("/img/connection/SessionPoolTransactional.png");
    public static final Icon SESSION_DEBUG                = load("/img/connection/SessionDebug.png");
    public static final Icon SESSION_DEBUG_CONNECTED      = load("/img/connection/SessionDebugConnected.png");
    public static final Icon SESSION_DEBUG_TRANSACTIONAL  = load("/img/connection/SessionDebugTransactional.png");


    public static final Icon DB_ORACLE            = AllIcons.Providers.Oracle;
    public static final Icon DB_POSTGRESQL        = AllIcons.Providers.Postgresql;
    public static final Icon DB_MYSQL             = AllIcons.Providers.Mysql;
    public static final Icon DB_SQLITE            = AllIcons.Providers.Sqlite;
    public static final Icon DB_GENERIC           = load("/img/database/Generic.svg");

    public static final Icon DB_ORACLE_LARGE      = IconUtil.toSize(AllIcons.Providers.Oracle, 32, 32);
    public static final Icon DB_POSTGRESQL_LARGE  = IconUtil.toSize(AllIcons.Providers.Postgresql, 32, 32);
    public static final Icon DB_MYSQL_LARGE       = IconUtil.toSize(AllIcons.Providers.Mysql, 32, 32);
    public static final Icon DB_SQLITE_LARGE      = IconUtil.toSize(AllIcons.Providers.Sqlite, 32, 32);
    public static final Icon DB_GENERIC_LARGE     = IconUtil.toSize(DB_GENERIC, 32, 32);

//    public static final Icon DBO_ARGUMENT_IN         = createRowIcon(DBO_ARGUMENT, COMMON_DIRECTION_IN);
//    public static final Icon DBO_ARGUMENT_OUT        = createRowIcon(DBO_ARGUMENT, COMMON_DIRECTION_OUT);
//    public static final Icon DBO_ARGUMENT_IN_OUT     = createRowIcon(DBO_ARGUMENT, COMMON_DIRECTION_IN_OUT);


    public static final Icon DBO_ATTRIBUTE                       = load("/img/object/Attribute.png");
    public static final Icon DBO_ATTRIBUTES                      = load("/img/object/Attributes.png");
    public static final Icon DBO_ARGUMENT                        = load("/img/object/Argument.png");
    public static final Icon DBO_ARGUMENTS                       = load("/img/object/Arguments.png");
    public static final Icon DBO_ARGUMENT_IN                     = load("/img/object/ArgumentIn.png");
    public static final Icon DBO_ARGUMENT_OUT                    = load("/img/object/ArgumentOut.png");
    public static final Icon DBO_ARGUMENT_IN_OUT                 = load("/img/object/ArgumentInOut.png");
    public static final Icon DBO_CONSOLE                         = load("/img/object/console/Console.svg");
    public static final Icon DBO_CONSOLES                        = load("/img/object/console/Consoles.svg");
    public static final Icon DBO_CONSOLE_DEBUG                   = load("/img/object/console/ConsoleDebug.svg");
    public static final Icon DBO_CLUSTER                         = load("/img/object/Cluster.png");
    public static final Icon DBO_CLUSTERS                        = load("/img/object/Clusters.png");
    public static final Icon DBO_COLUMN                          = load("/img/object/column/Column.svg");
    public static final Icon DBO_COLUMN_PK                       = load("/img/object/column/ColumnPk.svg");
    public static final Icon DBO_COLUMN_FK                       = load("/img/object/column/ColumnFk.svg");
    public static final Icon DBO_COLUMN_PFK                      = load("/img/object/column/ColumnPkFk.svg");
    public static final Icon DBO_COLUMN_HIDDEN                   = load("/img/object/column/ColumnHidden.svg");
    public static final Icon DBO_COLUMNS                         = load("/img/object/column/Columns.svg");
    public static final Icon DBO_CONSTRAINT                      = load("/img/object/constraint/Constraint.svg");
    public static final Icon DBO_CONSTRAINT_DISABLED             = load("/img/object/constraint/ConstraintDisabled.svg");
    public static final Icon DBO_CONSTRAINTS                     = load("/img/object/constraint/Constraints.svg");
    public static final Icon DBO_DATABASE_LINK                   = load("/img/object/DatabaseLink.png");
    public static final Icon DBO_DATABASE_LINKS                  = load("/img/object/DatabaseLinks.png");
    public static final Icon DBO_DIMENSION                       = load("/img/object/Dimension.png");
    public static final Icon DBO_DIMENSIONS                      = load("/img/object/Dimensions.png");
    public static final Icon DBO_FUNCTION                        = load("/img/object/function/Function.svg", "DBO_FUNCTION");
    public static final Icon DBO_FUNCTION_DEBUG                  = load("/img/object/function/FunctionDebug.svg");
    public static final Icon DBO_FUNCTION_ERR                    = load("/img/object/function/FunctionErr.svg");
    public static final Icon DBO_FUNCTIONS                       = load("/img/object/function/Functions.svg");
    public static final Icon DBO_INDEX                           = load("/img/object/index/Index.svg");
    public static final Icon DBO_INDEX_DISABLED                  = load("/img/object/index/IndexDisabled.svg");
    public static final Icon DBO_INDEXES                         = load("/img/object/index/Indexes.svg");
    public static final Icon DBO_JAVA_CLASS                      = AllIcons.FileTypes.JavaClass;
    public static final Icon DBO_JAVA_CLASSES                    = AllIcons.FileTypes.JavaClass;
    public static final Icon DBO_MATERIALIZED_VIEW               = load("/img/object/view/MaterializedView.svg");
    public static final Icon DBO_MATERIALIZED_VIEWS              = load("/img/object/view/MaterializedViews.svg");
    public static final Icon DBO_METHOD                          = load("/img/object/Method.png");
    public static final Icon DBO_METHODS                         = load("/img/object/Methods.png");
    public static final Icon DBO_NESTED_TABLE                    = load("/img/object/NestedTable.png");
    public static final Icon DBO_NESTED_TABLES                   = load("/img/object/NestedTables.png");
    public static final Icon DBO_PACKAGE                         = load("/img/object/package/Package.svg");
    public static final Icon DBO_PACKAGE_ERR                     = load("/img/object/package/PackageErr.svg");
    public static final Icon DBO_PACKAGE_DEBUG                   = load("/img/object/package/PackageDebug.svg");
    public static final Icon DBO_PACKAGES                        = load("/img/object/package/Packages.svg");
    public static final Icon DBO_PACKAGE_SPEC                    = load("/img/object/package/PackageSpec.png", "DBO_PACKAGE_SPEC");
    public static final Icon DBO_PACKAGE_BODY                    = load("/img/object/package/PackageBody.png", "DBO_PACKAGE_BODY");
    public static final Icon DBO_PROCEDURE                       = load("/img/object/Procedure.png", "DBO_PROCEDURE");
    public static final Icon DBO_PROCEDURE_ERR                   = load("/img/object/ProcedureErr.png");
    public static final Icon DBO_PROCEDURE_DEBUG                 = load("/img/object/ProcedureDebug.png");
    public static final Icon DBO_PROCEDURES                      = load("/img/object/Procedures.png");
    public static final Icon DBO_PRIVILEGE                       = load("/img/object/privilege/Privilege.svg");
    public static final Icon DBO_PRIVILEGES                      = load("/img/object/privilege/Privileges.svg");
    public static final Icon DBO_ROLE                            = load("/img/object/role/Role.svg");
    public static final Icon DBO_ROLES                           = load("/img/object/role/Roles.svg");
    public static final Icon DBO_SCHEMA                          = load("/img/object/schema/Schema.svg");
    public static final Icon DBO_SCHEMAS                         = load("/img/object/schema/Schemas.svg");
    public static final Icon DBO_SYNONYM                         = load("/img/object/synonym/Synonym.svg");
    public static final Icon DBO_SYNONYM_ERR                     = load("/img/object/synonym/SynonymErr.svg");
    public static final Icon DBO_SYNONYMS                        = load("/img/object/synonym/Synonyms.svg");
    public static final Icon DBO_SEQUENCE                        = load("/img/object/sequence/Sequence.svg");
    public static final Icon DBO_SEQUENCES                       = load("/img/object/sequence/Sequences.svg");
    public static final Icon DBO_TMP_TABLE                       = load("/img/object/table/TableTmp.svg");
    public static final Icon DBO_TMP_TABLES                      = load("/img/object/table/TablesTmp.svg");
    public static final Icon DBO_TABLE                           = load("/img/object/table/Table.svg");
    public static final Icon DBO_TABLES                          = load("/img/object/table/Tables.svg");
    public static final Icon DBO_TRIGGER                         = load("/img/object/trigger/Trigger.svg", "DBO_TRIGGER");
    public static final Icon DBO_TRIGGER_ERR                     = load("/img/object/trigger/TriggerErr.svg");
    public static final Icon DBO_TRIGGER_DEBUG                   = load("/img/object/trigger/TriggerDebug.svg");
    public static final Icon DBO_TRIGGER_ERR_DISABLED            = load("/img/object/trigger/TriggerErrDisabled.svg");
    public static final Icon DBO_TRIGGER_DISABLED                = load("/img/object/trigger/TriggerDisabled.svg");
    public static final Icon DBO_TRIGGER_DISABLED_DEBUG          = load("/img/object/trigger/TriggerDisabledDebug.svg");
    public static final Icon DBO_TRIGGERS                        = load("/img/object/trigger/Triggers.svg");
    public static final Icon DBO_DATABASE_TRIGGER                = load("/img/object/DatabaseTrigger.png", "DBO_DATABASE_TRIGGER");
    public static final Icon DBO_DATABASE_TRIGGER_ERR            = load("/img/object/DatabaseTriggerErr.png");
    public static final Icon DBO_DATABASE_TRIGGER_DEBUG          = load("/img/object/DatabaseTriggerDebug.png");
    public static final Icon DBO_DATABASE_TRIGGER_ERR_DISABLED   = load("/img/object/DatabaseTriggerErrDisabled.png");
    public static final Icon DBO_DATABASE_TRIGGER_DISABLED       = load("/img/object/DatabaseTriggerDisabled.png");
    public static final Icon DBO_DATABASE_TRIGGER_DISABLED_DEBUG = load("/img/object/DatabaseTriggerDisabledDebug.png");
    public static final Icon DBO_DATABASE_TRIGGERS               = load("/img/object/DatabaseTriggers.png");
    public static final Icon DBO_TYPE                            = load("/img/object/type/Type.svg");
    public static final Icon DBO_TYPE_COLLECTION                 = load("/img/object/type/TypeCollection.svg");
    public static final Icon DBO_TYPE_COLLECTION_ERR             = load("/img/object/type/TypeCollectionErr.svg");
    public static final Icon DBO_TYPE_ERR                        = load("/img/object/type/TypeErr.svg");
    public static final Icon DBO_TYPE_DEBUG                      = load("/img/object/type/TypeDebug.svg");
    public static final Icon DBO_TYPE_SPEC                       = load("/img/object/type/TypeSpec.png", "DBO_TYPE_SPEC");
    public static final Icon DBO_TYPE_BODY                       = load("/img/object/type/TypeBody.png", "DBO_TYPE_BODY");
    public static final Icon DBO_TYPES                           = load("/img/object/type/Types.svg");
    public static final Icon DBO_USER                            = load("/img/object/user/User.svg");
    public static final Icon DBO_USER_EXPIRED                    = load("/img/object/user/UserExpired.svg");
    public static final Icon DBO_USER_LOCKED                     = load("/img/object/user/UserLocked.svg");
    public static final Icon DBO_USER_EXPIRED_LOCKED             = load("/img/object/user/UserExpiredLocked.svg");
    public static final Icon DBO_USERS                           = load("/img/object/user/Users.svg");
    public static final Icon DBO_VIEW                            = load("/img/object/view/View.svg");
    public static final Icon DBO_VIEW_SYNONYM                    = load("/img/object/view/ViewSynonym.png");
    public static final Icon DBO_VIEWS                           = load("/img/object/view/Views.svg");
    public static final Icon DBO_VARIABLE                        = load("/img/object/Variable.png");
    public static final Icon DBO_CURSOR                          = load("/img/object/Cursor.png");
    public static final Icon DBO_LABEL_PK_FK                     = load("/img/object/PrimaryKeyForeignKey.png");
    public static final Icon DBO_LABEL_PK                        = load("/img/object/PrimaryKey.png");
    public static final Icon DBO_LABEL_FK                        = load("/img/object/ForeignKey.png");


    public static final Icon DBO_OUTGOING_REF                    = load("/img/object/OutgoingReference.png");
    public static final Icon DBO_INCOMING_REF                    = load("/img/object/IncomingReference.png");
    public static final Icon DBO_OUTGOING_REF_SOFT               = load("/img/object/OutgoingRefSoft.png");
    public static final Icon DBO_INCOMING_REF_SOFT               = load("/img/object/IncomingRefSoft.png");

    public static final Icon SB_FILTER_SERVER                    = load("/img/filter/SessionFilterServer.png");
    public static final Icon SB_FILTER_STATUS                    = load("/img/filter/SessionFilterStatus.png");
    public static final Icon SB_FILTER_USER                      = load("/img/filter/SessionFilterUser.png");

    public static final Icon DEBUG_INVALID_BREAKPOINT            = AllIcons.Debugger.Db_invalid_breakpoint;

    public static final Icon SPACE                        = load("/img/Space.png");
    public static final Icon TREE_BRANCH                  = load("/img/TreeBranch.png");
    public static final Icon SMALL_TREE_BRANCH            = load("/img/SmallTreeBranch.png");

    public static Icon getIcon(String key) {
        return IconLoader.REGISTRY.get(key);
    }

    public static Icon scaleToWidth(Icon icon, float newWidth) {
        if (icon instanceof ScalableIcon) {
            ScalableIcon scalableIcon = (ScalableIcon) icon;

            int iconWidth = scalableIcon.getIconWidth();
            if (newWidth != iconWidth) {
                return scalableIcon.scale(newWidth / iconWidth);
            }
        }
        return icon;
    }

    private static Icon createRowIcon(Icon left, Icon right) {
        RowIcon rowIcon = new RowIcon(2);
        rowIcon.setIcon(left, 0);
        rowIcon.setIcon(right, 1);
        return rowIcon;
    }

}
