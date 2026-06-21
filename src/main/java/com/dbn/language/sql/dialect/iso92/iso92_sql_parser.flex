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

"="{wso}"="      { return tt.getOperatorTokenType(0); }
"|"{wso}"|"      { return tt.getOperatorTokenType(1); }
"<"{wso}"="      { return tt.getOperatorTokenType(2); }
">"{wso}"="      { return tt.getOperatorTokenType(3); }
"<"{wso}">"      { return tt.getOperatorTokenType(4); }
"!"{wso}"="      { return tt.getOperatorTokenType(5); }
":"{wso}"="      { return tt.getOperatorTokenType(6); }
"="{wso}">"      { return tt.getOperatorTokenType(7); }

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
"bit" {return tt.dtt(0);}
"char" {return tt.dtt(1);}
"character" {return tt.dtt(2);}
"date" {return tt.dtt(3);}
"dec" {return tt.dtt(4);}
"decimal" {return tt.dtt(5);}
"double" {return tt.dtt(6);}
"float" {return tt.dtt(7);}
"int" {return tt.dtt(8);}
"integer" {return tt.dtt(9);}
"interval" {return tt.dtt(10);}
"numeric" {return tt.dtt(11);}
"real" {return tt.dtt(12);}
"smallint" {return tt.dtt(13);}
"time" {return tt.dtt(14);}
"timestamp" {return tt.dtt(15);}
"varchar" {return tt.dtt(16);}
// MARKER_END_DATATYPES


// MARKER_BEGIN_KEYWORDS
"absolute" {return tt.ktt(0);}
"action" {return tt.ktt(1);}
"add" {return tt.ktt(2);}
"all" {return tt.ktt(3);}
"allocate" {return tt.ktt(4);}
"alter" {return tt.ktt(5);}
"and" {return tt.ktt(6);}
"any" {return tt.ktt(7);}
"as" {return tt.ktt(8);}
"asc" {return tt.ktt(9);}
"assertion" {return tt.ktt(10);}
"asensitive" {return tt.ktt(11);}
"authorization" {return tt.ktt(12);}
"before" {return tt.ktt(13);}
"between" {return tt.ktt(14);}
"both" {return tt.ktt(15);}
"by" {return tt.ktt(16);}
"call" {return tt.ktt(17);}
"cascade" {return tt.ktt(18);}
"cascaded" {return tt.ktt(19);}
"case" {return tt.ktt(20);}
"catalog" {return tt.ktt(21);}
"cast" {return tt.ktt(22);}
"characteristics" {return tt.ktt(23);}
"character_set_catalog" {return tt.ktt(24);}
"character_set_name" {return tt.ktt(25);}
"character_set_schema" {return tt.ktt(26);}
"check" {return tt.ktt(27);}
"close" {return tt.ktt(28);}
"collate" {return tt.ktt(29);}
"collation" {return tt.ktt(30);}
"collation_catalog" {return tt.ktt(31);}
"collation_name" {return tt.ktt(32);}
"collation_schema" {return tt.ktt(33);}
"column" {return tt.ktt(34);}
"columns" {return tt.ktt(35);}
"commit" {return tt.ktt(36);}
"committed" {return tt.ktt(37);}
"concurrent" {return tt.ktt(38);}
"connect" {return tt.ktt(39);}
"connection" {return tt.ktt(40);}
"condition" {return tt.ktt(41);}
"constraint" {return tt.ktt(42);}
"constraints" {return tt.ktt(43);}
"continue" {return tt.ktt(44);}
"convert" {return tt.ktt(45);}
"corresponding" {return tt.ktt(46);}
"create" {return tt.ktt(47);}
"cross" {return tt.ktt(48);}
"current" {return tt.ktt(49);}
"cursor" {return tt.ktt(50);}
"data" {return tt.ktt(51);}
"datetime_interval_code" {return tt.ktt(52);}
"datetime_interval_precision" {return tt.ktt(53);}
"day" {return tt.ktt(54);}
"declare" {return tt.ktt(55);}
"default" {return tt.ktt(56);}
"deferrable" {return tt.ktt(57);}
"deferred" {return tt.ktt(58);}
"deallocate" {return tt.ktt(59);}
"delete" {return tt.ktt(60);}
"desc" {return tt.ktt(61);}
"describe" {return tt.ktt(62);}
"descriptor" {return tt.ktt(63);}
"deterministic" {return tt.ktt(64);}
"diagnostics" {return tt.ktt(65);}
"disconnect" {return tt.ktt(66);}
"distinct" {return tt.ktt(67);}
"do" {return tt.ktt(68);}
"domain" {return tt.ktt(69);}
"drop" {return tt.ktt(70);}
"each" {return tt.ktt(71);}
"else" {return tt.ktt(72);}
"elseif" {return tt.ktt(73);}
"end" {return tt.ktt(74);}
"escape" {return tt.ktt(75);}
"except" {return tt.ktt(76);}
"execute" {return tt.ktt(77);}
"exists" {return tt.ktt(78);}
"exit" {return tt.ktt(79);}
"expansion" {return tt.ktt(80);}
"false" {return tt.ktt(81);}
"fetch" {return tt.ktt(82);}
"first" {return tt.ktt(83);}
"for" {return tt.ktt(84);}
"foreign" {return tt.ktt(85);}
"from" {return tt.ktt(86);}
"full" {return tt.ktt(87);}
"get" {return tt.ktt(88);}
"global" {return tt.ktt(89);}
"grant" {return tt.ktt(90);}
"group" {return tt.ktt(91);}
"handler" {return tt.ktt(92);}
"having" {return tt.ktt(93);}
"hold" {return tt.ktt(94);}
"hour" {return tt.ktt(95);}
"if" {return tt.ktt(96);}
"immediate" {return tt.ktt(97);}
"in" {return tt.ktt(98);}
"index" {return tt.ktt(99);}
"indicator" {return tt.ktt(100);}
"inner" {return tt.ktt(101);}
"inout" {return tt.ktt(102);}
"input" {return tt.ktt(103);}
"insensitive" {return tt.ktt(104);}
"insert" {return tt.ktt(105);}
"initially" {return tt.ktt(106);}
"into" {return tt.ktt(107);}
"is" {return tt.ktt(108);}
"isolation" {return tt.ktt(109);}
"intersect" {return tt.ktt(110);}
"iterate" {return tt.ktt(111);}
"join" {return tt.ktt(112);}
"key" {return tt.ktt(113);}
"language" {return tt.ktt(114);}
"last" {return tt.ktt(115);}
"leading" {return tt.ktt(116);}
"leave" {return tt.ktt(117);}
"left" {return tt.ktt(118);}
"level" {return tt.ktt(119);}
"like" {return tt.ktt(120);}
"local" {return tt.ktt(121);}
"lock" {return tt.ktt(122);}
"long" {return tt.ktt(123);}
"loop" {return tt.ktt(124);}
"match" {return tt.ktt(125);}
"microsecond" {return tt.ktt(126);}
"minute" {return tt.ktt(127);}
"mod" {return tt.ktt(128);}
"mode" {return tt.ktt(129);}
"modifies" {return tt.ktt(130);}
"month" {return tt.ktt(131);}
"name" {return tt.ktt(132);}
"names" {return tt.ktt(133);}
"national" {return tt.ktt(134);}
"natural" {return tt.ktt(135);}
"next" {return tt.ktt(136);}
"no" {return tt.ktt(137);}
"not" {return tt.ktt(138);}
"null" {return tt.ktt(139);}
"nullable" {return tt.ktt(140);}
"offset" {return tt.ktt(141);}
"of" {return tt.ktt(142);}
"oj" {return tt.ktt(143);}
"on" {return tt.ktt(144);}
"only" {return tt.ktt(145);}
"open" {return tt.ktt(146);}
"option" {return tt.ktt(147);}
"optionally" {return tt.ktt(148);}
"or" {return tt.ktt(149);}
"order" {return tt.ktt(150);}
"out" {return tt.ktt(151);}
"outer" {return tt.ktt(152);}
"output" {return tt.ktt(153);}
"overlaps" {return tt.ktt(154);}
"pad" {return tt.ktt(155);}
"partial" {return tt.ktt(156);}
"precision" {return tt.ktt(157);}
"prepare" {return tt.ktt(158);}
"preserve" {return tt.ktt(159);}
"prev" {return tt.ktt(160);}
"primary" {return tt.ktt(161);}
"prior" {return tt.ktt(162);}
"privileges" {return tt.ktt(163);}
"procedure" {return tt.ktt(164);}
"public" {return tt.ktt(165);}
"query" {return tt.ktt(166);}
"range" {return tt.ktt(167);}
"read" {return tt.ktt(168);}
"reads" {return tt.ktt(169);}
"read_only" {return tt.ktt(170);}
"read_write" {return tt.ktt(171);}
"references" {return tt.ktt(172);}
"release" {return tt.ktt(173);}
"relative" {return tt.ktt(174);}
"rename" {return tt.ktt(175);}
"repeat" {return tt.ktt(176);}
"repeatable" {return tt.ktt(177);}
"restrict" {return tt.ktt(178);}
"return" {return tt.ktt(179);}
"returned_length" {return tt.ktt(180);}
"returned_octet_length" {return tt.ktt(181);}
"revoke" {return tt.ktt(182);}
"right" {return tt.ktt(183);}
"rollback" {return tt.ktt(184);}
"rows" {return tt.ktt(185);}
"scale" {return tt.ktt(186);}
"schema" {return tt.ktt(187);}
"schemas" {return tt.ktt(188);}
"scroll" {return tt.ktt(189);}
"second" {return tt.ktt(190);}
"select" {return tt.ktt(191);}
"sensitive" {return tt.ktt(192);}
"separator" {return tt.ktt(193);}
"serializable" {return tt.ktt(194);}
"session" {return tt.ktt(195);}
"set" {return tt.ktt(196);}
"share" {return tt.ktt(197);}
"show" {return tt.ktt(198);}
"simple" {return tt.ktt(199);}
"size" {return tt.ktt(200);}
"some" {return tt.ktt(201);}
"space" {return tt.ktt(202);}
"specific" {return tt.ktt(203);}
"sql" {return tt.ktt(204);}
"sqlexception" {return tt.ktt(205);}
"sqlstate" {return tt.ktt(206);}
"sqlwarning" {return tt.ktt(207);}
"starting" {return tt.ktt(208);}
"table" {return tt.ktt(209);}
"temporary" {return tt.ktt(210);}
"then" {return tt.ktt(211);}
"time" {return tt.ktt(212);}
"timezone_hour" {return tt.ktt(213);}
"timezone_minute" {return tt.ktt(214);}
"to" {return tt.ktt(215);}
"trailing" {return tt.ktt(216);}
"transaction" {return tt.ktt(217);}
"translate" {return tt.ktt(218);}
"translation" {return tt.ktt(219);}
"trigger" {return tt.ktt(220);}
"true" {return tt.ktt(221);}
"truncate" {return tt.ktt(222);}
"type" {return tt.ktt(223);}
"undo" {return tt.ktt(224);}
"union" {return tt.ktt(225);}
"unique" {return tt.ktt(226);}
"uncommitted" {return tt.ktt(227);}
"unknown" {return tt.ktt(228);}
"unnamed" {return tt.ktt(229);}
"update" {return tt.ktt(230);}
"usage" {return tt.ktt(231);}
"using" {return tt.ktt(232);}
"value" {return tt.ktt(233);}
"values" {return tt.ktt(234);}
"varcharacter" {return tt.ktt(235);}
"varying" {return tt.ktt(236);}
"view" {return tt.ktt(237);}
"when" {return tt.ktt(238);}
"where" {return tt.ktt(239);}
"while" {return tt.ktt(240);}
"with" {return tt.ktt(241);}
"without" {return tt.ktt(242);}
"work" {return tt.ktt(243);}
"write" {return tt.ktt(244);}
"year" {return tt.ktt(245);}
"zone" {return tt.ktt(246);}
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
"compress" {return tt.ftt(24);}
"concat" {return tt.ftt(25);}
"concat_ws" {return tt.ftt(26);}
"connection_id" {return tt.ftt(27);}
"conv" {return tt.ftt(28);}
"convert_tz" {return tt.ftt(29);}
"cos" {return tt.ftt(30);}
"cot" {return tt.ftt(31);}
"count" {return tt.ftt(32);}
"crc32" {return tt.ftt(33);}
"curdate" {return tt.ftt(34);}
"current_date" {return tt.ftt(35);}
"current_time" {return tt.ftt(36);}
"current_timestamp" {return tt.ftt(37);}
"current_user" {return tt.ftt(38);}
"curtime" {return tt.ftt(39);}
"datediff" {return tt.ftt(40);}
"date_add" {return tt.ftt(41);}
"date_format" {return tt.ftt(42);}
"date_sub" {return tt.ftt(43);}
"dayname" {return tt.ftt(44);}
"dayofmonth" {return tt.ftt(45);}
"dayofweek" {return tt.ftt(46);}
"dayofyear" {return tt.ftt(47);}
"day_hour" {return tt.ftt(48);}
"day_microsecond" {return tt.ftt(49);}
"day_minute" {return tt.ftt(50);}
"day_second" {return tt.ftt(51);}
"decode" {return tt.ftt(52);}
"degrees" {return tt.ftt(53);}
"des_decrypt" {return tt.ftt(54);}
"des_encrypt" {return tt.ftt(55);}
"elt" {return tt.ftt(56);}
"encode" {return tt.ftt(57);}
"encrypt" {return tt.ftt(58);}
"exp" {return tt.ftt(59);}
"export_set" {return tt.ftt(60);}
"extract" {return tt.ftt(61);}
"field" {return tt.ftt(62);}
"find_in_set" {return tt.ftt(63);}
"floor" {return tt.ftt(64);}
"fn_second_microsecond" {return tt.ftt(65);}
"format" {return tt.ftt(66);}
"found_rows" {return tt.ftt(67);}
"from_days" {return tt.ftt(68);}
"from_unixtime" {return tt.ftt(69);}
"get_format" {return tt.ftt(70);}
"get_lock" {return tt.ftt(71);}
"group_concat" {return tt.ftt(72);}
"hex" {return tt.ftt(73);}
"hour_microsecond" {return tt.ftt(74);}
"hour_minute" {return tt.ftt(75);}
"hour_second" {return tt.ftt(76);}
"ifnull" {return tt.ftt(77);}
"inet_aton" {return tt.ftt(78);}
"inet_ntoa" {return tt.ftt(79);}
"instr" {return tt.ftt(80);}
"is_free_lock" {return tt.ftt(81);}
"is_used_lock" {return tt.ftt(82);}
"last_day" {return tt.ftt(83);}
"last_insert_id" {return tt.ftt(84);}
"lcase" {return tt.ftt(85);}
"length" {return tt.ftt(86);}
"ln" {return tt.ftt(87);}
"load_file" {return tt.ftt(88);}
"localtime" {return tt.ftt(89);}
"localtimestamp" {return tt.ftt(90);}
"locate" {return tt.ftt(91);}
"log" {return tt.ftt(92);}
"log10" {return tt.ftt(93);}
"log2" {return tt.ftt(94);}
"lower" {return tt.ftt(95);}
"lpad" {return tt.ftt(96);}
"ltrim" {return tt.ftt(97);}
"makedate" {return tt.ftt(98);}
"maketime" {return tt.ftt(99);}
"make_set" {return tt.ftt(100);}
"master_pos_wait" {return tt.ftt(101);}
"max" {return tt.ftt(102);}
"md5" {return tt.ftt(103);}
"mid" {return tt.ftt(104);}
"min" {return tt.ftt(105);}
"minute_microsecond" {return tt.ftt(106);}
"minute_second" {return tt.ftt(107);}
"monthname" {return tt.ftt(108);}
"name_const" {return tt.ftt(109);}
"not_like" {return tt.ftt(110);}
"not_regexp" {return tt.ftt(111);}
"now" {return tt.ftt(112);}
"nullif" {return tt.ftt(113);}
"oct" {return tt.ftt(114);}
"octet_length" {return tt.ftt(115);}
"old_password" {return tt.ftt(116);}
"ord" {return tt.ftt(117);}
"password" {return tt.ftt(118);}
"period_add" {return tt.ftt(119);}
"period_diff" {return tt.ftt(120);}
"pi" {return tt.ftt(121);}
"position" {return tt.ftt(122);}
"pow" {return tt.ftt(123);}
"power" {return tt.ftt(124);}
"quarter" {return tt.ftt(125);}
"quote" {return tt.ftt(126);}
"radians" {return tt.ftt(127);}
"rand" {return tt.ftt(128);}
"release_lock" {return tt.ftt(129);}
"round" {return tt.ftt(130);}
"row_count" {return tt.ftt(131);}
"rpad" {return tt.ftt(132);}
"rtrim" {return tt.ftt(133);}
"second_microsecond" {return tt.ftt(134);}
"sec_to_time" {return tt.ftt(135);}
"session_user" {return tt.ftt(136);}
"sha" {return tt.ftt(137);}
"sha1" {return tt.ftt(138);}
"sha2" {return tt.ftt(139);}
"sign" {return tt.ftt(140);}
"sin" {return tt.ftt(141);}
"sleep" {return tt.ftt(142);}
"soundex" {return tt.ftt(143);}
"sounds_like" {return tt.ftt(144);}
"sqrt" {return tt.ftt(145);}
"std" {return tt.ftt(146);}
"stddev" {return tt.ftt(147);}
"stddev_pop" {return tt.ftt(148);}
"stddev_samp" {return tt.ftt(149);}
"strcmp" {return tt.ftt(150);}
"str_to_date" {return tt.ftt(151);}
"subdate" {return tt.ftt(152);}
"substr" {return tt.ftt(153);}
"substring" {return tt.ftt(154);}
"substring_index" {return tt.ftt(155);}
"subtime" {return tt.ftt(156);}
"sum" {return tt.ftt(157);}
"sysdate" {return tt.ftt(158);}
"system_user" {return tt.ftt(159);}
"tan" {return tt.ftt(160);}
"timediff" {return tt.ftt(161);}
"timestampadd" {return tt.ftt(162);}
"timestampdiff" {return tt.ftt(163);}
"time_format" {return tt.ftt(164);}
"time_to_sec" {return tt.ftt(165);}
"to_days" {return tt.ftt(166);}
"trim" {return tt.ftt(167);}
"ucase" {return tt.ftt(168);}
"uncompress" {return tt.ftt(169);}
"uncompressed_length" {return tt.ftt(170);}
"unhex" {return tt.ftt(171);}
"unix_timestamp" {return tt.ftt(172);}
"upper" {return tt.ftt(173);}
"user" {return tt.ftt(174);}
"utc_date" {return tt.ftt(175);}
"utc_time" {return tt.ftt(176);}
"utc_timestamp" {return tt.ftt(177);}
"uuid" {return tt.ftt(178);}
"uuid_short" {return tt.ftt(179);}
"variance" {return tt.ftt(180);}
"var_pop" {return tt.ftt(181);}
"var_samp" {return tt.ftt(182);}
"version" {return tt.ftt(183);}
"week" {return tt.ftt(184);}
"weekday" {return tt.ftt(185);}
"weekofyear" {return tt.ftt(186);}
"weight_string" {return tt.ftt(187);}
"yearweek" {return tt.ftt(188);}
"year_month" {return tt.ftt(189);}
// MARKER_END_FUNCTIONS


// MARKER_BEGIN_PARAMETERS
// MARKER_END_PARAMETERS


// MARKER_BEGIN_EXCEPTIONS
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.identifier; }
{QUOTED_IDENTIFIER}    { return stt.identifier; }
.                      { return stt.identifier; }

