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
"bit"{ws}"varying" {return tt.dtt(3);}
"blob" {return tt.dtt(4);}
"bool" {return tt.dtt(5);}
"boolean" {return tt.dtt(6);}
"char" {return tt.dtt(7);}
"char"{ws}"varying" {return tt.dtt(8);}
"character" {return tt.dtt(9);}
"character"{ws}"varying" {return tt.dtt(10);}
"date" {return tt.dtt(11);}
"datetime" {return tt.dtt(12);}
"dec" {return tt.dtt(13);}
"decimal" {return tt.dtt(14);}
"double" {return tt.dtt(15);}
"double"{ws}"precision" {return tt.dtt(16);}
"enum" {return tt.dtt(17);}
"fixed" {return tt.dtt(18);}
"float" {return tt.dtt(19);}
"int" {return tt.dtt(20);}
"integer" {return tt.dtt(21);}
"interval" {return tt.dtt(22);}
"longblob" {return tt.dtt(23);}
"longtext" {return tt.dtt(24);}
"mediumblob" {return tt.dtt(25);}
"mediumint" {return tt.dtt(26);}
"mediumtext" {return tt.dtt(27);}
"national"{ws}"varchar" {return tt.dtt(28);}
"numeric" {return tt.dtt(29);}
"real" {return tt.dtt(30);}
"smallint" {return tt.dtt(31);}
"text" {return tt.dtt(32);}
"time" {return tt.dtt(33);}
"timestamp" {return tt.dtt(34);}
"tinyblob" {return tt.dtt(35);}
"tinyint" {return tt.dtt(36);}
"tinytext" {return tt.dtt(37);}
"varbinary" {return tt.dtt(38);}
"varchar" {return tt.dtt(39);}
"year" {return tt.dtt(40);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"accessible" {return tt.ktt(0);}
"add" {return tt.ktt(1);}
"all" {return tt.ktt(2);}
"alter" {return tt.ktt(3);}
"analyze" {return tt.ktt(4);}
"and" {return tt.ktt(5);}
"any" {return tt.ktt(6);}
"as" {return tt.ktt(7);}
"asc" {return tt.ktt(8);}
"assertion" {return tt.ktt(9);}
"asensitive" {return tt.ktt(10);}
"authorization" {return tt.ktt(11);}
"before" {return tt.ktt(12);}
"between" {return tt.ktt(13);}
"both" {return tt.ktt(14);}
"by" {return tt.ktt(15);}
"call" {return tt.ktt(16);}
"cascade" {return tt.ktt(17);}
"cascaded" {return tt.ktt(18);}
"case" {return tt.ktt(19);}
"cast" {return tt.ktt(20);}
"change" {return tt.ktt(21);}
"check" {return tt.ktt(22);}
"close" {return tt.ktt(23);}
"collate" {return tt.ktt(24);}
"column" {return tt.ktt(25);}
"columns" {return tt.ktt(26);}
"concurrent" {return tt.ktt(27);}
"condition" {return tt.ktt(28);}
"constraint" {return tt.ktt(29);}
"continue" {return tt.ktt(30);}
"convert" {return tt.ktt(31);}
"create" {return tt.ktt(32);}
"cross" {return tt.ktt(33);}
"cursor" {return tt.ktt(34);}
"data" {return tt.ktt(35);}
"database" {return tt.ktt(36);}
"databases" {return tt.ktt(37);}
"day" {return tt.ktt(38);}
"declare" {return tt.ktt(39);}
"default" {return tt.ktt(40);}
"delayed" {return tt.ktt(41);}
"delete" {return tt.ktt(42);}
"desc" {return tt.ktt(43);}
"describe" {return tt.ktt(44);}
"deterministic" {return tt.ktt(45);}
"distinct" {return tt.ktt(46);}
"distinctrow" {return tt.ktt(47);}
"div" {return tt.ktt(48);}
"do" {return tt.ktt(49);}
"domain" {return tt.ktt(50);}
"drop" {return tt.ktt(51);}
"dual" {return tt.ktt(52);}
"dumpfile" {return tt.ktt(53);}
"duplicate" {return tt.ktt(54);}
"each" {return tt.ktt(55);}
"else" {return tt.ktt(56);}
"elseif" {return tt.ktt(57);}
"enclosed" {return tt.ktt(58);}
"end" {return tt.ktt(59);}
"escape" {return tt.ktt(60);}
"escaped" {return tt.ktt(61);}
"exists" {return tt.ktt(62);}
"exit" {return tt.ktt(63);}
"expansion" {return tt.ktt(64);}
"explain" {return tt.ktt(65);}
"false" {return tt.ktt(66);}
"fetch" {return tt.ktt(67);}
"fields" {return tt.ktt(68);}
"first" {return tt.ktt(69);}
"float4" {return tt.ktt(70);}
"float8" {return tt.ktt(71);}
"for" {return tt.ktt(72);}
"force" {return tt.ktt(73);}
"foreign" {return tt.ktt(74);}
"from" {return tt.ktt(75);}
"full" {return tt.ktt(76);}
"fulltext" {return tt.ktt(77);}
"grant" {return tt.ktt(78);}
"group" {return tt.ktt(79);}
"handler" {return tt.ktt(80);}
"having" {return tt.ktt(81);}
"high_priority" {return tt.ktt(82);}
"hour" {return tt.ktt(83);}
"if" {return tt.ktt(84);}
"ignore" {return tt.ktt(85);}
"in" {return tt.ktt(86);}
"index" {return tt.ktt(87);}
"indicator" {return tt.ktt(88);}
"infile" {return tt.ktt(89);}
"inner" {return tt.ktt(90);}
"inout" {return tt.ktt(91);}
"insensitive" {return tt.ktt(92);}
"insert" {return tt.ktt(93);}
"int1" {return tt.ktt(94);}
"int2" {return tt.ktt(95);}
"int3" {return tt.ktt(96);}
"int4" {return tt.ktt(97);}
"int8" {return tt.ktt(98);}
"into" {return tt.ktt(99);}
"is" {return tt.ktt(100);}
"iterate" {return tt.ktt(101);}
"join" {return tt.ktt(102);}
"key" {return tt.ktt(103);}
"keys" {return tt.ktt(104);}
"kill" {return tt.ktt(105);}
"language" {return tt.ktt(106);}
"last" {return tt.ktt(107);}
"leading" {return tt.ktt(108);}
"leave" {return tt.ktt(109);}
"left" {return tt.ktt(110);}
"level" {return tt.ktt(111);}
"like" {return tt.ktt(112);}
"limit" {return tt.ktt(113);}
"linear" {return tt.ktt(114);}
"lines" {return tt.ktt(115);}
"load" {return tt.ktt(116);}
"local" {return tt.ktt(117);}
"lock" {return tt.ktt(118);}
"long" {return tt.ktt(119);}
"loop" {return tt.ktt(120);}
"low_ignore" {return tt.ktt(121);}
"low_priority" {return tt.ktt(122);}
"master_ssl_verify_server_cert" {return tt.ktt(123);}
"match" {return tt.ktt(124);}
"microsecond" {return tt.ktt(125);}
"middleint" {return tt.ktt(126);}
"minute" {return tt.ktt(127);}
"mod" {return tt.ktt(128);}
"mode" {return tt.ktt(129);}
"modifies" {return tt.ktt(130);}
"month" {return tt.ktt(131);}
"national" {return tt.ktt(132);}
"natural" {return tt.ktt(133);}
"next" {return tt.ktt(134);}
"not" {return tt.ktt(135);}
"no_write_to_binlog" {return tt.ktt(136);}
"null" {return tt.ktt(137);}
"offset" {return tt.ktt(138);}
"oj" {return tt.ktt(139);}
"on" {return tt.ktt(140);}
"open" {return tt.ktt(141);}
"optimize" {return tt.ktt(142);}
"option" {return tt.ktt(143);}
"optionally" {return tt.ktt(144);}
"or" {return tt.ktt(145);}
"order" {return tt.ktt(146);}
"out" {return tt.ktt(147);}
"outer" {return tt.ktt(148);}
"outfile" {return tt.ktt(149);}
"overlaps" {return tt.ktt(150);}
"partial" {return tt.ktt(151);}
"precision" {return tt.ktt(152);}
"prev" {return tt.ktt(153);}
"primary" {return tt.ktt(154);}
"procedure" {return tt.ktt(155);}
"purge" {return tt.ktt(156);}
"query" {return tt.ktt(157);}
"quick" {return tt.ktt(158);}
"range" {return tt.ktt(159);}
"read" {return tt.ktt(160);}
"reads" {return tt.ktt(161);}
"read_only" {return tt.ktt(162);}
"read_write" {return tt.ktt(163);}
"references" {return tt.ktt(164);}
"regexp" {return tt.ktt(165);}
"release" {return tt.ktt(166);}
"rename" {return tt.ktt(167);}
"repeat" {return tt.ktt(168);}
"replace" {return tt.ktt(169);}
"require" {return tt.ktt(170);}
"restrict" {return tt.ktt(171);}
"return" {return tt.ktt(172);}
"reverse" {return tt.ktt(173);}
"revoke" {return tt.ktt(174);}
"right" {return tt.ktt(175);}
"rlike" {return tt.ktt(176);}
"rollup" {return tt.ktt(177);}
"schema" {return tt.ktt(178);}
"schemas" {return tt.ktt(179);}
"second" {return tt.ktt(180);}
"select" {return tt.ktt(181);}
"sensitive" {return tt.ktt(182);}
"separator" {return tt.ktt(183);}
"set" {return tt.ktt(184);}
"share" {return tt.ktt(185);}
"show" {return tt.ktt(186);}
"simple" {return tt.ktt(187);}
"some" {return tt.ktt(188);}
"spatial" {return tt.ktt(189);}
"specific" {return tt.ktt(190);}
"sql" {return tt.ktt(191);}
"sqlexception" {return tt.ktt(192);}
"sqlstate" {return tt.ktt(193);}
"sqlwarning" {return tt.ktt(194);}
"sql_big_result" {return tt.ktt(195);}
"sql_buffer_result" {return tt.ktt(196);}
"sql_cache" {return tt.ktt(197);}
"sql_calc_found_rows" {return tt.ktt(198);}
"sql_no_cache" {return tt.ktt(199);}
"sql_small_result" {return tt.ktt(200);}
"ssl" {return tt.ktt(201);}
"starting" {return tt.ktt(202);}
"straight_join" {return tt.ktt(203);}
"table" {return tt.ktt(204);}
"terminated" {return tt.ktt(205);}
"then" {return tt.ktt(206);}
"to" {return tt.ktt(207);}
"trailing" {return tt.ktt(208);}
"trigger" {return tt.ktt(209);}
"true" {return tt.ktt(210);}
"truncate" {return tt.ktt(211);}
"undo" {return tt.ktt(212);}
"union" {return tt.ktt(213);}
"unique" {return tt.ktt(214);}
"unknown" {return tt.ktt(215);}
"unlock" {return tt.ktt(216);}
"unsigned" {return tt.ktt(217);}
"update" {return tt.ktt(218);}
"usage" {return tt.ktt(219);}
"use" {return tt.ktt(220);}
"using" {return tt.ktt(221);}
"value" {return tt.ktt(222);}
"values" {return tt.ktt(223);}
"varcharacter" {return tt.ktt(224);}
"varying" {return tt.ktt(225);}
"view" {return tt.ktt(226);}
"when" {return tt.ktt(227);}
"where" {return tt.ktt(228);}
"while" {return tt.ktt(229);}
"with" {return tt.ktt(230);}
"without" {return tt.ktt(231);}
"write" {return tt.ktt(232);}
"xor" {return tt.ktt(233);}
"zerofill" {return tt.ktt(234);}
"zone" {return tt.ktt(235);}
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
"ceil" {return tt.ftt(18);}
"ceiling" {return tt.ftt(19);}
"character_length" {return tt.ftt(20);}
"charset" {return tt.ftt(21);}
"char_length" {return tt.ftt(22);}
"coercibility" {return tt.ftt(23);}
"collation" {return tt.ftt(24);}
"compress" {return tt.ftt(25);}
"concat" {return tt.ftt(26);}
"concat_ws" {return tt.ftt(27);}
"connection_id" {return tt.ftt(28);}
"conv" {return tt.ftt(29);}
"convert_tz" {return tt.ftt(30);}
"cos" {return tt.ftt(31);}
"cot" {return tt.ftt(32);}
"count" {return tt.ftt(33);}
"crc32" {return tt.ftt(34);}
"curdate" {return tt.ftt(35);}
"current_date" {return tt.ftt(36);}
"current_time" {return tt.ftt(37);}
"current_timestamp" {return tt.ftt(38);}
"current_user" {return tt.ftt(39);}
"curtime" {return tt.ftt(40);}
"datediff" {return tt.ftt(41);}
"date_add" {return tt.ftt(42);}
"date_format" {return tt.ftt(43);}
"date_sub" {return tt.ftt(44);}
"dayname" {return tt.ftt(45);}
"dayofmonth" {return tt.ftt(46);}
"dayofweek" {return tt.ftt(47);}
"dayofyear" {return tt.ftt(48);}
"day_hour" {return tt.ftt(49);}
"day_microsecond" {return tt.ftt(50);}
"day_minute" {return tt.ftt(51);}
"day_second" {return tt.ftt(52);}
"decode" {return tt.ftt(53);}
"degrees" {return tt.ftt(54);}
"des_decrypt" {return tt.ftt(55);}
"des_encrypt" {return tt.ftt(56);}
"elt" {return tt.ftt(57);}
"encode" {return tt.ftt(58);}
"encrypt" {return tt.ftt(59);}
"exp" {return tt.ftt(60);}
"export_set" {return tt.ftt(61);}
"extract" {return tt.ftt(62);}
"field" {return tt.ftt(63);}
"find_in_set" {return tt.ftt(64);}
"floor" {return tt.ftt(65);}
"fn_second_microsecond" {return tt.ftt(66);}
"format" {return tt.ftt(67);}
"found_rows" {return tt.ftt(68);}
"from_days" {return tt.ftt(69);}
"from_unixtime" {return tt.ftt(70);}
"get_format" {return tt.ftt(71);}
"get_lock" {return tt.ftt(72);}
"group_concat" {return tt.ftt(73);}
"hex" {return tt.ftt(74);}
"hour_microsecond" {return tt.ftt(75);}
"hour_minute" {return tt.ftt(76);}
"hour_second" {return tt.ftt(77);}
"ifnull" {return tt.ftt(78);}
"inet_aton" {return tt.ftt(79);}
"inet_ntoa" {return tt.ftt(80);}
"instr" {return tt.ftt(81);}
"is_free_lock" {return tt.ftt(82);}
"is_used_lock" {return tt.ftt(83);}
"last_day" {return tt.ftt(84);}
"last_insert_id" {return tt.ftt(85);}
"lcase" {return tt.ftt(86);}
"length" {return tt.ftt(87);}
"ln" {return tt.ftt(88);}
"load_file" {return tt.ftt(89);}
"localtime" {return tt.ftt(90);}
"localtimestamp" {return tt.ftt(91);}
"locate" {return tt.ftt(92);}
"log" {return tt.ftt(93);}
"log10" {return tt.ftt(94);}
"log2" {return tt.ftt(95);}
"lower" {return tt.ftt(96);}
"lpad" {return tt.ftt(97);}
"ltrim" {return tt.ftt(98);}
"makedate" {return tt.ftt(99);}
"maketime" {return tt.ftt(100);}
"make_set" {return tt.ftt(101);}
"master_pos_wait" {return tt.ftt(102);}
"max" {return tt.ftt(103);}
"md5" {return tt.ftt(104);}
"mid" {return tt.ftt(105);}
"min" {return tt.ftt(106);}
"minute_microsecond" {return tt.ftt(107);}
"minute_second" {return tt.ftt(108);}
"monthname" {return tt.ftt(109);}
"name_const" {return tt.ftt(110);}
"not_like" {return tt.ftt(111);}
"not_regexp" {return tt.ftt(112);}
"now" {return tt.ftt(113);}
"nullif" {return tt.ftt(114);}
"oct" {return tt.ftt(115);}
"octet_length" {return tt.ftt(116);}
"old_password" {return tt.ftt(117);}
"ord" {return tt.ftt(118);}
"password" {return tt.ftt(119);}
"period_add" {return tt.ftt(120);}
"period_diff" {return tt.ftt(121);}
"pi" {return tt.ftt(122);}
"position" {return tt.ftt(123);}
"pow" {return tt.ftt(124);}
"power" {return tt.ftt(125);}
"quarter" {return tt.ftt(126);}
"quote" {return tt.ftt(127);}
"radians" {return tt.ftt(128);}
"rand" {return tt.ftt(129);}
"release_lock" {return tt.ftt(130);}
"round" {return tt.ftt(131);}
"row_count" {return tt.ftt(132);}
"rpad" {return tt.ftt(133);}
"rtrim" {return tt.ftt(134);}
"second_microsecond" {return tt.ftt(135);}
"sec_to_time" {return tt.ftt(136);}
"session_user" {return tt.ftt(137);}
"sha" {return tt.ftt(138);}
"sha1" {return tt.ftt(139);}
"sha2" {return tt.ftt(140);}
"sign" {return tt.ftt(141);}
"sin" {return tt.ftt(142);}
"sleep" {return tt.ftt(143);}
"soundex" {return tt.ftt(144);}
"sounds_like" {return tt.ftt(145);}
"space" {return tt.ftt(146);}
"sqrt" {return tt.ftt(147);}
"std" {return tt.ftt(148);}
"stddev" {return tt.ftt(149);}
"stddev_pop" {return tt.ftt(150);}
"stddev_samp" {return tt.ftt(151);}
"strcmp" {return tt.ftt(152);}
"str_to_date" {return tt.ftt(153);}
"subdate" {return tt.ftt(154);}
"substr" {return tt.ftt(155);}
"substring" {return tt.ftt(156);}
"substring_index" {return tt.ftt(157);}
"subtime" {return tt.ftt(158);}
"sum" {return tt.ftt(159);}
"sysdate" {return tt.ftt(160);}
"system_user" {return tt.ftt(161);}
"tan" {return tt.ftt(162);}
"timediff" {return tt.ftt(163);}
"timestampadd" {return tt.ftt(164);}
"timestampdiff" {return tt.ftt(165);}
"time_format" {return tt.ftt(166);}
"time_to_sec" {return tt.ftt(167);}
"to_days" {return tt.ftt(168);}
"trim" {return tt.ftt(169);}
"ucase" {return tt.ftt(170);}
"uncompress" {return tt.ftt(171);}
"uncompressed_length" {return tt.ftt(172);}
"unhex" {return tt.ftt(173);}
"unix_timestamp" {return tt.ftt(174);}
"upper" {return tt.ftt(175);}
"user" {return tt.ftt(176);}
"utc_date" {return tt.ftt(177);}
"utc_time" {return tt.ftt(178);}
"utc_timestamp" {return tt.ftt(179);}
"uuid" {return tt.ftt(180);}
"uuid_short" {return tt.ftt(181);}
"variance" {return tt.ftt(182);}
"var_pop" {return tt.ftt(183);}
"var_samp" {return tt.ftt(184);}
"version" {return tt.ftt(185);}
"week" {return tt.ftt(186);}
"weekday" {return tt.ftt(187);}
"weekofyear" {return tt.ftt(188);}
"weight_string" {return tt.ftt(189);}
"yearweek" {return tt.ftt(190);}
"year_month" {return tt.ftt(191);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }

