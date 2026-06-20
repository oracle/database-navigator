package com.dbn.language.sql.dialect.iso92;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

%%

%class Iso92SQLParserFlexLexer
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
    public Iso92SQLParserFlexLexer(TokenTypeBundle tt) {
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

%state DIV
%%

{WHITE_SPACE}+     { return stt.whiteSpace; }

{BLOCK_COMMENT}    { return stt.blockComment; }
{LINE_COMMENT}     { return stt.lineComment; }

{VARIABLE}         { return stt.variable; }
{INTEGER}          { return stt.integer; }
{NUMBER}           { return stt.number; }
{STRING}           { return stt.string; }

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
"binary" {return tt.dtt(1);}
"bit" {return tt.dtt(2);}
"blob" {return tt.dtt(3);}
"bool" {return tt.dtt(4);}
"boolean" {return tt.dtt(5);}
"date" {return tt.dtt(6);}
"datetime" {return tt.dtt(7);}
"dec" {return tt.dtt(8);}
"decimal" {return tt.dtt(9);}
"double" {return tt.dtt(10);}
"double"{ws}"precision" {return tt.dtt(11);}
"enum" {return tt.dtt(12);}
"fixed" {return tt.dtt(13);}
"float" {return tt.dtt(14);}
"int" {return tt.dtt(15);}
"integer" {return tt.dtt(16);}
"longblob" {return tt.dtt(17);}
"longtext" {return tt.dtt(18);}
"mediumblob" {return tt.dtt(19);}
"mediumint" {return tt.dtt(20);}
"mediumtext" {return tt.dtt(21);}
"national"{ws}"varchar" {return tt.dtt(22);}
"numeric" {return tt.dtt(23);}
"real" {return tt.dtt(24);}
"smallint" {return tt.dtt(25);}
"text" {return tt.dtt(26);}
"time" {return tt.dtt(27);}
"timestamp" {return tt.dtt(28);}
"tinyblob" {return tt.dtt(29);}
"tinyint" {return tt.dtt(30);}
"tinytext" {return tt.dtt(31);}
"varbinary" {return tt.dtt(32);}
"varchar" {return tt.dtt(33);}
"year" {return tt.dtt(34);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"accessible" {return tt.ktt(0);}
"add" {return tt.ktt(1);}
"all" {return tt.ktt(2);}
"alter" {return tt.ktt(3);}
"analyze" {return tt.ktt(4);}
"and" {return tt.ktt(5);}
"as" {return tt.ktt(6);}
"asc" {return tt.ktt(7);}
"asensitive" {return tt.ktt(8);}
"before" {return tt.ktt(9);}
"between" {return tt.ktt(10);}
"both" {return tt.ktt(11);}
"by" {return tt.ktt(12);}
"call" {return tt.ktt(13);}
"cascade" {return tt.ktt(14);}
"case" {return tt.ktt(15);}
"change" {return tt.ktt(16);}
"character" {return tt.ktt(17);}
"check" {return tt.ktt(18);}
"close" {return tt.ktt(19);}
"collate" {return tt.ktt(20);}
"column" {return tt.ktt(21);}
"columns" {return tt.ktt(22);}
"concurrent" {return tt.ktt(23);}
"condition" {return tt.ktt(24);}
"constraint" {return tt.ktt(25);}
"continue" {return tt.ktt(26);}
"convert" {return tt.ktt(27);}
"create" {return tt.ktt(28);}
"cross" {return tt.ktt(29);}
"current_user" {return tt.ktt(30);}
"cursor" {return tt.ktt(31);}
"data" {return tt.ktt(32);}
"database" {return tt.ktt(33);}
"databases" {return tt.ktt(34);}
"declare" {return tt.ktt(35);}
"default" {return tt.ktt(36);}
"delayed" {return tt.ktt(37);}
"delete" {return tt.ktt(38);}
"desc" {return tt.ktt(39);}
"describe" {return tt.ktt(40);}
"deterministic" {return tt.ktt(41);}
"distinct" {return tt.ktt(42);}
"distinctrow" {return tt.ktt(43);}
"div" {return tt.ktt(44);}
"do" {return tt.ktt(45);}
"drop" {return tt.ktt(46);}
"dual" {return tt.ktt(47);}
"dumpfile" {return tt.ktt(48);}
"duplicate" {return tt.ktt(49);}
"each" {return tt.ktt(50);}
"else" {return tt.ktt(51);}
"elseif" {return tt.ktt(52);}
"enclosed" {return tt.ktt(53);}
"end" {return tt.ktt(54);}
"escaped" {return tt.ktt(55);}
"exists" {return tt.ktt(56);}
"exit" {return tt.ktt(57);}
"expansion" {return tt.ktt(58);}
"explain" {return tt.ktt(59);}
"false" {return tt.ktt(60);}
"fetch" {return tt.ktt(61);}
"fields" {return tt.ktt(62);}
"first" {return tt.ktt(63);}
"float4" {return tt.ktt(64);}
"float8" {return tt.ktt(65);}
"for" {return tt.ktt(66);}
"force" {return tt.ktt(67);}
"foreign" {return tt.ktt(68);}
"from" {return tt.ktt(69);}
"fulltext" {return tt.ktt(70);}
"grant" {return tt.ktt(71);}
"group" {return tt.ktt(72);}
"handler" {return tt.ktt(73);}
"having" {return tt.ktt(74);}
"high_priority" {return tt.ktt(75);}
"if" {return tt.ktt(76);}
"ignore" {return tt.ktt(77);}
"in" {return tt.ktt(78);}
"index" {return tt.ktt(79);}
"indicator" {return tt.ktt(80);}
"infile" {return tt.ktt(81);}
"inner" {return tt.ktt(82);}
"inout" {return tt.ktt(83);}
"insensitive" {return tt.ktt(84);}
"insert" {return tt.ktt(85);}
"int1" {return tt.ktt(86);}
"int2" {return tt.ktt(87);}
"int3" {return tt.ktt(88);}
"int4" {return tt.ktt(89);}
"int8" {return tt.ktt(90);}
"interval" {return tt.ktt(91);}
"into" {return tt.ktt(92);}
"is" {return tt.ktt(93);}
"iterate" {return tt.ktt(94);}
"join" {return tt.ktt(95);}
"key" {return tt.ktt(96);}
"keys" {return tt.ktt(97);}
"kill" {return tt.ktt(98);}
"language" {return tt.ktt(99);}
"last" {return tt.ktt(100);}
"leading" {return tt.ktt(101);}
"leave" {return tt.ktt(102);}
"left" {return tt.ktt(103);}
"level" {return tt.ktt(104);}
"like" {return tt.ktt(105);}
"limit" {return tt.ktt(106);}
"linear" {return tt.ktt(107);}
"lines" {return tt.ktt(108);}
"load" {return tt.ktt(109);}
"local" {return tt.ktt(110);}
"lock" {return tt.ktt(111);}
"long" {return tt.ktt(112);}
"loop" {return tt.ktt(113);}
"low_ignore" {return tt.ktt(114);}
"low_priority" {return tt.ktt(115);}
"master_ssl_verify_server_cert" {return tt.ktt(116);}
"match" {return tt.ktt(117);}
"microsecond" {return tt.ktt(118);}
"middleint" {return tt.ktt(119);}
"mod" {return tt.ktt(120);}
"mode" {return tt.ktt(121);}
"modifies" {return tt.ktt(122);}
"natural" {return tt.ktt(123);}
"next" {return tt.ktt(124);}
"not" {return tt.ktt(125);}
"no_write_to_binlog" {return tt.ktt(126);}
"null" {return tt.ktt(127);}
"offset" {return tt.ktt(128);}
"oj" {return tt.ktt(129);}
"on" {return tt.ktt(130);}
"open" {return tt.ktt(131);}
"optimize" {return tt.ktt(132);}
"option" {return tt.ktt(133);}
"optionally" {return tt.ktt(134);}
"or" {return tt.ktt(135);}
"order" {return tt.ktt(136);}
"out" {return tt.ktt(137);}
"outer" {return tt.ktt(138);}
"outfile" {return tt.ktt(139);}
"precision" {return tt.ktt(140);}
"prev" {return tt.ktt(141);}
"primary" {return tt.ktt(142);}
"procedure" {return tt.ktt(143);}
"purge" {return tt.ktt(144);}
"query" {return tt.ktt(145);}
"quick" {return tt.ktt(146);}
"range" {return tt.ktt(147);}
"read" {return tt.ktt(148);}
"reads" {return tt.ktt(149);}
"read_only" {return tt.ktt(150);}
"read_write" {return tt.ktt(151);}
"references" {return tt.ktt(152);}
"regexp" {return tt.ktt(153);}
"release" {return tt.ktt(154);}
"rename" {return tt.ktt(155);}
"repeat" {return tt.ktt(156);}
"replace" {return tt.ktt(157);}
"require" {return tt.ktt(158);}
"restrict" {return tt.ktt(159);}
"return" {return tt.ktt(160);}
"reverse" {return tt.ktt(161);}
"revoke" {return tt.ktt(162);}
"right" {return tt.ktt(163);}
"rlike" {return tt.ktt(164);}
"rollup" {return tt.ktt(165);}
"schema" {return tt.ktt(166);}
"schemas" {return tt.ktt(167);}
"select" {return tt.ktt(168);}
"sensitive" {return tt.ktt(169);}
"separator" {return tt.ktt(170);}
"set" {return tt.ktt(171);}
"share" {return tt.ktt(172);}
"show" {return tt.ktt(173);}
"spatial" {return tt.ktt(174);}
"specific" {return tt.ktt(175);}
"sql" {return tt.ktt(176);}
"sqlexception" {return tt.ktt(177);}
"sqlstate" {return tt.ktt(178);}
"sqlwarning" {return tt.ktt(179);}
"sql_big_result" {return tt.ktt(180);}
"sql_buffer_result" {return tt.ktt(181);}
"sql_cache" {return tt.ktt(182);}
"sql_calc_found_rows" {return tt.ktt(183);}
"sql_no_cache" {return tt.ktt(184);}
"sql_small_result" {return tt.ktt(185);}
"ssl" {return tt.ktt(186);}
"starting" {return tt.ktt(187);}
"straight_join" {return tt.ktt(188);}
"table" {return tt.ktt(189);}
"terminated" {return tt.ktt(190);}
"then" {return tt.ktt(191);}
"to" {return tt.ktt(192);}
"trailing" {return tt.ktt(193);}
"trigger" {return tt.ktt(194);}
"true" {return tt.ktt(195);}
"truncate" {return tt.ktt(196);}
"undo" {return tt.ktt(197);}
"union" {return tt.ktt(198);}
"unique" {return tt.ktt(199);}
"unlock" {return tt.ktt(200);}
"unsigned" {return tt.ktt(201);}
"update" {return tt.ktt(202);}
"usage" {return tt.ktt(203);}
"use" {return tt.ktt(204);}
"using" {return tt.ktt(205);}
"value" {return tt.ktt(206);}
"values" {return tt.ktt(207);}
"varcharacter" {return tt.ktt(208);}
"varying" {return tt.ktt(209);}
"when" {return tt.ktt(210);}
"where" {return tt.ktt(211);}
"while" {return tt.ktt(212);}
"with" {return tt.ktt(213);}
"write" {return tt.ktt(214);}
"xor" {return tt.ktt(215);}
"zerofill" {return tt.ktt(216);}
// MARKER_END_KEYWORDS


// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"acos" {return tt.ftt(1);}
"adddate" {return tt.ftt(2);}
"addtime" {return tt.ftt(3);}
"aes_decrypt" {return tt.ftt(4);}
"aes_encrypt" {return tt.ftt(5);}
"against" {return tt.ftt(6);}
"ascii" {return tt.ftt(7);}
"asin" {return tt.ftt(8);}
"atan" {return tt.ftt(9);}
"atan2" {return tt.ftt(10);}
"avg" {return tt.ftt(11);}
"benchmark" {return tt.ftt(12);}
"bin" {return tt.ftt(13);}
"bit_and" {return tt.ftt(14);}
"bit_length" {return tt.ftt(15);}
"bit_or" {return tt.ftt(16);}
"bit_xor" {return tt.ftt(17);}
"boolean" {return tt.ftt(18);}
"ceil" {return tt.ftt(19);}
"ceiling" {return tt.ftt(20);}
"char" {return tt.ftt(21);}
"character_length" {return tt.ftt(22);}
"charset" {return tt.ftt(23);}
"char_length" {return tt.ftt(24);}
"coercibility" {return tt.ftt(25);}
"collation" {return tt.ftt(26);}
"compress" {return tt.ftt(27);}
"concat" {return tt.ftt(28);}
"concat_ws" {return tt.ftt(29);}
"connection_id" {return tt.ftt(30);}
"conv" {return tt.ftt(31);}
"convert_tz" {return tt.ftt(32);}
"cos" {return tt.ftt(33);}
"cot" {return tt.ftt(34);}
"count" {return tt.ftt(35);}
"crc32" {return tt.ftt(36);}
"curdate" {return tt.ftt(37);}
"current_date" {return tt.ftt(38);}
"current_time" {return tt.ftt(39);}
"current_timestamp" {return tt.ftt(40);}
"current_user" {return tt.ftt(41);}
"curtime" {return tt.ftt(42);}
"database" {return tt.ftt(43);}
"date" {return tt.ftt(44);}
"datediff" {return tt.ftt(45);}
"date_add" {return tt.ftt(46);}
"date_format" {return tt.ftt(47);}
"date_sub" {return tt.ftt(48);}
"day" {return tt.ftt(49);}
"dayname" {return tt.ftt(50);}
"dayofmonth" {return tt.ftt(51);}
"dayofweek" {return tt.ftt(52);}
"dayofyear" {return tt.ftt(53);}
"day_hour" {return tt.ftt(54);}
"day_microsecond" {return tt.ftt(55);}
"day_minute" {return tt.ftt(56);}
"day_second" {return tt.ftt(57);}
"decode" {return tt.ftt(58);}
"default" {return tt.ftt(59);}
"degrees" {return tt.ftt(60);}
"des_decrypt" {return tt.ftt(61);}
"des_encrypt" {return tt.ftt(62);}
"elt" {return tt.ftt(63);}
"encode" {return tt.ftt(64);}
"encrypt" {return tt.ftt(65);}
"exp" {return tt.ftt(66);}
"expansion" {return tt.ftt(67);}
"export_set" {return tt.ftt(68);}
"extract" {return tt.ftt(69);}
"field" {return tt.ftt(70);}
"find_in_set" {return tt.ftt(71);}
"floor" {return tt.ftt(72);}
"fn_second_microsecond" {return tt.ftt(73);}
"format" {return tt.ftt(74);}
"found_rows" {return tt.ftt(75);}
"from_days" {return tt.ftt(76);}
"from_unixtime" {return tt.ftt(77);}
"get_format" {return tt.ftt(78);}
"get_lock" {return tt.ftt(79);}
"group_concat" {return tt.ftt(80);}
"hex" {return tt.ftt(81);}
"hour" {return tt.ftt(82);}
"hour_microsecond" {return tt.ftt(83);}
"hour_minute" {return tt.ftt(84);}
"hour_second" {return tt.ftt(85);}
"if" {return tt.ftt(86);}
"ifnull" {return tt.ftt(87);}
"inet_aton" {return tt.ftt(88);}
"inet_ntoa" {return tt.ftt(89);}
"insert" {return tt.ftt(90);}
"instr" {return tt.ftt(91);}
"is_free_lock" {return tt.ftt(92);}
"is_used_lock" {return tt.ftt(93);}
"language" {return tt.ftt(94);}
"last_day" {return tt.ftt(95);}
"last_insert_id" {return tt.ftt(96);}
"lcase" {return tt.ftt(97);}
"left" {return tt.ftt(98);}
"length" {return tt.ftt(99);}
"like" {return tt.ftt(100);}
"ln" {return tt.ftt(101);}
"load_file" {return tt.ftt(102);}
"localtime" {return tt.ftt(103);}
"localtimestamp" {return tt.ftt(104);}
"locate" {return tt.ftt(105);}
"log" {return tt.ftt(106);}
"log10" {return tt.ftt(107);}
"log2" {return tt.ftt(108);}
"lower" {return tt.ftt(109);}
"lpad" {return tt.ftt(110);}
"ltrim" {return tt.ftt(111);}
"makedate" {return tt.ftt(112);}
"maketime" {return tt.ftt(113);}
"make_set" {return tt.ftt(114);}
"master_pos_wait" {return tt.ftt(115);}
"match" {return tt.ftt(116);}
"max" {return tt.ftt(117);}
"md5" {return tt.ftt(118);}
"microsecond" {return tt.ftt(119);}
"mid" {return tt.ftt(120);}
"min" {return tt.ftt(121);}
"minute" {return tt.ftt(122);}
"minute_microsecond" {return tt.ftt(123);}
"minute_second" {return tt.ftt(124);}
"mod" {return tt.ftt(125);}
"month" {return tt.ftt(126);}
"monthname" {return tt.ftt(127);}
"name_const" {return tt.ftt(128);}
"not_like" {return tt.ftt(129);}
"not_regexp" {return tt.ftt(130);}
"now" {return tt.ftt(131);}
"nullif" {return tt.ftt(132);}
"oct" {return tt.ftt(133);}
"octet_length" {return tt.ftt(134);}
"old_password" {return tt.ftt(135);}
"ord" {return tt.ftt(136);}
"password" {return tt.ftt(137);}
"period_add" {return tt.ftt(138);}
"period_diff" {return tt.ftt(139);}
"pi" {return tt.ftt(140);}
"position" {return tt.ftt(141);}
"pow" {return tt.ftt(142);}
"power" {return tt.ftt(143);}
"quarter" {return tt.ftt(144);}
"query" {return tt.ftt(145);}
"quote" {return tt.ftt(146);}
"radians" {return tt.ftt(147);}
"rand" {return tt.ftt(148);}
"regexp" {return tt.ftt(149);}
"release_lock" {return tt.ftt(150);}
"repeat" {return tt.ftt(151);}
"replace" {return tt.ftt(152);}
"reverse" {return tt.ftt(153);}
"right" {return tt.ftt(154);}
"rlike" {return tt.ftt(155);}
"round" {return tt.ftt(156);}
"row_count" {return tt.ftt(157);}
"rpad" {return tt.ftt(158);}
"rtrim" {return tt.ftt(159);}
"schema" {return tt.ftt(160);}
"second" {return tt.ftt(161);}
"second_microsecond" {return tt.ftt(162);}
"sec_to_time" {return tt.ftt(163);}
"session_user" {return tt.ftt(164);}
"sha" {return tt.ftt(165);}
"sha1" {return tt.ftt(166);}
"sha2" {return tt.ftt(167);}
"sign" {return tt.ftt(168);}
"sin" {return tt.ftt(169);}
"sleep" {return tt.ftt(170);}
"soundex" {return tt.ftt(171);}
"sounds_like" {return tt.ftt(172);}
"space" {return tt.ftt(173);}
"sqrt" {return tt.ftt(174);}
"std" {return tt.ftt(175);}
"stddev" {return tt.ftt(176);}
"stddev_pop" {return tt.ftt(177);}
"stddev_samp" {return tt.ftt(178);}
"strcmp" {return tt.ftt(179);}
"str_to_date" {return tt.ftt(180);}
"subdate" {return tt.ftt(181);}
"substr" {return tt.ftt(182);}
"substring" {return tt.ftt(183);}
"substring_index" {return tt.ftt(184);}
"subtime" {return tt.ftt(185);}
"sum" {return tt.ftt(186);}
"sysdate" {return tt.ftt(187);}
"system_user" {return tt.ftt(188);}
"tan" {return tt.ftt(189);}
"time" {return tt.ftt(190);}
"timediff" {return tt.ftt(191);}
"timestamp" {return tt.ftt(192);}
"timestampadd" {return tt.ftt(193);}
"timestampdiff" {return tt.ftt(194);}
"time_format" {return tt.ftt(195);}
"time_to_sec" {return tt.ftt(196);}
"to_days" {return tt.ftt(197);}
"trim" {return tt.ftt(198);}
"truncate" {return tt.ftt(199);}
"ucase" {return tt.ftt(200);}
"uncompress" {return tt.ftt(201);}
"uncompressed_length" {return tt.ftt(202);}
"unhex" {return tt.ftt(203);}
"unix_timestamp" {return tt.ftt(204);}
"upper" {return tt.ftt(205);}
"user" {return tt.ftt(206);}
"utc_date" {return tt.ftt(207);}
"utc_time" {return tt.ftt(208);}
"utc_timestamp" {return tt.ftt(209);}
"uuid" {return tt.ftt(210);}
"uuid_short" {return tt.ftt(211);}
"variance" {return tt.ftt(212);}
"var_pop" {return tt.ftt(213);}
"var_samp" {return tt.ftt(214);}
"version" {return tt.ftt(215);}
"week" {return tt.ftt(216);}
"weekday" {return tt.ftt(217);}
"weekofyear" {return tt.ftt(218);}
"weight_string" {return tt.ftt(219);}
"year" {return tt.ftt(220);}
"yearweek" {return tt.ftt(221);}
"year_month" {return tt.ftt(222);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }

