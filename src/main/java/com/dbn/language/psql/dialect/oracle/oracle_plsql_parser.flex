package com.dbn.language.psql.dialect.oracle;

import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageLexerBase;
import com.intellij.psi.tree.IElementType;

%%

%class OraclePLSQLParserFlexLexer
%extends DBLanguageLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    public OraclePLSQLParserFlexLexer(TokenTypeBundle tt) {
      super(tt);
  }
%}

%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext

VARIABLE = ":"{INTEGER}
SQLP_VARIABLE = "&""&"?{IDENTIFIER}

%state WRAPPED
%state CONDITIONAL
%%

<WRAPPED> {
    {WHITE_SPACE}   { return stt.getWhiteSpace(); }
    .*              { return stt.getLineComment(); }
    .               { return stt.getLineComment(); }
}
<CONDITIONAL> {
    "$end"          { yybegin(YYINITIAL); }
}

{BLOCK_COMMENT}  { return stt.getBlockComment(); }
{LINE_COMMENT}   { return stt.getLineComment(); }

"wrapped"          { yybegin(WRAPPED); return tt.getTokenType("KW_WRAPPED");}
"$if"(~"$then")    { yybegin(CONDITIONAL);}
"$elsif"(~"$then") { }
"$else"            { }
"$then"            { }

{VARIABLE}       { return stt.getVariable(); }
{SQLP_VARIABLE}  { return stt.getVariable(); }


{INTEGER}     { return stt.getInteger(); }
{NUMBER}      { return stt.getNumber(); }
{STRING}      { return stt.getString(); }

"("{wso}"+"{wso}")"  {return tt.getTokenType("CT_OUTER_JOIN");}

"="{wso}"=" {return tt.getOperatorTokenType(0);}
"|"{wso}"|" {return tt.getOperatorTokenType(1);}
"<"{wso}"=" {return tt.getOperatorTokenType(2);}
">"{wso}"=" {return tt.getOperatorTokenType(3);}
"<"{wso}">" {return tt.getOperatorTokenType(4);}
"!"{wso}"=" {return tt.getOperatorTokenType(5);}
":"{wso}"=" {return tt.getOperatorTokenType(6);}
"="{wso}">" {return tt.getOperatorTokenType(7);}
".."        {return tt.getOperatorTokenType(8);}
"::"        {return tt.getOperatorTokenType(9);}



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
"varchar2" {return tt.dtt(0);}
"bfile" {return tt.dtt(1);}
"binary_double" {return tt.dtt(2);}
"binary_float" {return tt.dtt(3);}
"binary_integer" {return tt.dtt(4);}
"blob" {return tt.dtt(5);}
"boolean" {return tt.dtt(6);}
"byte" {return tt.dtt(7);}
"char" {return tt.dtt(8);}
"character" {return tt.dtt(9);}
"character"{ws}"varying" {return tt.dtt(10);}
"clob" {return tt.dtt(11);}
"date" {return tt.dtt(12);}
"decimal" {return tt.dtt(13);}
"double"{ws}"precision" {return tt.dtt(14);}
"float" {return tt.dtt(15);}
"int" {return tt.dtt(16);}
"integer" {return tt.dtt(17);}
"long" {return tt.dtt(18);}
"long"{ws}"raw" {return tt.dtt(19);}
"long"{ws}"varchar" {return tt.dtt(20);}
"national"{ws}"char" {return tt.dtt(21);}
"national"{ws}"char"{ws}"varying" {return tt.dtt(22);}
"national"{ws}"character" {return tt.dtt(23);}
"national"{ws}"character"{ws}"varying" {return tt.dtt(24);}
"nchar" {return tt.dtt(25);}
"nchar"{ws}"varying" {return tt.dtt(26);}
"nclob" {return tt.dtt(27);}
"number" {return tt.dtt(28);}
"numeric" {return tt.dtt(29);}
"nvarchar2" {return tt.dtt(30);}
"pls_integer" {return tt.dtt(31);}
"raw" {return tt.dtt(32);}
"real" {return tt.dtt(33);}
"rowid" {return tt.dtt(34);}
"smallint" {return tt.dtt(35);}
"string" {return tt.dtt(36);}
"timestamp" {return tt.dtt(37);}
"urowid" {return tt.dtt(38);}
"varchar" {return tt.dtt(39);}
"with"{ws}"local"{ws}"time"{ws}"zone" {return tt.dtt(40);}
"with"{ws}"time"{ws}"zone" {return tt.dtt(41);}
// MARKER_END_DATATYPES



// MARKER_BEGIN_KEYWORDS
//"$if" {return tt.ktt(0);}
//"$else" {return tt.ktt(1);}
//"$elsif" {return tt.ktt(2);}
//"$end" {return tt.ktt(3);}
//"$then" {return tt.ktt(4);}
"a set" {return tt.ktt(5);}
"absent" {return tt.ktt(6);}
"accessible" {return tt.ktt(7);}
"after" {return tt.ktt(8);}
"agent" {return tt.ktt(9);}
"aggregate" {return tt.ktt(10);}
"all" {return tt.ktt(11);}
"alter" {return tt.ktt(12);}
"analyze" {return tt.ktt(13);}
"and" {return tt.ktt(14);}
"any" {return tt.ktt(15);}
"apply" {return tt.ktt(16);}
"array" {return tt.ktt(17);}
"as" {return tt.ktt(18);}
"asc" {return tt.ktt(19);}
"associate" {return tt.ktt(20);}
"at" {return tt.ktt(21);}
"audit" {return tt.ktt(22);}
"authid" {return tt.ktt(23);}
"automatic" {return tt.ktt(24);}
"autonomous_transaction" {return tt.ktt(25);}
"before" {return tt.ktt(26);}
"begin" {return tt.ktt(27);}
"between" {return tt.ktt(28);}
"block" {return tt.ktt(29);}
"body" {return tt.ktt(30);}
"both" {return tt.ktt(31);}
"bulk" {return tt.ktt(32);}
"bulk_exceptions" {return tt.ktt(33);}
"bulk_rowcount" {return tt.ktt(34);}
"by" {return tt.ktt(35);}
"c" {return tt.ktt(36);}
"call" {return tt.ktt(37);}
"canonical" {return tt.ktt(38);}
"case" {return tt.ktt(39);}
"char_base" {return tt.ktt(40);}
"char_cs" {return tt.ktt(41);}
"charsetform" {return tt.ktt(42);}
"charsetid" {return tt.ktt(43);}
"check" {return tt.ktt(44);}
"chisq_df" {return tt.ktt(45);}
"chisq_obs" {return tt.ktt(46);}
"chisq_sig" {return tt.ktt(47);}
"clone" {return tt.ktt(48);}
"close" {return tt.ktt(49);}
"cluster" {return tt.ktt(50);}
"coalesce" {return tt.ktt(51);}
"coefficient" {return tt.ktt(52);}
"cohens_k" {return tt.ktt(53);}
"collation" {return tt.ktt(54);}
"collect" {return tt.ktt(55);}
"columns" {return tt.ktt(56);}
"comment" {return tt.ktt(57);}
"commit" {return tt.ktt(58);}
"committed" {return tt.ktt(59);}
"compatibility" {return tt.ktt(60);}
"compound" {return tt.ktt(61);}
"compress" {return tt.ktt(62);}
"conditional" {return tt.ktt(63);}
"connect" {return tt.ktt(64);}
"constant" {return tt.ktt(65);}
"constraint" {return tt.ktt(66);}
"constructor" {return tt.ktt(67);}
"cont_coefficient" {return tt.ktt(68);}
"container" {return tt.ktt(69);}
"content" {return tt.ktt(70);}
"context" {return tt.ktt(71);}
"conversion" {return tt.ktt(72);}
"count" {return tt.ktt(73);}
"cramers_v" {return tt.ktt(74);}
"create" {return tt.ktt(75);}
"cross" {return tt.ktt(76);}
"crossedition" {return tt.ktt(77);}
"cube" {return tt.ktt(78);}
"current" {return tt.ktt(79);}
"current_user" {return tt.ktt(80);}
"currval" {return tt.ktt(81);}
"cursor" {return tt.ktt(82);}
"database" {return tt.ktt(83);}
"day" {return tt.ktt(84);}
"db_role_change" {return tt.ktt(85);}
"ddl" {return tt.ktt(86);}
"declare" {return tt.ktt(87);}
"decrement" {return tt.ktt(88);}
"default" {return tt.ktt(89);}
"defaults" {return tt.ktt(90);}
"definer" {return tt.ktt(91);}
"delete" {return tt.ktt(92);}
"deleting" {return tt.ktt(93);}
"dense_rank" {return tt.ktt(94);}
"deprecate" {return tt.ktt(95);}
"desc" {return tt.ktt(96);}
"deterministic" {return tt.ktt(97);}
"df" {return tt.ktt(98);}
"df_between" {return tt.ktt(99);}
"df_den" {return tt.ktt(100);}
"df_num" {return tt.ktt(101);}
"df_within" {return tt.ktt(102);}
"dimension" {return tt.ktt(103);}
"disable" {return tt.ktt(104);}
"disassociate" {return tt.ktt(105);}
"distinct" {return tt.ktt(106);}
"do" {return tt.ktt(107);}
"document" {return tt.ktt(108);}
"drop" {return tt.ktt(109);}
"dump" {return tt.ktt(110);}
"duration" {return tt.ktt(111);}
"each" {return tt.ktt(112);}
"editionable" {return tt.ktt(113);}
"else" {return tt.ktt(114);}
"elsif" {return tt.ktt(115);}
"empty" {return tt.ktt(116);}
"enable" {return tt.ktt(117);}
"encoding" {return tt.ktt(118);}
"end" {return tt.ktt(119);}
"entityescaping" {return tt.ktt(120);}
"equals_path" {return tt.ktt(121);}
"error" {return tt.ktt(122);}
"error_code" {return tt.ktt(123);}
"error_index" {return tt.ktt(124);}
"errors" {return tt.ktt(125);}
"escape" {return tt.ktt(126);}
"evalname" {return tt.ktt(127);}
"exact_prob" {return tt.ktt(128);}
"except" {return tt.ktt(129);}
"exception" {return tt.ktt(130);}
"exception_init" {return tt.ktt(131);}
"exceptions" {return tt.ktt(132);}
"exclude" {return tt.ktt(133);}
"exclusive" {return tt.ktt(134);}
"execute" {return tt.ktt(135);}
"exists" {return tt.ktt(136);}
"exit" {return tt.ktt(137);}
"extend" {return tt.ktt(138);}
"extends" {return tt.ktt(139);}
"external" {return tt.ktt(140);}
"f_ratio" {return tt.ktt(141);}
"fetch" {return tt.ktt(142);}
"final" {return tt.ktt(143);}
"first" {return tt.ktt(144);}
"following" {return tt.ktt(145);}
"follows" {return tt.ktt(146);}
"for" {return tt.ktt(147);}
"forall" {return tt.ktt(148);}
"force" {return tt.ktt(149);}
"forward" {return tt.ktt(150);}
"found" {return tt.ktt(151);}
"from" {return tt.ktt(152);}
"format" {return tt.ktt(153);}
"full" {return tt.ktt(154);}
"function" {return tt.ktt(155);}
"goto" {return tt.ktt(156);}
"grant" {return tt.ktt(157);}
"group" {return tt.ktt(158);}
"hash" {return tt.ktt(159);}
"having" {return tt.ktt(160);}
"heap" {return tt.ktt(161);}
"hide" {return tt.ktt(162);}
"hour" {return tt.ktt(163);}
"if" {return tt.ktt(164);}
"ignore" {return tt.ktt(165);}
"immediate" {return tt.ktt(166);}
"in" {return tt.ktt(167);}
"include" {return tt.ktt(168);}
"increment" {return tt.ktt(169);}
"indent" {return tt.ktt(170);}
"index" {return tt.ktt(171);}
"indicator" {return tt.ktt(172);}
"indices" {return tt.ktt(173);}
"infinite" {return tt.ktt(174);}
"inline" {return tt.ktt(175);}
"inner" {return tt.ktt(176);}
"insert" {return tt.ktt(177);}
"inserting" {return tt.ktt(178);}
"instantiable" {return tt.ktt(179);}
"instead" {return tt.ktt(180);}
"interface" {return tt.ktt(181);}
"intersect" {return tt.ktt(182);}
"interval" {return tt.ktt(183);}
"into" {return tt.ktt(184);}
"is" {return tt.ktt(185);}
"isolation" {return tt.ktt(186);}
"isopen" {return tt.ktt(187);}
"iterate" {return tt.ktt(188);}
"java" {return tt.ktt(189);}
"join" {return tt.ktt(190);}
"json" {return tt.ktt(191);}
"keep" {return tt.ktt(192);}
"key" {return tt.ktt(193);}
"keys" {return tt.ktt(194);}
"language" {return tt.ktt(195);}
"last" {return tt.ktt(196);}
"leading" {return tt.ktt(197);}
"left" {return tt.ktt(198);}
"level" {return tt.ktt(199);}
"library" {return tt.ktt(200);}
"like" {return tt.ktt(201);}
"like2" {return tt.ktt(202);}
"like4" {return tt.ktt(203);}
"likec" {return tt.ktt(204);}
"limit" {return tt.ktt(205);}
"limited" {return tt.ktt(206);}
"local" {return tt.ktt(207);}
"lock" {return tt.ktt(208);}
"locked" {return tt.ktt(209);}
"log" {return tt.ktt(210);}
"logoff" {return tt.ktt(211);}
"logon" {return tt.ktt(212);}
"loop" {return tt.ktt(213);}
"main" {return tt.ktt(214);}
"map" {return tt.ktt(215);}
"matched" {return tt.ktt(216);}
"maxlen" {return tt.ktt(217);}
"maxvalue" {return tt.ktt(218);}
"mean_squares_between" {return tt.ktt(219);}
"mean_squares_within" {return tt.ktt(220);}
"measures" {return tt.ktt(221);}
"member" {return tt.ktt(222);}
"merge" {return tt.ktt(223);}
"metadata" {return tt.ktt(224);}
"minus" {return tt.ktt(225);}
"minute" {return tt.ktt(226);}
"minvalue" {return tt.ktt(227);}
"mismatch" {return tt.ktt(228);}
"mlslabel" {return tt.ktt(229);}
"mode" {return tt.ktt(230);}
"model" {return tt.ktt(231);}
"month" {return tt.ktt(232);}
"multiset" {return tt.ktt(233);}
"name" {return tt.ktt(234);}
"nan" {return tt.ktt(235);}
"natural" {return tt.ktt(236);}
"naturaln" {return tt.ktt(237);}
"nav" {return tt.ktt(238);}
"nchar_cs" {return tt.ktt(239);}
"nested" {return tt.ktt(240);}
"new" {return tt.ktt(241);}
"next" {return tt.ktt(242);}
"nextval" {return tt.ktt(243);}
"no" {return tt.ktt(244);}
"noaudit" {return tt.ktt(245);}
"nocopy" {return tt.ktt(246);}
"nocycle" {return tt.ktt(247);}
"none" {return tt.ktt(248);}
"noentityescaping" {return tt.ktt(249);}
"noneditionable" {return tt.ktt(250);}
"noschemacheck" {return tt.ktt(251);}
"not" {return tt.ktt(252);}
"notfound" {return tt.ktt(253);}
"nowait" {return tt.ktt(254);}
"null" {return tt.ktt(255);}
"nulls" {return tt.ktt(256);}
"number_base" {return tt.ktt(257);}
"object" {return tt.ktt(258);}
"ocirowid" {return tt.ktt(259);}
"of" {return tt.ktt(260);}
"offset" {return tt.ktt(261);}
"oid" {return tt.ktt(262);}
"old" {return tt.ktt(263);}
"on" {return tt.ktt(264);}
"one_sided_prob_or_less" {return tt.ktt(265);}
"one_sided_prob_or_more" {return tt.ktt(266);}
"one_sided_sig" {return tt.ktt(267);}
"only" {return tt.ktt(268);}
"opaque" {return tt.ktt(269);}
"open" {return tt.ktt(270);}
"operator" {return tt.ktt(271);}
"option" {return tt.ktt(272);}
"or" {return tt.ktt(273);}
"order" {return tt.ktt(274);}
"ordinality" {return tt.ktt(275);}
"organization" {return tt.ktt(276);}
"others" {return tt.ktt(277);}
"out" {return tt.ktt(278);}
"outer" {return tt.ktt(279);}
"over" {return tt.ktt(280);}
"overflow" {return tt.ktt(281);}
"overlaps" {return tt.ktt(282);}
"overriding" {return tt.ktt(283);}
"package" {return tt.ktt(284);}
"parallel_enable" {return tt.ktt(285);}
"parameters" {return tt.ktt(286);}
"parent" {return tt.ktt(287);}
"partition" {return tt.ktt(288);}
"passing" {return tt.ktt(289);}
"path" {return tt.ktt(290);}
"pctfree" {return tt.ktt(291);}
"percent" {return tt.ktt(292);}
"phi_coefficient" {return tt.ktt(293);}
"pipe" {return tt.ktt(294);}
"pipelined" {return tt.ktt(295);}
"pivot" {return tt.ktt(296);}
"pluggable" {return tt.ktt(297);}
"positive" {return tt.ktt(298);}
"positiven" {return tt.ktt(299);}
"power" {return tt.ktt(300);}
"pragma" {return tt.ktt(301);}
"preceding" {return tt.ktt(302);}
"precedes" {return tt.ktt(303);}
"present" {return tt.ktt(304);}
"pretty" {return tt.ktt(305);}
"prior" {return tt.ktt(306);}
"private" {return tt.ktt(307);}
"procedure" {return tt.ktt(308);}
"public" {return tt.ktt(309);}
"raise" {return tt.ktt(310);}
"range" {return tt.ktt(311);}
"read" {return tt.ktt(312);}
"record" {return tt.ktt(313);}
"ref" {return tt.ktt(314);}
"reference" {return tt.ktt(315);}
"referencing" {return tt.ktt(316);}
"regexp_like" {return tt.ktt(317);}
"reject" {return tt.ktt(318);}
"release" {return tt.ktt(319);}
"relies_on" {return tt.ktt(320);}
"remainder" {return tt.ktt(321);}
"rename" {return tt.ktt(322);}
"replace" {return tt.ktt(323);}
"restrict_references" {return tt.ktt(324);}
"result" {return tt.ktt(325);}
"result_cache" {return tt.ktt(326);}
"return" {return tt.ktt(327);}
"returning" {return tt.ktt(328);}
"reverse" {return tt.ktt(329);}
"revoke" {return tt.ktt(330);}
"right" {return tt.ktt(331);}
"rnds" {return tt.ktt(332);}
"rnps" {return tt.ktt(333);}
"rollback" {return tt.ktt(334);}
"rollup" {return tt.ktt(335);}
"row" {return tt.ktt(336);}
"rowcount" {return tt.ktt(337);}
"rownum" {return tt.ktt(338);}
"rows" {return tt.ktt(339);}
"rowtype" {return tt.ktt(340);}
"rules" {return tt.ktt(341);}
"sample" {return tt.ktt(342);}
"save" {return tt.ktt(343);}
"savepoint" {return tt.ktt(344);}
"schema" {return tt.ktt(345);}
"schemacheck" {return tt.ktt(346);}
"scn" {return tt.ktt(347);}
"second" {return tt.ktt(348);}
"seed" {return tt.ktt(349);}
"segment" {return tt.ktt(350);}
"select" {return tt.ktt(351);}
"self" {return tt.ktt(352);}
"separate" {return tt.ktt(353);}
"sequential" {return tt.ktt(354);}
"serializable" {return tt.ktt(355);}
"serially_reusable" {return tt.ktt(356);}
"servererror" {return tt.ktt(357);}
"set" {return tt.ktt(358);}
"sets" {return tt.ktt(359);}
"share" {return tt.ktt(360);}
"sharing" {return tt.ktt(361);}
"show" {return tt.ktt(362);}
"shutdown" {return tt.ktt(363);}
"siblings" {return tt.ktt(364);}
"sig" {return tt.ktt(365);}
"single" {return tt.ktt(366);}
"size" {return tt.ktt(367);}
"skip" {return tt.ktt(368);}
"some" {return tt.ktt(369);}
"space" {return tt.ktt(370);}
"sql" {return tt.ktt(371);}
"sqlcode" {return tt.ktt(372);}
"sqlerrm" {return tt.ktt(373);}
"standalone" {return tt.ktt(374);}
"start" {return tt.ktt(375);}
"startup" {return tt.ktt(376);}
"statement" {return tt.ktt(377);}
"static" {return tt.ktt(378);}
"statistic" {return tt.ktt(379);}
"statistics" {return tt.ktt(380);}
"strict" {return tt.ktt(381);}
"struct" {return tt.ktt(382);}
"submultiset" {return tt.ktt(383);}
"subpartition" {return tt.ktt(384);}
"subtype" {return tt.ktt(385);}
"successful" {return tt.ktt(386);}
"sum_squares_between" {return tt.ktt(387);}
"sum_squares_within" {return tt.ktt(388);}
"suspend" {return tt.ktt(389);}
"synonym" {return tt.ktt(390);}
"table" {return tt.ktt(391);}
"tdo" {return tt.ktt(392);}
"then" {return tt.ktt(393);}
"ties" {return tt.ktt(394);}
"time" {return tt.ktt(395);}
"timezone_abbr" {return tt.ktt(396);}
"timezone_hour" {return tt.ktt(397);}
"timezone_minute" {return tt.ktt(398);}
"timezone_region" {return tt.ktt(399);}
"to" {return tt.ktt(400);}
"trailing" {return tt.ktt(401);}
"transaction" {return tt.ktt(402);}
"trigger" {return tt.ktt(403);}
"truncate" {return tt.ktt(404);}
"trust" {return tt.ktt(405);}
"two_sided_prob" {return tt.ktt(406);}
"two_sided_sig" {return tt.ktt(407);}
"type" {return tt.ktt(408);}
"u_statistic" {return tt.ktt(409);}
"unbounded" {return tt.ktt(410);}
"unconditional" {return tt.ktt(411);}
"under" {return tt.ktt(412);}
"under_path" {return tt.ktt(413);}
"union" {return tt.ktt(414);}
"unique" {return tt.ktt(415);}
"unlimited" {return tt.ktt(416);}
"unpivot" {return tt.ktt(417);}
"unplug" {return tt.ktt(418);}
"until" {return tt.ktt(419);}
"update" {return tt.ktt(420);}
"updated" {return tt.ktt(421);}
"updating" {return tt.ktt(422);}
"upsert" {return tt.ktt(423);}
"use" {return tt.ktt(424);}
"user" {return tt.ktt(425);}
"using" {return tt.ktt(426);}
"validate" {return tt.ktt(427);}
"value" {return tt.ktt(428);}
"values" {return tt.ktt(429);}
"variable" {return tt.ktt(430);}
"varray" {return tt.ktt(431);}
"varying" {return tt.ktt(432);}
"version" {return tt.ktt(433);}
"versions" {return tt.ktt(434);}
"view" {return tt.ktt(435);}
"wait" {return tt.ktt(436);}
"wellformed" {return tt.ktt(437);}
"when" {return tt.ktt(438);}
"whenever" {return tt.ktt(439);}
"where" {return tt.ktt(440);}
"while" {return tt.ktt(441);}
"with" {return tt.ktt(442);}
"within" {return tt.ktt(443);}
"without" {return tt.ktt(444);}
"wnds" {return tt.ktt(445);}
"wnps" {return tt.ktt(446);}
"work" {return tt.ktt(447);}
"write" {return tt.ktt(448);}
"wrapped" {return tt.ktt(449);}
"wrapper" {return tt.ktt(450);}
"xml" {return tt.ktt(451);}
"xmlnamespaces" {return tt.ktt(452);}
"year" {return tt.ktt(453);}
"yes" {return tt.ktt(454);}
"zone" {return tt.ktt(455);}
"false" {return tt.ktt(456);}
"true" {return tt.ktt(457);}
// MARKER_END_KEYWORDS



// MARKER_BEGIN_FUNCTIONS
"abs" {return tt.ftt(0);}
"acos" {return tt.ftt(1);}
"add_months" {return tt.ftt(2);}
"appendchildxml" {return tt.ftt(3);}
"ascii" {return tt.ftt(4);}
"asciistr" {return tt.ftt(5);}
"asin" {return tt.ftt(6);}
"atan" {return tt.ftt(7);}
"atan2" {return tt.ftt(8);}
"avg" {return tt.ftt(9);}
"bfilename" {return tt.ftt(10);}
"bin_to_num" {return tt.ftt(11);}
"bitand" {return tt.ftt(12);}
"cardinality" {return tt.ftt(13);}
"cast" {return tt.ftt(14);}
"ceil" {return tt.ftt(15);}
"chartorowid" {return tt.ftt(16);}
"chr" {return tt.ftt(17);}
"compose" {return tt.ftt(18);}
"concat" {return tt.ftt(19);}
"convert" {return tt.ftt(20);}
"corr" {return tt.ftt(21);}
"corr_k" {return tt.ftt(22);}
"corr_s" {return tt.ftt(23);}
"cos" {return tt.ftt(24);}
"cosh" {return tt.ftt(25);}
"covar_pop" {return tt.ftt(26);}
"covar_samp" {return tt.ftt(27);}
"cume_dist" {return tt.ftt(28);}
"current_date" {return tt.ftt(29);}
"current_timestamp" {return tt.ftt(30);}
"cv" {return tt.ftt(31);}
"dbtimezone" {return tt.ftt(32);}
"dbtmezone" {return tt.ftt(33);}
"decode" {return tt.ftt(34);}
"decompose" {return tt.ftt(35);}
"deletexml" {return tt.ftt(36);}
"depth" {return tt.ftt(37);}
"deref" {return tt.ftt(38);}
"empty_blob" {return tt.ftt(39);}
"empty_clob" {return tt.ftt(40);}
"existsnode" {return tt.ftt(41);}
"exp" {return tt.ftt(42);}
"extract" {return tt.ftt(43);}
"extractvalue" {return tt.ftt(44);}
"first_value" {return tt.ftt(45);}
"floor" {return tt.ftt(46);}
"from_tz" {return tt.ftt(47);}
"greatest" {return tt.ftt(48);}
"group_id" {return tt.ftt(49);}
"grouping" {return tt.ftt(50);}
"grouping_id" {return tt.ftt(51);}
"hextoraw" {return tt.ftt(52);}
"initcap" {return tt.ftt(53);}
"insertchildxml" {return tt.ftt(54);}
"insertchildxmlafter" {return tt.ftt(55);}
"insertchildxmlbefore" {return tt.ftt(56);}
"insertxmlafter" {return tt.ftt(57);}
"insertxmlbefore" {return tt.ftt(58);}
"instr" {return tt.ftt(59);}
"instr2" {return tt.ftt(60);}
"instr4" {return tt.ftt(61);}
"instrb" {return tt.ftt(62);}
"instrc" {return tt.ftt(63);}
"iteration_number" {return tt.ftt(64);}
"json_array" {return tt.ftt(65);}
"json_arrayagg" {return tt.ftt(66);}
"json_dataguide" {return tt.ftt(67);}
"json_object" {return tt.ftt(68);}
"json_objectagg" {return tt.ftt(69);}
"json_query" {return tt.ftt(70);}
"json_table" {return tt.ftt(71);}
"json_value" {return tt.ftt(72);}
"lag" {return tt.ftt(73);}
"last_day" {return tt.ftt(74);}
"last_value" {return tt.ftt(75);}
"lateral" {return tt.ftt(76);}
"lead" {return tt.ftt(77);}
"least" {return tt.ftt(78);}
"length" {return tt.ftt(79);}
"length2" {return tt.ftt(80);}
"length4" {return tt.ftt(81);}
"lengthb" {return tt.ftt(82);}
"lengthc" {return tt.ftt(83);}
"listagg" {return tt.ftt(84);}
"ln" {return tt.ftt(85);}
"lnnvl" {return tt.ftt(86);}
"localtimestamp" {return tt.ftt(87);}
"lower" {return tt.ftt(88);}
"lpad" {return tt.ftt(89);}
"ltrim" {return tt.ftt(90);}
"make_ref" {return tt.ftt(91);}
"max" {return tt.ftt(92);}
"median" {return tt.ftt(93);}
"min" {return tt.ftt(94);}
"mod" {return tt.ftt(95);}
"months_between" {return tt.ftt(96);}
"nanvl" {return tt.ftt(97);}
"nchr" {return tt.ftt(98);}
"new_time" {return tt.ftt(99);}
"next_day" {return tt.ftt(100);}
"nls_charset_decl_len" {return tt.ftt(101);}
"nls_charset_id" {return tt.ftt(102);}
"nls_charset_name" {return tt.ftt(103);}
"nls_initcap" {return tt.ftt(104);}
"nls_lower" {return tt.ftt(105);}
"nls_upper" {return tt.ftt(106);}
"nlssort" {return tt.ftt(107);}
"ntile" {return tt.ftt(108);}
"nullif" {return tt.ftt(109);}
"numtodsinterval" {return tt.ftt(110);}
"numtoyminterval" {return tt.ftt(111);}
"nvl" {return tt.ftt(112);}
"nvl2" {return tt.ftt(113);}
"ora_hash" {return tt.ftt(114);}
"percent_rank" {return tt.ftt(115);}
"percentile_cont" {return tt.ftt(116);}
"percentile_disc" {return tt.ftt(117);}
"powermultiset" {return tt.ftt(118);}
"powermultiset_by_cardinality" {return tt.ftt(119);}
"presentnnv" {return tt.ftt(120);}
"presentv" {return tt.ftt(121);}
"previous" {return tt.ftt(122);}
"rank" {return tt.ftt(123);}
"ratio_to_report" {return tt.ftt(124);}
"rawtohex" {return tt.ftt(125);}
"rawtonhex" {return tt.ftt(126);}
"reftohex" {return tt.ftt(127);}
"regexp_instr" {return tt.ftt(128);}
"regexp_replace" {return tt.ftt(129);}
"regexp_substr" {return tt.ftt(130);}
"regr_avgx" {return tt.ftt(131);}
"regr_avgy" {return tt.ftt(132);}
"regr_count" {return tt.ftt(133);}
"regr_intercept" {return tt.ftt(134);}
"regr_r2" {return tt.ftt(135);}
"regr_slope" {return tt.ftt(136);}
"regr_sxx" {return tt.ftt(137);}
"regr_sxy" {return tt.ftt(138);}
"regr_syy" {return tt.ftt(139);}
"round" {return tt.ftt(140);}
"row_number" {return tt.ftt(141);}
"rowidtochar" {return tt.ftt(142);}
"rowidtonchar" {return tt.ftt(143);}
"rpad" {return tt.ftt(144);}
"rtrim" {return tt.ftt(145);}
"scn_to_timestamp" {return tt.ftt(146);}
"sessiontimezone" {return tt.ftt(147);}
"sign" {return tt.ftt(148);}
"sin" {return tt.ftt(149);}
"sinh" {return tt.ftt(150);}
"soundex" {return tt.ftt(151);}
"sqrt" {return tt.ftt(152);}
"stats_binomial_test" {return tt.ftt(153);}
"stats_crosstab" {return tt.ftt(154);}
"stats_f_test" {return tt.ftt(155);}
"stats_ks_test" {return tt.ftt(156);}
"stats_mode" {return tt.ftt(157);}
"stats_mw_test" {return tt.ftt(158);}
"stats_one_way_anova" {return tt.ftt(159);}
"stats_t_test_indep" {return tt.ftt(160);}
"stats_t_test_indepu" {return tt.ftt(161);}
"stats_t_test_one" {return tt.ftt(162);}
"stats_t_test_paired" {return tt.ftt(163);}
"stats_wsr_test" {return tt.ftt(164);}
"stddev" {return tt.ftt(165);}
"stddev_pop" {return tt.ftt(166);}
"stddev_samp" {return tt.ftt(167);}
"substr" {return tt.ftt(168);}
"substr2" {return tt.ftt(169);}
"substr4" {return tt.ftt(170);}
"substrb" {return tt.ftt(171);}
"substrc" {return tt.ftt(172);}
"sum" {return tt.ftt(173);}
"sys_connect_by_path" {return tt.ftt(174);}
"sys_context" {return tt.ftt(175);}
"sys_dburigen" {return tt.ftt(176);}
"sys_extract_utc" {return tt.ftt(177);}
"sys_guid" {return tt.ftt(178);}
"sys_typeid" {return tt.ftt(179);}
"sys_xmlagg" {return tt.ftt(180);}
"sys_xmlgen" {return tt.ftt(181);}
"sysdate" {return tt.ftt(182);}
"systimestamp" {return tt.ftt(183);}
"tan" {return tt.ftt(184);}
"tanh" {return tt.ftt(185);}
"timestamp_to_scn" {return tt.ftt(186);}
"to_binary_double" {return tt.ftt(187);}
"to_binary_float" {return tt.ftt(188);}
"to_char" {return tt.ftt(189);}
"to_clob" {return tt.ftt(190);}
"to_date" {return tt.ftt(191);}
"to_dsinterval" {return tt.ftt(192);}
"to_lob" {return tt.ftt(193);}
"to_multi_byte" {return tt.ftt(194);}
"to_nchar" {return tt.ftt(195);}
"to_nclob" {return tt.ftt(196);}
"to_number" {return tt.ftt(197);}
"to_single_byte" {return tt.ftt(198);}
"to_timestamp" {return tt.ftt(199);}
"to_timestamp_tz" {return tt.ftt(200);}
"to_yminterval" {return tt.ftt(201);}
"translate" {return tt.ftt(202);}
"treat" {return tt.ftt(203);}
"trim" {return tt.ftt(204);}
"trunc" {return tt.ftt(205);}
"tz_offset" {return tt.ftt(206);}
"uid" {return tt.ftt(207);}
"unistr" {return tt.ftt(208);}
"updatexml" {return tt.ftt(209);}
"upper" {return tt.ftt(210);}
"userenv" {return tt.ftt(211);}
"validate_conversion" {return tt.ftt(212);}
"var_pop" {return tt.ftt(213);}
"var_samp" {return tt.ftt(214);}
"variance" {return tt.ftt(215);}
"vsize" {return tt.ftt(216);}
"width_bucket" {return tt.ftt(217);}
"xmlagg" {return tt.ftt(218);}
"xmlattributes" {return tt.ftt(219);}
"xmlcast" {return tt.ftt(220);}
"xmlcdata" {return tt.ftt(221);}
"xmlcolattval" {return tt.ftt(222);}
"xmlcomment" {return tt.ftt(223);}
"xmlconcat" {return tt.ftt(224);}
"xmldiff" {return tt.ftt(225);}
"xmlelement" {return tt.ftt(226);}
"xmlforest" {return tt.ftt(227);}
"xmlisvalid" {return tt.ftt(228);}
"xmlparse" {return tt.ftt(229);}
"xmlpatch" {return tt.ftt(230);}
"xmlpi" {return tt.ftt(231);}
"xmlquery" {return tt.ftt(232);}
"xmlroot" {return tt.ftt(233);}
"xmlsequence" {return tt.ftt(234);}
"xmlserialize" {return tt.ftt(235);}
"xmltable" {return tt.ftt(236);}
"xmltransform" {return tt.ftt(237);}
// MARKER_END_FUNCTIONS



// MARKER_BEGIN_PARAMETERS
"using_nls_comp" {return tt.ptt(0);}
// MARKER_END_PARAMETERS

// MARKER_BEGIN_EXCEPTIONS
"access_into_null" {return tt.ett(0);}
"case_not_found" {return tt.ett(1);}
"collection_is_null" {return tt.ett(2);}
"cursor_already_open" {return tt.ett(3);}
"dup_val_on_index" {return tt.ett(4);}
"invalid_cursor" {return tt.ett(5);}
"invalid_number" {return tt.ett(6);}
"login_denied" {return tt.ett(7);}
"no_data_found" {return tt.ett(8);}
"not_logged_on" {return tt.ett(9);}
"program_error" {return tt.ett(10);}
"rowtype_mismatch" {return tt.ett(11);}
"self_is_null" {return tt.ett(12);}
"storage_error" {return tt.ett(13);}
"subscript_beyond_count" {return tt.ett(14);}
"subscript_outside_limit" {return tt.ett(15);}
"sys_invalid_rowid" {return tt.ett(16);}
"timeout_on_resource" {return tt.ett(17);}
"too_many_rows" {return tt.ett(18);}
"value_error" {return tt.ett(19);}
"zero_divide" {return tt.ett(20);}
// MARKER_END_EXCEPTIONS

{IDENTIFIER}           { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}    { return stt.getQuotedIdentifier(); }
{WHITE_SPACE}          { return stt.getWhiteSpace(); }
.                      { return stt.getIdentifier(); }


