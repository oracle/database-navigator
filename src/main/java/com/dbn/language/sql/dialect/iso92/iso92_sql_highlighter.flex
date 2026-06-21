package com.dbn.language.sql.dialect.iso92;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class Iso92SQLHighlighterFlexLexer
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
    public Iso92SQLHighlighterFlexLexer(TokenTypeBundle tt) {
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
LINE_COMMENT = "--" {input_character}*

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = "`"[^\`]*"`"?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"swe7"|"tis620"|"ucs2"|"ujis"|"utf8"

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


SQL_KEYWORD = "accessible"|"add"|"all"|"alter"|"analyze"|"and"|"any"|"as"|"asc"|"assertion"|"asensitive"|"authorization"|"before"|"between"|"both"|"by"|"call"|"cascade"|"cascaded"|"case"|"cast"|"change"|"check"|"close"|"collate"|"column"|"columns"|"commit"|"concurrent"|"condition"|"constraint"|"continue"|"convert"|"create"|"cross"|"cursor"|"data"|"database"|"databases"|"day"|"declare"|"default"|"delayed"|"delete"|"desc"|"describe"|"deterministic"|"distinct"|"distinctrow"|"div"|"do"|"domain"|"drop"|"dual"|"dumpfile"|"duplicate"|"each"|"else"|"elseif"|"enclosed"|"end"|"escape"|"escaped"|"exists"|"exit"|"expansion"|"explain"|"false"|"fetch"|"fields"|"first"|"float4"|"float8"|"for"|"force"|"foreign"|"from"|"full"|"fulltext"|"grant"|"group"|"handler"|"having"|"high_priority"|"hour"|"if"|"ignore"|"in"|"index"|"indicator"|"infile"|"inner"|"inout"|"insensitive"|"insert"|"int1"|"int2"|"int3"|"int4"|"int8"|"into"|"is"|"iterate"|"join"|"key"|"keys"|"kill"|"language"|"last"|"leading"|"leave"|"left"|"level"|"like"|"limit"|"linear"|"lines"|"load"|"local"|"lock"|"long"|"loop"|"low_ignore"|"low_priority"|"master_ssl_verify_server_cert"|"match"|"microsecond"|"middleint"|"minute"|"mod"|"mode"|"modifies"|"month"|"national"|"natural"|"next"|"not"|"no_write_to_binlog"|"null"|"offset"|"oj"|"on"|"open"|"optimize"|"option"|"optionally"|"or"|"order"|"out"|"outer"|"outfile"|"overlaps"|"partial"|"precision"|"prev"|"primary"|"privileges"|"procedure"|"public"|"purge"|"query"|"quick"|"range"|"read"|"reads"|"read_only"|"read_write"|"references"|"regexp"|"release"|"rename"|"repeat"|"replace"|"require"|"restrict"|"return"|"reverse"|"revoke"|"right"|"rlike"|"rollback"|"rollup"|"schema"|"schemas"|"second"|"select"|"sensitive"|"separator"|"set"|"share"|"show"|"simple"|"some"|"spatial"|"specific"|"sql"|"sqlexception"|"sqlstate"|"sqlwarning"|"sql_big_result"|"sql_buffer_result"|"sql_cache"|"sql_calc_found_rows"|"sql_no_cache"|"sql_small_result"|"ssl"|"starting"|"straight_join"|"table"|"terminated"|"then"|"to"|"trailing"|"trigger"|"true"|"truncate"|"undo"|"union"|"unique"|"unknown"|"unlock"|"unsigned"|"update"|"usage"|"use"|"using"|"value"|"values"|"varcharacter"|"varying"|"view"|"when"|"where"|"while"|"with"|"without"|"work"|"write"|"xor"|"zerofill"|"zone"
SQL_FUNCTION = "abs"|"acos"|"adddate"|"addtime"|"aes_decrypt"|"aes_encrypt"|"against"|"ascii"|"asin"|"atan"|"atan2"|"avg"|"benchmark"|"bin"|"bit_and"|"bit_length"|"bit_or"|"bit_xor"|"ceil"|"ceiling"|"character_length"|"charset"|"char_length"|"coercibility"|"collation"|"compress"|"concat"|"concat_ws"|"connection_id"|"conv"|"convert_tz"|"cos"|"cot"|"count"|"crc32"|"curdate"|"current_date"|"current_time"|"current_timestamp"|"current_user"|"curtime"|"datediff"|"date_add"|"date_format"|"date_sub"|"dayname"|"dayofmonth"|"dayofweek"|"dayofyear"|"day_hour"|"day_microsecond"|"day_minute"|"day_second"|"decode"|"degrees"|"des_decrypt"|"des_encrypt"|"elt"|"encode"|"encrypt"|"exp"|"export_set"|"extract"|"field"|"find_in_set"|"floor"|"fn_second_microsecond"|"format"|"found_rows"|"from_days"|"from_unixtime"|"get_format"|"get_lock"|"group_concat"|"hex"|"hour_microsecond"|"hour_minute"|"hour_second"|"ifnull"|"inet_aton"|"inet_ntoa"|"instr"|"is_free_lock"|"is_used_lock"|"last_day"|"last_insert_id"|"lcase"|"length"|"ln"|"load_file"|"localtime"|"localtimestamp"|"locate"|"log"|"log10"|"log2"|"lower"|"lpad"|"ltrim"|"makedate"|"maketime"|"make_set"|"master_pos_wait"|"max"|"md5"|"mid"|"min"|"minute_microsecond"|"minute_second"|"monthname"|"name_const"|"not_like"|"not_regexp"|"now"|"nullif"|"oct"|"octet_length"|"old_password"|"ord"|"password"|"period_add"|"period_diff"|"pi"|"position"|"pow"|"power"|"quarter"|"quote"|"radians"|"rand"|"release_lock"|"round"|"row_count"|"rpad"|"rtrim"|"second_microsecond"|"sec_to_time"|"session_user"|"sha"|"sha1"|"sha2"|"sign"|"sin"|"sleep"|"soundex"|"sounds_like"|"space"|"sqrt"|"std"|"stddev"|"stddev_pop"|"stddev_samp"|"strcmp"|"str_to_date"|"subdate"|"substr"|"substring"|"substring_index"|"subtime"|"sum"|"sysdate"|"system_user"|"tan"|"timediff"|"timestampadd"|"timestampdiff"|"time_format"|"time_to_sec"|"to_days"|"trim"|"ucase"|"uncompress"|"uncompressed_length"|"unhex"|"unix_timestamp"|"upper"|"user"|"utc_date"|"utc_time"|"utc_timestamp"|"uuid"|"uuid_short"|"variance"|"var_pop"|"var_samp"|"version"|"week"|"weekday"|"weekofyear"|"weight_string"|"yearweek"|"year_month"
SQL_DATATYPE = "bigint"|"binary"|"bit"|"bit"{ws}"varying"|"blob"|"bool"|"boolean"|"char"|"char"{ws}"varying"|"character"|"character"{ws}"varying"|"date"|"datetime"|"dec"|"decimal"|"double"|"double"{ws}"precision"|"enum"|"fixed"|"float"|"int"|"integer"|"interval"|"longblob"|"longtext"|"mediumblob"|"mediumint"|"mediumtext"|"national"{ws}"varchar"|"numeric"|"real"|"smallint"|"text"|"time"|"timestamp"|"tinyblob"|"tinyint"|"tinytext"|"varbinary"|"varchar"|"year"

%state DIV
%%

{VARIABLE}          { return tt.getTokenType("VARIABLE"); }

{WHITE_SPACE}+      { return stt.whiteSpace; }

{BLOCK_COMMENT}     { return stt.blockComment; }
{LINE_COMMENT}      { return stt.lineComment; }

{INTEGER}           { return stt.integer; }
{NUMBER}            { return stt.number; }
{STRING}            { return stt.string; }

{SQL_FUNCTION}      { return tt.getTokenType("FUNCTION");}
//{SQL_PARAMETER}         { return tt.getTokenType("PARAMETER");}

{SQL_DATATYPE}      { return tt.getTokenType("DATA_TYPE"); }
{SQL_KEYWORD}       { return tt.getTokenType("KEYWORD"); }
{OPERATOR}          { return tt.getTokenType("OPERATOR"); }


{IDENTIFIER}        { return stt.identifier; }
{QUOTED_IDENTIFIER} { return stt.identifier; }


"("                 { return stt.chrLeftParenthesis; }
")"                 { return stt.chrRightParenthesis; }
"["                 { return stt.chrLeftBracket; }
"]"                 { return stt.chrRightBracket; }

.                   { return stt.identifier; }
