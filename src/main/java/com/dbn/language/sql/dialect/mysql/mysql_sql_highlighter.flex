package com.dbn.language.sql.dialect.mysql;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class MysqlSQLHighlighterFlexLexer
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
    public MysqlSQLHighlighterFlexLexer(TokenTypeBundle tt) {
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
LINE_COMMENT = ("--"|"#") {input_character}*

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = "`"[^\`]*"`"?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"tis620"|"ucs2"|"ujis"|"utf8"|"utf8mb3"|"utf8mb4"|"utf16"|"utf16le"|"utf32"

string_simple_quoted      = "'"([^\']|"''"|{WHITE_SPACE})*"'"?
string_double_quoted      = \"([^\"]|"\"\""|{WHITE_SPACE})*\"?
string_dollar_quoted      = "$$"([^$]|"$"[^$])*"$$"?
STRING = ("n"|"_"{CHARSET})?{wso}({string_simple_quoted}|{string_double_quoted}|{string_dollar_quoted})

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

BIND_VARIABLE = ":"({IDENTIFIER}|{INTEGER})
USER_VARIABLE = "@"({IDENTIFIER}|{INTEGER})
SYSTEM_VARIABLE = "@@"({IDENTIFIER}|{INTEGER})
VARIABLE = {BIND_VARIABLE}|{SYSTEM_VARIABLE}|{USER_VARIABLE}

operator_equals             = "="
operator_not_equals         = (("!"|"^"){wso}"=")|("<"{wso}">")
operator_greater_than       = ">"
operator_greater_equal_than = ">"{wso}"="
operator_less_than          = "<"
operator_less_equal_than    = ">"{wso}"="
OPERATOR                    = {operator_equals}|{operator_not_equals}|{operator_greater_than}|{operator_greater_equal_than}|{operator_less_than}|{operator_less_equal_than}


SQL_KEYWORD = "accessible"|"account"|"action"|"active"|"admin"|"add"|"after"|"algorithm"|"all"|"alter"|"always"|"analyze"|"and"|"any"|"as"|"asc"|"asensitive"|"at"|"attribute"|"authentication"|"backup"|"before"|"begin"|"between"|"binlog"|"both"|"btree"|"by"|"call"|"cascade"|"cascaded"|"cast"|"case"|"change"|"filter"|"channel"|"character"|"charset"|"check"|"checksum"|"chain"|"cipher"|"close"|"collate"|"column"|"columns"|"comment"|"commit"|"committed"|"compile"|"compact"|"completion"|"compressed"|"compression"|"concurrent"|"condition"|"connection"|"consistent"|"constraint"|"contains"|"continue"|"convert"|"copy"|"create"|"cross"|"current"|"current_user"|"cursor"|"data"|"datafile"|"database"|"databases"|"declare"|"default"|"definer"|"definition"|"delayed"|"delete"|"desc"|"describe"|"description"|"deterministic"|"directory"|"disable"|"disk"|"distinct"|"distinctrow"|"div"|"do"|"drop"|"dual"|"duality"|"discard"|"dumpfile"|"duplicate"|"dynamic"|"each"|"else"|"elseif"|"enclosed"|"encryption"|"end"|"enable"|"enforced"|"engine"|"ends"|"error"|"escape"|"escaped"|"event"|"every"|"except"|"exchange"|"execute"|"export"|"exists"|"exit"|"exclusive"|"expire"|"expansion"|"explain"|"factor"|"false"|"fetch"|"fields"|"finish"|"first"|"fixed"|"float4"|"float8"|"flush"|"following"|"follows"|"for"|"force"|"foreign"|"from"|"full"|"fulltext"|"function"|"generated"|"general"|"generate"|"gtids"|"grant"|"global"|"group"|"group_replication"|"handler"|"help"|"hash"|"having"|"high_priority"|"history"|"if"|"ignore"|"import"|"identified"|"in"|"index"|"infile"|"inactive"|"initial"|"initiate"|"inner"|"inplace"|"instant"|"inout"|"io_thread"|"innodb"|"insensitive"|"insert"|"intersect"|"int1"|"int2"|"int3"|"int4"|"int8"|"interval"|"into"|"invisible"|"instance"|"invoker"|"is"|"isolation"|"issuer"|"iterate"|"javascript"|"join"|"key"|"keyring"|"keys"|"kill"|"language"|"last"|"leading"|"leave"|"left"|"less"|"level"|"library"|"like"|"limit"|"linear"|"lines"|"list"|"load"|"local"|"logfile"|"logs"|"locked"|"lock"|"long"|"loop"|"master"|"masking"|"match"|"materialized"|"maxvalue"|"member"|"memory"|"merge"|"microsecond"|"middleint"|"migrate"|"mod"|"mode"|"modify"|"modifies"|"national"|"natural"|"name"|"never"|"new"|"next"|"no"|"none"|"not"|"nowait"|"null"|"offset"|"of"|"oj"|"old"|"on"|"open"|"optimize"|"option"|"options"|"optional"|"optionally"|"only"|"one"|"organization"|"or"|"order"|"out"|"outer"|"outfile"|"off"|"owner"|"over"|"parser"|"partial"|"partition"|"partitioning"|"partitions"|"password"|"phase"|"policy"|"precision"|"preceding"|"precedes"|"preserve"|"prev"|"prepare"|"primary"|"privileges"|"procedure"|"proxy"|"purge"|"query"|"quick"|"random"|"range"|"read"|"reads"|"read_only"|"read_write"|"rebuild"|"recover"|"recursive"|"redundant"|"redo_log"|"reference"|"references"|"regexp"|"relational"|"release"|"reload"|"rename"|"repeat"|"repeatable"|"replace"|"registration"|"require"|"reset"|"resume"|"restrict"|"retain"|"return"|"returns"|"reverse"|"replica"|"replication"|"resource"|"remove"|"reorganize"|"repair"|"reuse"|"revoke"|"right"|"rotate"|"rlike"|"rollback"|"rollup"|"role"|"row"|"rows"|"savepoint"|"schedule"|"schema"|"schemas"|"security"|"select"|"sensitive"|"separator"|"serializable"|"server"|"set"|"session"|"share"|"shared"|"show"|"signed"|"simple"|"skip"|"slave"|"snapshot"|"sounds"|"source"|"spatial"|"specific"|"sql"|"sqlexception"|"sqlstate"|"sqlwarning"|"sql_big_result"|"sql_buffer_result"|"sql_cache"|"sql_calc_found_rows"|"sql_no_cache"|"sql_small_result"|"sql_thread"|"ssl"|"start"|"starting"|"starts"|"stop"|"storage"|"stored"|"straight_join"|"stream"|"subject"|"subpartition"|"subpartitions"|"suspend"|"system"|"type"|"until"|"table"|"tables"|"tablespace"|"temporary"|"temptable"|"terminated"|"traditional"|"tree"|"than"|"then"|"tls"|"to"|"trailing"|"transaction"|"trigger"|"true"|"truncate"|"undefined"|"undo"|"undofile"|"union"|"unique"|"unknown"|"unlock"|"unbounded"|"uncommitted"|"unregister"|"unsigned"|"update"|"upgrade"|"usage"|"use"|"using"|"validation"|"value"|"values"|"varcharacter"|"varying"|"view"|"virtual"|"visible"|"wait"|"wasm"|"when"|"where"|"while"|"window"|"with"|"without"|"work"|"wrapper"|"write"|"x509"|"xml"|"xa"|"xid"|"xor"|"zerofill"
SQL_FUNCTION = "abs"|"acos"|"adddate"|"addtime"|"aes_decrypt"|"aes_encrypt"|"against"|"ascii"|"asin"|"atan"|"atan2"|"avg"|"benchmark"|"bin"|"bit_and"|"bit_length"|"bit_or"|"bit_xor"|"ceil"|"ceiling"|"character_length"|"char_length"|"coalesce"|"coercibility"|"collation"|"compress"|"concat"|"concat_ws"|"connection_id"|"conv"|"convert_tz"|"cos"|"cot"|"count"|"crc32"|"curdate"|"current_date"|"current_time"|"current_timestamp"|"curtime"|"datediff"|"date_add"|"date_format"|"date_sub"|"day"|"dayname"|"dayofmonth"|"dayofweek"|"dayofyear"|"day_hour"|"day_microsecond"|"day_minute"|"day_second"|"decode"|"degrees"|"des_decrypt"|"des_encrypt"|"elt"|"encode"|"encrypt"|"exp"|"export_set"|"extract"|"field"|"find_in_set"|"floor"|"fn_second_microsecond"|"format"|"found_rows"|"from_days"|"from_unixtime"|"get_format"|"get_lock"|"group_concat"|"hex"|"hour"|"hour_microsecond"|"hour_minute"|"hour_second"|"ifnull"|"inet_aton"|"inet_ntoa"|"instr"|"is_free_lock"|"is_used_lock"|"json_duality_object"|"last_day"|"last_insert_id"|"lcase"|"length"|"ln"|"load_file"|"localtime"|"localtimestamp"|"locate"|"log"|"log10"|"log2"|"lower"|"lpad"|"ltrim"|"makedate"|"maketime"|"make_set"|"master_pos_wait"|"max"|"md5"|"mid"|"min"|"minute"|"minute_microsecond"|"minute_second"|"month"|"monthname"|"name_const"|"not_like"|"not_regexp"|"now"|"nullif"|"oct"|"octet_length"|"old_password"|"ord"|"period_add"|"period_diff"|"pi"|"position"|"pow"|"power"|"quarter"|"quote"|"radians"|"rand"|"release_lock"|"round"|"row_count"|"rpad"|"rtrim"|"second"|"second_microsecond"|"sec_to_time"|"session_user"|"sha"|"sha1"|"sha2"|"sign"|"sin"|"sleep"|"soundex"|"sounds_like"|"space"|"sqrt"|"std"|"stddev"|"stddev_pop"|"stddev_samp"|"strcmp"|"str_to_date"|"subdate"|"substr"|"substring"|"substring_index"|"subtime"|"sum"|"sysdate"|"system_user"|"tan"|"timediff"|"timestampadd"|"timestampdiff"|"time_format"|"time_to_sec"|"to_days"|"trim"|"ucase"|"uncompress"|"uncompressed_length"|"unhex"|"unix_timestamp"|"upper"|"user"|"utc_date"|"utc_time"|"utc_timestamp"|"uuid"|"uuid_short"|"variance"|"var_pop"|"var_samp"|"version"|"week"|"weekday"|"weekofyear"|"weight_string"|"yearweek"|"year_month"
SQL_PARAMETER = "assign_gtids_to_anonymous_transactions"|"autoextend_size"|"auto_increment"|"avg_row_length"|"challenge_response"|"column_format"|"default_auth"|"delay_key_write"|"engine_attribute"|"extent_size"|"failed_login_attempts"|"file_block_size"|"get_source_public_key"|"gtid_only"|"ignore_server_ids"|"initial_size"|"insert_method"|"key_block_size"|"low_ignore"|"low_priority"|"master_ssl_verify_server_cert"|"max_connections_per_hour"|"max_queries_per_hour"|"max_rows"|"max_size"|"max_updates_per_hour"|"max_user_connections"|"min_rows"|"network_namespace"|"nodegroup"|"no_write_to_binlog"|"pack_keys"|"password_lock_time"|"plugin_dir"|"privilege_checks_user"|"redo_buffer_size"|"relay_log_file"|"relay_log_pos"|"replicate_do_db"|"replicate_do_table"|"replicate_ignore_db"|"replicate_ignore_table"|"replicate_rewrite_db"|"replicate_wild_do_table"|"replicate_wild_ignore_table"|"require_row_format"|"require_table_primary_key_check"|"row_format"|"secondary_engine_attribute"|"sql_after_gtids"|"sql_after_mts_gaps"|"sql_before_gtids"|"source_auto_position"|"source_bind"|"source_compression_algorithms"|"source_connect_retry"|"source_connection_auto_failover"|"source_delay"|"source_heartbeat_period"|"source_host"|"source_log_file"|"source_log_pos"|"source_password"|"source_port"|"source_public_key_path"|"source_retry_count"|"source_ssl"|"source_ssl_ca"|"source_ssl_capath"|"source_ssl_cert"|"source_ssl_cipher"|"source_ssl_crl"|"source_ssl_crlpath"|"source_ssl_key"|"source_ssl_verify_server_cert"|"source_tls_ciphersuites"|"source_tls_version"|"source_user"|"source_zstd_compression_level"|"stats_auto_recalc"|"stats_persistent"|"stats_sample_pages"|"thread_priority"|"undo_buffer_size"|"vcpu"
SQL_DATATYPE = "bigint"|"binary"|"bit"|"blob"|"bool"|"boolean"|"char"|"date"|"datetime"|"dec"|"decimal"|"double"|"enum"|"float"|"geometry"|"geometrycollection"|"int"|"integer"|"json"|"linestring"|"longblob"|"longtext"|"mediumblob"|"mediumint"|"mediumtext"|"multilinestring"|"multipoint"|"multipolygon"|"numeric"|"point"|"polygon"|"real"|"smallint"|"text"|"time"|"timestamp"|"tinyblob"|"tinyint"|"tinytext"|"varbinary"|"varchar"|"year"

%state DIV
%%

{VARIABLE}           { return tt.getTokenType("VARIABLE"); }

{WHITE_SPACE}+       { return stt.whiteSpace; }

{BLOCK_COMMENT}      { return stt.blockComment; }
{LINE_COMMENT}       { return stt.lineComment; }

{INTEGER}            { return stt.integer; }
{NUMBER}             { return stt.number; }
{STRING}             { return stt.string; }

{SQL_FUNCTION}       { return tt.getTokenType("FUNCTION");}
{SQL_PARAMETER}      { return tt.getTokenType("PARAMETER");}
{SQL_DATATYPE}       { return tt.getTokenType("DATA_TYPE"); }
{SQL_KEYWORD}        { return tt.getTokenType("KEYWORD"); }

{OPERATOR}           { return tt.getTokenType("OPERATOR"); }

{IDENTIFIER}         { return stt.identifier; }
{QUOTED_IDENTIFIER}  { return stt.identifier; }


"("                  { return stt.chrLeftParenthesis; }
")"                  { return stt.chrRightParenthesis; }
"["                  { return stt.chrLeftBracket; }
"]"                  { return stt.chrRightBracket; }
"?"                  { return tt.getCharacterTokenType(25); }

.                    { return stt.identifier; }
