package com.dbn.language.sql.dialect.sqlite;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class SqliteSQLParserFlexLexer
%implements FlexLexer
%public
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    private TokenTypeBundle tt;
    private SharedTokenTypeBundle stt;
    public SqliteSQLParserFlexLexer(TokenTypeBundle tt) {
        this.tt = tt;
        this.stt = tt.getSharedTokenTypes();
    }
%}

WHITE_SPACE= {white_space_char}|{line_terminator}
line_terminator = \r|\n|\r\n
input_character = [^\r\n]
white_space = [ \t\f]
white_space_char= [ \n\r\t\f]
ws  = {WHITE_SPACE}+
wso = {WHITE_SPACE}*

comment_tail =([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
BLOCK_COMMENT=("/*"[^]{comment_tail})|"/*"
LINE_COMMENT = "--"{input_character}*

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = `[^\`]*`?|\"[^\"]*\"?|\[[^\]]*\]?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"swe7"|"tis620"|"ucs2"|"ujis"|"utf8"

string_simple_quoted      = "'"([^\']|"''"|{WHITE_SPACE})*"'"?
STRING = ("n"|"_"{CHARSET})?{wso}{string_simple_quoted}

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

VARIABLE = ":"{wso}({IDENTIFIER}|{INTEGER})

%state DIV
%%

{WHITE_SPACE}+   { return stt.whiteSpace; }

{BLOCK_COMMENT}  { return stt.blockComment; }
{LINE_COMMENT}   { return stt.lineComment; }

{VARIABLE}       { return stt.variable; }
{INTEGER}        { return stt.integer; }
{NUMBER}         { return stt.number; }
{STRING}         { return stt.string; }

"="{wso}"="      { return tt.getOperatorTokenType(0); }
"|"{wso}"|"      { return tt.getOperatorTokenType(1); }
"<"{wso}"="      { return tt.getOperatorTokenType(2); }
">"{wso}"="      { return tt.getOperatorTokenType(3); }
"<"{wso}">"      { return tt.getOperatorTokenType(4); }
"!"{wso}"="      { return tt.getOperatorTokenType(5); }
":"{wso}"="      { return tt.getOperatorTokenType(6); }
"="{wso}">"      { return tt.getOperatorTokenType(7); }
".."             { return tt.getOperatorTokenType(8); }
"::"             { return tt.getOperatorTokenType(9); }

"("{wso}"+"{wso}")"  {return tt.getTokenType("CT_OUTER_JOIN");}

"@" {return tt.getCharacterTokenType(0);}
":" {return tt.getCharacterTokenType(1);}
"," {return tt.getCharacterTokenType(2);}
"." {return tt.getCharacterTokenType(3);}
"=" {return tt.getCharacterTokenType(4);}
"!" {return tt.getCharacterTokenType(5);}
">" {return tt.getCharacterTokenType(6);}
"#" {return tt.getCharacterTokenType(7);}
"[" {return tt.getCharacterTokenType(8);}
"{" {return tt.getCharacterTokenType(9);}
"(" {return tt.getCharacterTokenType(10);}
"<" {return tt.getCharacterTokenType(11);}
"-" {return tt.getCharacterTokenType(12);}
"%" {return tt.getCharacterTokenType(13);}
"+" {return tt.getCharacterTokenType(14);}
"]" {return tt.getCharacterTokenType(15);}
"}" {return tt.getCharacterTokenType(16);}
")" {return tt.getCharacterTokenType(17);}
";" {return tt.getCharacterTokenType(18);}
"/" {return tt.getCharacterTokenType(19);}
"*" {return tt.getCharacterTokenType(20);}
"|" {return tt.getCharacterTokenType(21);}





// MARKER_BEGIN_DATATYPES
"bigint" {return tt.dtt(0);}
"blob" {return tt.dtt(1);}
"boolean" {return tt.dtt(2);}
"character" {return tt.dtt(3);}
"clob" {return tt.dtt(4);}
"date" {return tt.dtt(5);}
"datetime" {return tt.dtt(6);}
"decimal" {return tt.dtt(7);}
"double" {return tt.dtt(8);}
"double"{ws}"precision" {return tt.dtt(9);}
"float" {return tt.dtt(10);}
"int" {return tt.dtt(11);}
"int2" {return tt.dtt(12);}
"int8" {return tt.dtt(13);}
"integer" {return tt.dtt(14);}
"mediumint" {return tt.dtt(15);}
"native"{ws}"character" {return tt.dtt(16);}
"nchar" {return tt.dtt(17);}
"numeric" {return tt.dtt(18);}
"nvarchar" {return tt.dtt(19);}
"real" {return tt.dtt(20);}
"smallint" {return tt.dtt(21);}
"text" {return tt.dtt(22);}
"time" {return tt.dtt(23);}
"tinyint" {return tt.dtt(24);}
"unsigned"{ws}"big"{ws}"int" {return tt.dtt(25);}
"varchar" {return tt.dtt(26);}
"varying"{ws}"character" {return tt.dtt(27);}
// MARKER_END_DATATYPES




// MARKER_BEGIN_KEYWORDS
"abort" {return tt.ktt(0);}
"action" {return tt.ktt(1);}
"add" {return tt.ktt(2);}
"after" {return tt.ktt(3);}
"all" {return tt.ktt(4);}
"alter" {return tt.ktt(5);}
"analyze" {return tt.ktt(6);}
"and" {return tt.ktt(7);}
"as" {return tt.ktt(8);}
"asc" {return tt.ktt(9);}
"attach" {return tt.ktt(10);}
"autoincrement" {return tt.ktt(11);}
"before" {return tt.ktt(12);}
"begin" {return tt.ktt(13);}
"between" {return tt.ktt(14);}
"by" {return tt.ktt(15);}
"cascade" {return tt.ktt(16);}
"case" {return tt.ktt(17);}
"cast" {return tt.ktt(18);}
"check" {return tt.ktt(19);}
"collate" {return tt.ktt(20);}
"column" {return tt.ktt(21);}
"commit" {return tt.ktt(22);}
"conflict" {return tt.ktt(23);}
"constraint" {return tt.ktt(24);}
"create" {return tt.ktt(25);}
"cross" {return tt.ktt(26);}
"current_date" {return tt.ktt(27);}
"current_time" {return tt.ktt(28);}
"current_timestamp" {return tt.ktt(29);}
"database" {return tt.ktt(30);}
"default" {return tt.ktt(31);}
"deferrable" {return tt.ktt(32);}
"deferred" {return tt.ktt(33);}
"delete" {return tt.ktt(34);}
"desc" {return tt.ktt(35);}
"detach" {return tt.ktt(36);}
"distinct" {return tt.ktt(37);}
"drop" {return tt.ktt(38);}
"each" {return tt.ktt(39);}
"else" {return tt.ktt(40);}
"end" {return tt.ktt(41);}
"escape" {return tt.ktt(42);}
"except" {return tt.ktt(43);}
"exclusive" {return tt.ktt(44);}
"exists" {return tt.ktt(45);}
"explain" {return tt.ktt(46);}
"fail" {return tt.ktt(47);}
"for" {return tt.ktt(48);}
"foreign" {return tt.ktt(49);}
"from" {return tt.ktt(50);}
"full" {return tt.ktt(51);}
"glob" {return tt.ktt(52);}
"group" {return tt.ktt(53);}
"having" {return tt.ktt(54);}
"if" {return tt.ktt(55);}
"ignore" {return tt.ktt(56);}
"immediate" {return tt.ktt(57);}
"in" {return tt.ktt(58);}
"index" {return tt.ktt(59);}
"indexed" {return tt.ktt(60);}
"initially" {return tt.ktt(61);}
"inner" {return tt.ktt(62);}
"insert" {return tt.ktt(63);}
"instead" {return tt.ktt(64);}
"intersect" {return tt.ktt(65);}
"into" {return tt.ktt(66);}
"is" {return tt.ktt(67);}
"isnull" {return tt.ktt(68);}
"join" {return tt.ktt(69);}
"key" {return tt.ktt(70);}
"left" {return tt.ktt(71);}
"like" {return tt.ktt(72);}
"limit" {return tt.ktt(73);}
"match" {return tt.ktt(74);}
"natural" {return tt.ktt(75);}
"no" {return tt.ktt(76);}
"not" {return tt.ktt(77);}
"notnull" {return tt.ktt(78);}
"null" {return tt.ktt(79);}
"of" {return tt.ktt(80);}
"off" {return tt.ktt(81);}
"offset" {return tt.ktt(82);}
"on" {return tt.ktt(83);}
"or" {return tt.ktt(84);}
"order" {return tt.ktt(85);}
"outer" {return tt.ktt(86);}
"plan" {return tt.ktt(87);}
"pragma" {return tt.ktt(88);}
"primary" {return tt.ktt(89);}
"query" {return tt.ktt(90);}
"raise" {return tt.ktt(91);}
"recursive" {return tt.ktt(92);}
"references" {return tt.ktt(93);}
"regexp" {return tt.ktt(94);}
"reindex" {return tt.ktt(95);}
"release" {return tt.ktt(96);}
"rename" {return tt.ktt(97);}
"replace" {return tt.ktt(98);}
"restrict" {return tt.ktt(99);}
"right" {return tt.ktt(100);}
"rollback" {return tt.ktt(101);}
"row" {return tt.ktt(102);}
"rowid" {return tt.ktt(103);}
"savepoint" {return tt.ktt(104);}
"select" {return tt.ktt(105);}
"set" {return tt.ktt(106);}
"table" {return tt.ktt(107);}
"temp" {return tt.ktt(108);}
"temporary" {return tt.ktt(109);}
"then" {return tt.ktt(110);}
"to" {return tt.ktt(111);}
"transaction" {return tt.ktt(112);}
"trigger" {return tt.ktt(113);}
"union" {return tt.ktt(114);}
"unique" {return tt.ktt(115);}
"update" {return tt.ktt(116);}
"using" {return tt.ktt(117);}
"vacuum" {return tt.ktt(118);}
"values" {return tt.ktt(119);}
"view" {return tt.ktt(120);}
"virtual" {return tt.ktt(121);}
"when" {return tt.ktt(122);}
"where" {return tt.ktt(123);}
"with" {return tt.ktt(124);}
"without" {return tt.ktt(125);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"avg" {return tt.ftt(1);}
"changes" {return tt.ftt(2);}
"char" {return tt.ftt(3);}
"coalesce" {return tt.ftt(4);}
"count" {return tt.ftt(5);}
"group_concat" {return tt.ftt(6);}
"hex" {return tt.ftt(7);}
"ifnull" {return tt.ftt(8);}
"instr" {return tt.ftt(9);}
"json" {return tt.ftt(10);}
"json_array" {return tt.ftt(11);}
"json_array_length" {return tt.ftt(12);}
"json_extract" {return tt.ftt(13);}
"json_insert" {return tt.ftt(14);}
"json_object" {return tt.ftt(15);}
"json_remove" {return tt.ftt(16);}
"json_replace" {return tt.ftt(17);}
"json_set" {return tt.ftt(18);}
"json_type" {return tt.ftt(19);}
"json_valid" {return tt.ftt(20);}
"julianday" {return tt.ftt(21);}
"last_insert_rowid" {return tt.ftt(22);}
"length" {return tt.ftt(23);}
"likelihood" {return tt.ftt(24);}
"likely" {return tt.ftt(25);}
"load_extension" {return tt.ftt(26);}
"lower" {return tt.ftt(27);}
"ltrim" {return tt.ftt(28);}
"max" {return tt.ftt(29);}
"min" {return tt.ftt(30);}
"nullif" {return tt.ftt(31);}
"printf" {return tt.ftt(32);}
"quote" {return tt.ftt(33);}
"random" {return tt.ftt(34);}
"randomblob" {return tt.ftt(35);}
"round" {return tt.ftt(36);}
"rtrim" {return tt.ftt(37);}
"soundex" {return tt.ftt(38);}
"sqlite_compileoption_get" {return tt.ftt(39);}
"sqlite_compileoption_used" {return tt.ftt(40);}
"sqlite_source_id" {return tt.ftt(41);}
"sqlite_version" {return tt.ftt(42);}
"strftime" {return tt.ftt(43);}
"substr" {return tt.ftt(44);}
"sum" {return tt.ftt(45);}
"total" {return tt.ftt(46);}
"total_changes" {return tt.ftt(47);}
"trim" {return tt.ftt(48);}
"typeof" {return tt.ftt(49);}
"unicode" {return tt.ftt(50);}
"unlikely" {return tt.ftt(51);}
"upper" {return tt.ftt(52);}
"zeroblob" {return tt.ftt(53);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
"application_id" {return tt.ptt(0);}
"automatic_index" {return tt.ptt(1);}
"auto_vacuum" {return tt.ptt(2);}
"busy_timeout" {return tt.ptt(3);}
"cache_size" {return tt.ptt(4);}
"cache_spill" {return tt.ptt(5);}
"case_sensitive_like" {return tt.ptt(6);}
"cell_size_check" {return tt.ptt(7);}
"checkpoint_fullfsync" {return tt.ptt(8);}
"collation_list" {return tt.ptt(9);}
"compile_options" {return tt.ptt(10);}
"count_changes" {return tt.ptt(11);}
"database_list" {return tt.ptt(12);}
"data_store_directory" {return tt.ptt(13);}
"data_version" {return tt.ptt(14);}
"default_cache_size" {return tt.ptt(15);}
"defer_foreign_keys" {return tt.ptt(16);}
"empty_result_callbacks" {return tt.ptt(17);}
"encoding" {return tt.ptt(18);}
"foreign_keys" {return tt.ptt(19);}
"foreign_key_check" {return tt.ptt(20);}
"foreign_key_list" {return tt.ptt(21);}
"freelist_count" {return tt.ptt(22);}
"fullfsync" {return tt.ptt(23);}
"full_column_names" {return tt.ptt(24);}
"ignore_check_constraints" {return tt.ptt(25);}
"incremental_vacuum" {return tt.ptt(26);}
"index_info" {return tt.ptt(27);}
"index_list" {return tt.ptt(28);}
"index_xinfo" {return tt.ptt(29);}
"integrity_check" {return tt.ptt(30);}
"journal_mode" {return tt.ptt(31);}
"journal_size_limit" {return tt.ptt(32);}
"legacy_file_format" {return tt.ptt(33);}
"locking_mode" {return tt.ptt(34);}
"max_page_count" {return tt.ptt(35);}
"mmap_size" {return tt.ptt(36);}
"page_count" {return tt.ptt(37);}
"page_size" {return tt.ptt(38);}
"parser_trace" {return tt.ptt(39);}
"query_only" {return tt.ptt(40);}
"quick_check" {return tt.ptt(41);}
"read_uncommitted" {return tt.ptt(42);}
"recursive_triggers" {return tt.ptt(43);}
"reverse_unordered_selects" {return tt.ptt(44);}
"schema_version" {return tt.ptt(45);}
"secure_delete" {return tt.ptt(46);}
"short_column_names" {return tt.ptt(47);}
"shrink_memory" {return tt.ptt(48);}
"soft_heap_limit" {return tt.ptt(49);}
"stats" {return tt.ptt(50);}
"synchronous" {return tt.ptt(51);}
"table_info" {return tt.ptt(52);}
"temp_store" {return tt.ptt(53);}
"temp_store_directory" {return tt.ptt(54);}
"threads" {return tt.ptt(55);}
"user_version" {return tt.ptt(56);}
"vdbe_addoptrace" {return tt.ptt(57);}
"vdbe_debug" {return tt.ptt(58);}
"vdbe_listing" {return tt.ptt(59);}
"vdbe_trace" {return tt.ptt(60);}
"wal_autocheckpoint" {return tt.ptt(61);}
"wal_checkpoint" {return tt.ptt(62);}
"writable_schema" {return tt.ptt(63);}
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS


{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }
