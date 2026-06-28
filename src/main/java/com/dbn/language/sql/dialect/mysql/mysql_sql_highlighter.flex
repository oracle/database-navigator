package com.dbn.language.sql.dialect.mysql;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

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
STRING = ("n"|"_"{CHARSET})?{wso}{string_simple_quoted}

sign = "+"|"-"
digit = [0-9]
INTEGER = {digit}+("e"{sign}?{digit}+)?
NUMBER = {INTEGER}?"."{digit}+(("e"{sign}?{digit}+)|(("f"|"d"){ws}))?

VARIABLE = ":"{wso}({IDENTIFIER}|{INTEGER})

operator_equals             = "="
operator_not_equals         = (("!"|"^"){wso}"=")|("<"{wso}">")
operator_greater_than       = ">"
operator_greater_equal_than = ">"{wso}"="
operator_less_than          = "<"
operator_less_equal_than    = ">"{wso}"="
OPERATOR                    = {operator_equals}|{operator_not_equals}|{operator_greater_than}|{operator_greater_equal_than}|{operator_less_than}|{operator_less_equal_than}


SQL_KEYWORD = "accessible"|"account"|"action"|"add"|"algorithm"|"all"|"alter"|"always"|"analyze"|"and"|"as"|"asc"|"asensitive"|"attribute"|"authentication"|"before"|"between"|"both"|"btree"|"by"|"call"|"cascade"|"cascaded"|"case"|"change"|"character"|"charset"|"check"|"checksum"|"cipher"|"close"|"collate"|"column"|"columns"|"comment"|"compact"|"compressed"|"compression"|"concurrent"|"condition"|"connection"|"constraint"|"continue"|"convert"|"copy"|"create"|"cross"|"current"|"current_user"|"cursor"|"data"|"database"|"databases"|"declare"|"default"|"definer"|"delayed"|"delete"|"desc"|"describe"|"deterministic"|"directory"|"disk"|"distinct"|"distinctrow"|"div"|"do"|"drop"|"dual"|"dumpfile"|"duplicate"|"dynamic"|"each"|"else"|"elseif"|"enclosed"|"encryption"|"end"|"enforced"|"engine"|"escaped"|"exists"|"exit"|"exclusive"|"expire"|"expansion"|"explain"|"false"|"fetch"|"fields"|"first"|"fixed"|"float4"|"float8"|"for"|"force"|"foreign"|"from"|"full"|"fulltext"|"generated"|"grant"|"group"|"handler"|"hash"|"having"|"high_priority"|"history"|"if"|"ignore"|"identified"|"in"|"index"|"infile"|"initial"|"inner"|"inplace"|"inout"|"insensitive"|"insert"|"int1"|"int2"|"int3"|"int4"|"int8"|"interval"|"into"|"invisible"|"invoker"|"is"|"issuer"|"iterate"|"join"|"key"|"keys"|"kill"|"language"|"last"|"leading"|"leave"|"left"|"less"|"level"|"like"|"limit"|"linear"|"lines"|"list"|"load"|"local"|"lock"|"long"|"loop"|"match"|"maxvalue"|"memory"|"merge"|"microsecond"|"middleint"|"mod"|"mode"|"modifies"|"national"|"natural"|"never"|"next"|"no"|"none"|"not"|"null"|"offset"|"oj"|"on"|"open"|"optimize"|"option"|"optional"|"optionally"|"or"|"order"|"out"|"outer"|"outfile"|"parser"|"partial"|"partition"|"partitions"|"password"|"precision"|"prev"|"primary"|"procedure"|"purge"|"query"|"quick"|"random"|"range"|"read"|"reads"|"read_only"|"read_write"|"recursive"|"redundant"|"references"|"regexp"|"release"|"rename"|"repeat"|"replace"|"require"|"restrict"|"return"|"reverse"|"reuse"|"revoke"|"right"|"rlike"|"rollback"|"rollup"|"role"|"row"|"schema"|"schemas"|"security"|"select"|"sensitive"|"separator"|"set"|"share"|"shared"|"show"|"simple"|"spatial"|"specific"|"sql"|"sqlexception"|"sqlstate"|"sqlwarning"|"sql_big_result"|"sql_buffer_result"|"sql_cache"|"sql_calc_found_rows"|"sql_no_cache"|"sql_small_result"|"ssl"|"start"|"starting"|"storage"|"stored"|"straight_join"|"subject"|"subpartition"|"table"|"tablespace"|"temporary"|"temptable"|"terminated"|"than"|"then"|"to"|"trailing"|"transaction"|"trigger"|"true"|"truncate"|"undefined"|"undo"|"union"|"unique"|"unlock"|"unbounded"|"unsigned"|"update"|"usage"|"use"|"using"|"value"|"values"|"varcharacter"|"varying"|"view"|"virtual"|"visible"|"when"|"where"|"while"|"with"|"write"|"x509"|"xor"|"zerofill"
SQL_FUNCTION = "abs"|"acos"|"adddate"|"addtime"|"aes_decrypt"|"aes_encrypt"|"against"|"ascii"|"asin"|"atan"|"atan2"|"avg"|"benchmark"|"bin"|"bit_and"|"bit_length"|"bit_or"|"bit_xor"|"ceil"|"ceiling"|"character_length"|"char_length"|"coercibility"|"collation"|"compress"|"concat"|"concat_ws"|"connection_id"|"conv"|"convert_tz"|"cos"|"cot"|"count"|"crc32"|"curdate"|"current_date"|"current_time"|"current_timestamp"|"curtime"|"datediff"|"date_add"|"date_format"|"date_sub"|"day"|"dayname"|"dayofmonth"|"dayofweek"|"dayofyear"|"day_hour"|"day_microsecond"|"day_minute"|"day_second"|"decode"|"degrees"|"des_decrypt"|"des_encrypt"|"elt"|"encode"|"encrypt"|"exp"|"export_set"|"extract"|"field"|"find_in_set"|"floor"|"fn_second_microsecond"|"format"|"found_rows"|"from_days"|"from_unixtime"|"get_format"|"get_lock"|"group_concat"|"hex"|"hour"|"hour_microsecond"|"hour_minute"|"hour_second"|"ifnull"|"inet_aton"|"inet_ntoa"|"instr"|"is_free_lock"|"is_used_lock"|"last_day"|"last_insert_id"|"lcase"|"length"|"ln"|"load_file"|"localtime"|"localtimestamp"|"locate"|"log"|"log10"|"log2"|"lower"|"lpad"|"ltrim"|"makedate"|"maketime"|"make_set"|"master_pos_wait"|"max"|"md5"|"mid"|"min"|"minute"|"minute_microsecond"|"minute_second"|"month"|"monthname"|"name_const"|"not_like"|"not_regexp"|"now"|"nullif"|"oct"|"octet_length"|"old_password"|"ord"|"period_add"|"period_diff"|"pi"|"position"|"pow"|"power"|"quarter"|"quote"|"radians"|"rand"|"release_lock"|"round"|"row_count"|"rpad"|"rtrim"|"second"|"second_microsecond"|"sec_to_time"|"session_user"|"sha"|"sha1"|"sha2"|"sign"|"sin"|"sleep"|"soundex"|"sounds_like"|"space"|"sqrt"|"std"|"stddev"|"stddev_pop"|"stddev_samp"|"strcmp"|"str_to_date"|"subdate"|"substr"|"substring"|"substring_index"|"subtime"|"sum"|"sysdate"|"system_user"|"tan"|"timediff"|"timestampadd"|"timestampdiff"|"time_format"|"time_to_sec"|"to_days"|"trim"|"ucase"|"uncompress"|"uncompressed_length"|"unhex"|"unix_timestamp"|"upper"|"user"|"utc_date"|"utc_time"|"utc_timestamp"|"uuid"|"uuid_short"|"variance"|"var_pop"|"var_samp"|"version"|"week"|"weekday"|"weekofyear"|"weight_string"|"yearweek"|"year_month"
SQL_PARAMETER = "autoextend_size"|"auto_increment"|"avg_row_length"|"column_format"|"delay_key_write"|"engine_attribute"|"failed_login_attempts"|"insert_method"|"key_block_size"|"low_ignore"|"low_priority"|"master_ssl_verify_server_cert"|"max_connections_per_hour"|"max_rows"|"max_queries_per_hour"|"max_updates_per_hour"|"max_user_connections"|"min_rows"|"no_write_to_binlog"|"pack_keys"|"password_lock_time"|"row_format"|"secondary_engine_attribute"|"stats_auto_recalc"|"stats_persistent"|"stats_sample_pages"
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

.                    { return stt.identifier; }
