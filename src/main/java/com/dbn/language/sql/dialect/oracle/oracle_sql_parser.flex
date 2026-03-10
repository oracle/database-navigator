package com.dbn.language.sql.dialect.oracle;

import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageCompoundLexerBase;
import com.intellij.psi.tree.IElementType;

import static com.dbn.language.sql.dialect.oracle.OraclePLSQLBlockMonitor.Marker;

%%

%class OracleSQLParserFlexLexer
%extends DBLanguageCompoundLexerBase
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}

%{
    private final OraclePLSQLBlockMonitor pbm = new OraclePLSQLBlockMonitor(this, YYINITIAL, PSQL_BLOCK);

    public OracleSQLParserFlexLexer(TokenTypeBundle tt) {
        super(tt, DBLanguageDialectIdentifier.ORACLE_PLSQL);
    }

    public void setTokenStart(int tokenStart) {
        zzStartRead = tokenStart;
    }

    public int getCurrentPosition() {
        return zzCurrentPos;
    }

    public String getCurrentToken() {
        return ((String) zzBuffer).substring(zzStartRead, zzMarkedPos);
    }
%}


%include ../../../common/lexer/shared_elements.flext
%include ../../../common/lexer/shared_elements_oracle.flext

NON_PSQL_BLOCK_ENTER = ("grant"|"revoke"){ws}"create"
NON_PSQL_BLOCK_EXIT = "to"|"from"|";"

PSQL_STUB_OR_REPLACE = ({ws}"or"{ws}"replace")?
PSQL_STUB_EDITIONABLE = ({ws}("editionable"|"editioning"|'noneditionable'))?
PSQL_STUB_FORCE = ({ws}("no"{ws})?"force")?
PSQL_STUB_PUBLIC = ({ws}"public")?
PSQL_STUB_PROGRAM = {ws}("package"|"trigger"|"function"|"procedure"|"type")
PSQL_STUB_IDENTIFIER = ({ws}({IDENTIFIER}|{QUOTED_IDENTIFIER}))*

PSQL_BLOCK_START_CREATE = "create"{PSQL_STUB_OR_REPLACE}{PSQL_STUB_FORCE}{PSQL_STUB_EDITIONABLE}{PSQL_STUB_PUBLIC}{PSQL_STUB_PROGRAM}
PSQL_BLOCK_START_DECLARE = "declare"
PSQL_BLOCK_START_BEGIN = "begin"
//PSQL_BLOCK_END_IGNORE = "end"{ws}("if"|"loop"|"case"){PSQL_STUB_IDENTIFIER}{wso}";"
PSQL_BLOCK_END_IGNORE = "end"{ws}("if"|"loop"){PSQL_STUB_IDENTIFIER}{wso}";"
PSQL_BLOCK_END = "end"{PSQL_STUB_IDENTIFIER}({wso}";"({wso}"/")?)?

CT_SIZE_CLAUSE = {INTEGER}{wso}("k"|"m"|"g"|"t"|"p"|"e"){ws}
SELECT_AI_START = "select"{ws}"ai"

VARIABLE = ":"({IDENTIFIER}|{INTEGER})
SQLP_VARIABLE = "&""&"?({IDENTIFIER}|{INTEGER})
VARIABLE_IDENTIFIER={IDENTIFIER}"&""&"?({IDENTIFIER}|{INTEGER})|"<"{IDENTIFIER}({ws}{IDENTIFIER})*">"

%state PSQL_BLOCK
%state NON_PSQL_BLOCK
%state SELECT_AI
%%

<PSQL_BLOCK> {
    {BLOCK_COMMENT}                 {}
    {LINE_COMMENT}                  {}

    {PSQL_BLOCK_START_CREATE}       {if (pbm.isBlockStarted()) { pbm.pushBack(); pbm.end(true); return getChameleon(); }}
    {PSQL_BLOCK_END_IGNORE}         { pbm.ignore();}
    {PSQL_BLOCK_END}                { if (pbm.end(false)) return getChameleon();}

    "begin"                         { pbm.mark(Marker.BEGIN); }
    "type"{ws}{IDENTIFIER}          { pbm.mark(Marker.PROGRAM); }
    "function"{ws}{IDENTIFIER}      { pbm.mark(Marker.PROGRAM); }
    "procedure"{ws}{IDENTIFIER}     { pbm.mark(Marker.PROGRAM); }
    "trigger"{ws}{IDENTIFIER}       { pbm.mark(Marker.PROGRAM); }
    "case"                          { pbm.mark(Marker.CASE); }

    {IDENTIFIER}                    {}
    {INTEGER}                       {}
    {NUMBER}                        {}
    {STRING}                        {}
    {WHITE_SPACE}                   {}
    .                               {}
    <<EOF>>                         { pbm.end(true); return getChameleon(); }
}

<NON_PSQL_BLOCK> {
    {NON_PSQL_BLOCK_EXIT}          { yybegin(YYINITIAL); pbm.pushBack(); }
}


<YYINITIAL> {
    {NON_PSQL_BLOCK_ENTER}         { yybegin(NON_PSQL_BLOCK); pbm.pushBack(); }

    {PSQL_BLOCK_START_CREATE}      { pbm.start(Marker.CREATE); }
    {PSQL_BLOCK_START_DECLARE}     { pbm.start(Marker.DECLARE); }
    {PSQL_BLOCK_START_BEGIN}       { pbm.start(Marker.BEGIN); }

    {SELECT_AI_START}              { yybegin(SELECT_AI); yypushback(yylength()); }
}

<SELECT_AI> {
    "select"           { return tt.getTokenType("KW_SELECT"); }
    "ai"               { return tt.getTokenType("KW_AI"); }
    "showprompt"       { return tt.getTokenType("KW_SHOWPROMPT"); }
    "showsql"          { return tt.getTokenType("KW_SHOWSQL"); }
    "explainsql"       { return tt.getTokenType("KW_EXPLAINSQL"); }
    "executesql"       { return tt.getTokenType("KW_EXECUTESQL"); }
    "narrate"          { return tt.getTokenType("KW_NARRATE"); }
    "chat"             { return tt.getTokenType("KW_CHAT"); }
    {STRING}           { yybegin(YYINITIAL); return stt.getString(); }   // string is allowed to have eols
    {eol}              { yybegin(YYINITIAL); return stt.getWhiteSpace();} // end of line -> exit the SELECT_AI block
    ";"                { yybegin(YYINITIAL); return stt.getChrSemicolon();}
    "/"                { yybegin(YYINITIAL); return stt.getChrSlash();}
    [^\r\n\t\f ;/]+     { return stt.getIdentifier();}
    {wsc}+             { return stt.getWhiteSpace(); }
}

<YYINITIAL, NON_PSQL_BLOCK> {

{BLOCK_COMMENT}        { return stt.getBlockComment(); }
{LINE_COMMENT}         { return stt.getLineComment(); }

{VARIABLE}             { return stt.getVariable(); }
{VARIABLE_IDENTIFIER}  { return stt.getIdentifier(); }
{SQLP_VARIABLE}        { return stt.getVariable(); }

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



"varchar2" {return tt.dtt(0);}
"bfile" {return tt.dtt(1);}
"binary_double" {return tt.dtt(2);}
"binary_float" {return tt.dtt(3);}
"blob" {return tt.dtt(4);}
"boolean" {return tt.dtt(5);}
"byte" {return tt.dtt(6);}
"char" {return tt.dtt(7);}
"character" {return tt.dtt(8);}
"character"{ws}"varying" {return tt.dtt(9);}
"clob" {return tt.dtt(10);}
"date" {return tt.dtt(11);}
"decimal" {return tt.dtt(12);}
"double"{ws}"precision" {return tt.dtt(13);}
"float" {return tt.dtt(14);}
"float32" {return tt.dtt(15);}
"float64" {return tt.dtt(16);}
"int" {return tt.dtt(17);}
"int8" {return tt.dtt(18);}
"integer" {return tt.dtt(19);}
"interval" {return tt.dtt(20);}
"long" {return tt.dtt(21);}
"long"{ws}"raw" {return tt.dtt(22);}
"long"{ws}"varchar" {return tt.dtt(23);}
"national"{ws}"char" {return tt.dtt(24);}
"national"{ws}"char"{ws}"varying" {return tt.dtt(25);}
"national"{ws}"character" {return tt.dtt(26);}
"national"{ws}"character"{ws}"varying" {return tt.dtt(27);}
"nchar" {return tt.dtt(28);}
"nchar"{ws}"varying" {return tt.dtt(29);}
"nclob" {return tt.dtt(30);}
"number" {return tt.dtt(31);}
"numeric" {return tt.dtt(32);}
"nvarchar2" {return tt.dtt(33);}
"raw" {return tt.dtt(34);}
"real" {return tt.dtt(35);}
"rowid" {return tt.dtt(36);}
"smallint" {return tt.dtt(37);}
"timestamp" {return tt.dtt(38);}
"urowid" {return tt.dtt(39);}
"varchar" {return tt.dtt(40);}
"vector" {return tt.dtt(41);}
"with"{ws}"local"{ws}"time"{ws}"zone" {return tt.dtt(42);}
"with"{ws}"time"{ws}"zone" {return tt.dtt(43);}




"a set" {return tt.ktt(0);}
"abort" {return tt.ktt(1);}
"absent" {return tt.ktt(2);}
"access" {return tt.ktt(3);}
"accessed" {return tt.ktt(4);}
"accessible" {return tt.ktt(5);}
"account" {return tt.ktt(6);}
"anyschema" {return tt.ktt(7);}
"archival" {return tt.ktt(8);}
"activate" {return tt.ktt(9);}
"active" {return tt.ktt(10);}
"add" {return tt.ktt(11);}
"admin" {return tt.ktt(12);}
"administer" {return tt.ktt(13);}
"advanced" {return tt.ktt(14);}
"advise" {return tt.ktt(15);}
"advisor" {return tt.ktt(16);}
"after" {return tt.ktt(17);}
"agent" {return tt.ktt(18);}
"ai" {return tt.ktt(19);}
"alias" {return tt.ktt(20);}
"all" {return tt.ktt(21);}
"allocate" {return tt.ktt(22);}
"allow" {return tt.ktt(23);}
"alter" {return tt.ktt(24);}
"always" {return tt.ktt(25);}
"analyze" {return tt.ktt(26);}
"ancillary" {return tt.ktt(27);}
"and" {return tt.ktt(28);}
"annotations" {return tt.ktt(29);}
"any" {return tt.ktt(30);}
"apply" {return tt.ktt(31);}
"archive" {return tt.ktt(32);}
"archivelog" {return tt.ktt(33);}
"array" {return tt.ktt(34);}
"as" {return tt.ktt(35);}
"asc" {return tt.ktt(36);}
"asynchronous" {return tt.ktt(37);}
"assembly" {return tt.ktt(38);}
"at" {return tt.ktt(39);}
"attribute" {return tt.ktt(40);}
"attributes" {return tt.ktt(41);}
"audit" {return tt.ktt(42);}
"authid" {return tt.ktt(43);}
"authentication" {return tt.ktt(44);}
"auto" {return tt.ktt(45);}
"autoextend" {return tt.ktt(46);}
"automatic" {return tt.ktt(47);}
"availability" {return tt.ktt(48);}
"backup" {return tt.ktt(49);}
"basic" {return tt.ktt(50);}
"basicfile" {return tt.ktt(51);}
"become" {return tt.ktt(52);}
"before" {return tt.ktt(53);}
"begin" {return tt.ktt(54);}
"beginning" {return tt.ktt(55);}
"bequeath" {return tt.ktt(56);}
"between" {return tt.ktt(57);}
"bigfile" {return tt.ktt(58);}
"binary" {return tt.ktt(59);}
"binding" {return tt.ktt(60);}
"bitmap" {return tt.ktt(61);}
"blankline" {return tt.ktt(62);}
"block" {return tt.ktt(63);}
"blockchain" {return tt.ktt(64);}
"body" {return tt.ktt(65);}
"both" {return tt.ktt(66);}
"buffer_cache" {return tt.ktt(67);}
"buffer_pool" {return tt.ktt(68);}
"build" {return tt.ktt(69);}
"by" {return tt.ktt(70);}
"cache" {return tt.ktt(71);}
"cancel" {return tt.ktt(72);}
"canonical" {return tt.ktt(73);}
"capacity" {return tt.ktt(74);}
"cascade" {return tt.ktt(75);}
"case" {return tt.ktt(76);}
"category" {return tt.ktt(77);}
"cell_flash_cache" {return tt.ktt(78);}
"change" {return tt.ktt(79);}
"char_cs" {return tt.ktt(80);}
"characters" {return tt.ktt(81);}
"chars" {return tt.ktt(82);}
"chat" {return tt.ktt(83);}
"check" {return tt.ktt(84);}
"checkpoint" {return tt.ktt(85);}
"child" {return tt.ktt(86);}
"chisq_df" {return tt.ktt(87);}
"chisq_obs" {return tt.ktt(88);}
"chisq_sig" {return tt.ktt(89);}
"chunk" {return tt.ktt(90);}
"class" {return tt.ktt(91);}
"clear" {return tt.ktt(92);}
"clone" {return tt.ktt(93);}
"close" {return tt.ktt(94);}
"cluster" {return tt.ktt(95);}
"clustering" {return tt.ktt(96);}
"coalesce" {return tt.ktt(97);}
"coarse" {return tt.ktt(98);}
"coefficient" {return tt.ktt(99);}
"cohens_k" {return tt.ktt(100);}
"collate" {return tt.ktt(101);}
"collation" {return tt.ktt(102);}
"column" {return tt.ktt(103);}
"column_value" {return tt.ktt(104);}
"columns" {return tt.ktt(105);}
"comment" {return tt.ktt(106);}
"commit" {return tt.ktt(107);}
"committed" {return tt.ktt(108);}
"compact" {return tt.ktt(109);}
"compatibility" {return tt.ktt(110);}
"compile" {return tt.ktt(111);}
"complete" {return tt.ktt(112);}
"compress" {return tt.ktt(113);}
"computation" {return tt.ktt(114);}
"compute" {return tt.ktt(115);}
"conditional" {return tt.ktt(116);}
"connect" {return tt.ktt(117);}
"consider" {return tt.ktt(118);}
"consistent" {return tt.ktt(119);}
"constraint" {return tt.ktt(120);}
"constraints" {return tt.ktt(121);}
"cont_coefficient" {return tt.ktt(122);}
"container" {return tt.ktt(123);}
"container_map" {return tt.ktt(124);}
"containers_default" {return tt.ktt(125);}
"content" {return tt.ktt(126);}
"contents" {return tt.ktt(127);}
"context" {return tt.ktt(128);}
"continue" {return tt.ktt(129);}
"controlfile" {return tt.ktt(130);}
"conversion" {return tt.ktt(131);}
"corruption" {return tt.ktt(132);}
"cost" {return tt.ktt(133);}
"cramers_v" {return tt.ktt(134);}
"create" {return tt.ktt(135);}
"creation" {return tt.ktt(136);}
"credential" {return tt.ktt(137);}
"critical" {return tt.ktt(138);}
"cross" {return tt.ktt(139);}
"cube" {return tt.ktt(140);}
"current" {return tt.ktt(141);}
"current_user" {return tt.ktt(142);}
"currval" {return tt.ktt(143);}
"cursor" {return tt.ktt(144);}
"custom" {return tt.ktt(145);}
"cycle" {return tt.ktt(146);}
"data" {return tt.ktt(147);}
"database" {return tt.ktt(148);}
"datafile" {return tt.ktt(149);}
"datafiles" {return tt.ktt(150);}
"day" {return tt.ktt(151);}
"days" {return tt.ktt(152);}
"dense" {return tt.ktt(153);}
"ddl" {return tt.ktt(154);}
"deallocate" {return tt.ktt(155);}
"debug" {return tt.ktt(156);}
"decrement" {return tt.ktt(157);}
"decrypt" {return tt.ktt(158);}
"default" {return tt.ktt(159);}
"defaults" {return tt.ktt(160);}
"deferrable" {return tt.ktt(161);}
"deferred" {return tt.ktt(162);}
"definer" {return tt.ktt(163);}
"definition" {return tt.ktt(164);}
"delay" {return tt.ktt(165);}
"delegate" {return tt.ktt(166);}
"delete" {return tt.ktt(167);}
"delete_all" {return tt.ktt(168);}
"demand" {return tt.ktt(169);}
"dense_rank" {return tt.ktt(170);}
"dequeue" {return tt.ktt(171);}
"desc" {return tt.ktt(172);}
"determines" {return tt.ktt(173);}
"df" {return tt.ktt(174);}
"df_between" {return tt.ktt(175);}
"df_den" {return tt.ktt(176);}
"df_num" {return tt.ktt(177);}
"df_within" {return tt.ktt(178);}
"dictionary" {return tt.ktt(179);}
"digest" {return tt.ktt(180);}
"dimension" {return tt.ktt(181);}
"directory" {return tt.ktt(182);}
"disable" {return tt.ktt(183);}
"disable_all" {return tt.ktt(184);}
"disallow" {return tt.ktt(185);}
"disconnect" {return tt.ktt(186);}
"disk" {return tt.ktt(187);}
"diskgroup" {return tt.ktt(188);}
"disks" {return tt.ktt(189);}
"dismount" {return tt.ktt(190);}
"distinct" {return tt.ktt(191);}
"distribute" {return tt.ktt(192);}
"distributed" {return tt.ktt(193);}
"dml" {return tt.ktt(194);}
"document" {return tt.ktt(195);}
"domain" {return tt.ktt(196);}
"downgrade" {return tt.ktt(197);}
"drop" {return tt.ktt(198);}
"duality" {return tt.ktt(199);}
"dump" {return tt.ktt(200);}
"duplicate" {return tt.ktt(201);}
"duplicated" {return tt.ktt(202);}
"edition" {return tt.ktt(203);}
"editions" {return tt.ktt(204);}
"editionable" {return tt.ktt(205);}
"editioning" {return tt.ktt(206);}
"element" {return tt.ktt(207);}
"else" {return tt.ktt(208);}
"empty" {return tt.ktt(209);}
"enable" {return tt.ktt(210);}
"enable_all" {return tt.ktt(211);}
"encoding" {return tt.ktt(212);}
"encrypt" {return tt.ktt(213);}
"end" {return tt.ktt(214);}
"enforced" {return tt.ktt(215);}
"entityescaping" {return tt.ktt(216);}
"entry" {return tt.ktt(217);}
"equals_path" {return tt.ktt(218);}
"error" {return tt.ktt(219);}
"errors" {return tt.ktt(220);}
"escape" {return tt.ktt(221);}
"etag" {return tt.ktt(222);}
"evalname" {return tt.ktt(223);}
"evaluate" {return tt.ktt(224);}
"evaluation" {return tt.ktt(225);}
"exact_prob" {return tt.ktt(226);}
"except" {return tt.ktt(227);}
"exceptions" {return tt.ktt(228);}
"exchange" {return tt.ktt(229);}
"exclude" {return tt.ktt(230);}
"excluding" {return tt.ktt(231);}
"exclusive" {return tt.ktt(232);}
"execute" {return tt.ktt(233);}
"executesql" {return tt.ktt(234);}
"exempt" {return tt.ktt(235);}
"exists" {return tt.ktt(236);}
"expire" {return tt.ktt(237);}
"explain" {return tt.ktt(238);}
"explainsql" {return tt.ktt(239);}
"export" {return tt.ktt(240);}
"extended" {return tt.ktt(241);}
"extends" {return tt.ktt(242);}
"extensions" {return tt.ktt(243);}
"extent" {return tt.ktt(244);}
"external" {return tt.ktt(245);}
"externally" {return tt.ktt(246);}
"f_ratio" {return tt.ktt(247);}
"failed" {return tt.ktt(248);}
"failgroup" {return tt.ktt(249);}
"fast" {return tt.ktt(250);}
"fetch" {return tt.ktt(251);}
"file" {return tt.ktt(252);}
"final" {return tt.ktt(253);}
"fine" {return tt.ktt(254);}
"finish" {return tt.ktt(255);}
"first" {return tt.ktt(256);}
"flash_cache" {return tt.ktt(257);}
"flashback" {return tt.ktt(258);}
"flex" {return tt.ktt(259);}
"flush" {return tt.ktt(260);}
"folder" {return tt.ktt(261);}
"following" {return tt.ktt(262);}
"for" {return tt.ktt(263);}
"force" {return tt.ktt(264);}
"foreign" {return tt.ktt(265);}
"format" {return tt.ktt(266);}
"freelist" {return tt.ktt(267);}
"freelists" {return tt.ktt(268);}
"freepools" {return tt.ktt(269);}
"fresh" {return tt.ktt(270);}
"from" {return tt.ktt(271);}
"full" {return tt.ktt(272);}
"function" {return tt.ktt(273);}
"generated" {return tt.ktt(274);}
"global" {return tt.ktt(275);}
"global_name" {return tt.ktt(276);}
"globally" {return tt.ktt(277);}
"grant" {return tt.ktt(278);}
"group" {return tt.ktt(279);}
"groups" {return tt.ktt(280);}
"guard" {return tt.ktt(281);}
"hash" {return tt.ktt(282);}
"hashing" {return tt.ktt(283);}
"having" {return tt.ktt(284);}
"heap" {return tt.ktt(285);}
"hide" {return tt.ktt(286);}
"hierarchy" {return tt.ktt(287);}
"high" {return tt.ktt(288);}
"history" {return tt.ktt(289);}
"hour" {return tt.ktt(290);}
"http" {return tt.ktt(291);}
"id" {return tt.ktt(292);}
"identified" {return tt.ktt(293);}
"identifier" {return tt.ktt(294);}
"identity" {return tt.ktt(295);}
"idle" {return tt.ktt(296);}
"if" {return tt.ktt(297);}
"ignore" {return tt.ktt(298);}
"ilm" {return tt.ktt(299);}
"immediate" {return tt.ktt(300);}
"immutable" {return tt.ktt(301);}
"import" {return tt.ktt(302);}
"in" {return tt.ktt(303);}
"include" {return tt.ktt(304);}
"including" {return tt.ktt(305);}
"increment" {return tt.ktt(306);}
"indent" {return tt.ktt(307);}
"index" {return tt.ktt(308);}
"indexes" {return tt.ktt(309);}
"indexing" {return tt.ktt(310);}
"indextype" {return tt.ktt(311);}
"infinite" {return tt.ktt(312);}
"initial" {return tt.ktt(313);}
"initialized" {return tt.ktt(314);}
"initially" {return tt.ktt(315);}
"initrans" {return tt.ktt(316);}
"inmemory" {return tt.ktt(317);}
"inner" {return tt.ktt(318);}
"insert" {return tt.ktt(319);}
"instance" {return tt.ktt(320);}
"instantiable" {return tt.ktt(321);}
"intermediate" {return tt.ktt(322);}
"intersect" {return tt.ktt(323);}
"intervaled" {return tt.ktt(324);}
"into" {return tt.ktt(325);}
"invalidate" {return tt.ktt(326);}
"invalidation" {return tt.ktt(327);}
"invisible" {return tt.ktt(328);}
"is" {return tt.ktt(329);}
"iterate" {return tt.ktt(330);}
"java" {return tt.ktt(331);}
"job" {return tt.ktt(332);}
"lobs" {return tt.ktt(333);}
"join" {return tt.ktt(334);}
"json" {return tt.ktt(335);}
"keep" {return tt.ktt(336);}
"key" {return tt.ktt(337);}
"keys" {return tt.ktt(338);}
"kill" {return tt.ktt(339);}
"language" {return tt.ktt(340);}
"last" {return tt.ktt(341);}
"leading" {return tt.ktt(342);}
"left" {return tt.ktt(343);}
"less" {return tt.ktt(344);}
"level" {return tt.ktt(345);}
"levels" {return tt.ktt(346);}
"library" {return tt.ktt(347);}
"like" {return tt.ktt(348);}
"like2" {return tt.ktt(349);}
"like4" {return tt.ktt(350);}
"likec" {return tt.ktt(351);}
"limit" {return tt.ktt(352);}
"linear" {return tt.ktt(353);}
"link" {return tt.ktt(354);}
"list" {return tt.ktt(355);}
"load" {return tt.ktt(356);}
"lob" {return tt.ktt(357);}
"local" {return tt.ktt(358);}
"location" {return tt.ktt(359);}
"locator" {return tt.ktt(360);}
"lock" {return tt.ktt(361);}
"lockdown" {return tt.ktt(362);}
"locked" {return tt.ktt(363);}
"locking" {return tt.ktt(364);}
"log" {return tt.ktt(365);}
"logfile" {return tt.ktt(366);}
"logging" {return tt.ktt(367);}
"logical" {return tt.ktt(368);}
"low" {return tt.ktt(369);}
"low_cost_tbs" {return tt.ktt(370);}
"main" {return tt.ktt(371);}
"manage" {return tt.ktt(372);}
"managed" {return tt.ktt(373);}
"manager" {return tt.ktt(374);}
"management" {return tt.ktt(375);}
"manual" {return tt.ktt(376);}
"mapping" {return tt.ktt(377);}
"master" {return tt.ktt(378);}
"matched" {return tt.ktt(379);}
"materialized" {return tt.ktt(380);}
"maxextents" {return tt.ktt(381);}
"maximize" {return tt.ktt(382);}
"maxsize" {return tt.ktt(383);}
"maxtrans" {return tt.ktt(384);}
"maxvalue" {return tt.ktt(385);}
"mean_squares_between" {return tt.ktt(386);}
"mean_squares_within" {return tt.ktt(387);}
"measure" {return tt.ktt(388);}
"measures" {return tt.ktt(389);}
"medium" {return tt.ktt(390);}
"member" {return tt.ktt(391);}
"memcompress" {return tt.ktt(392);}
"memoptimize" {return tt.ktt(393);}
"memory" {return tt.ktt(394);}
"memspeed" {return tt.ktt(395);}
"merge" {return tt.ktt(396);}
"metadata" {return tt.ktt(397);}
"minextents" {return tt.ktt(398);}
"minimize" {return tt.ktt(399);}
"mining" {return tt.ktt(400);}
"minus" {return tt.ktt(401);}
"minute" {return tt.ktt(402);}
"minutes" {return tt.ktt(403);}
"minvalue" {return tt.ktt(404);}
"mirror" {return tt.ktt(405);}
"mismatch" {return tt.ktt(406);}
"mlslabel" {return tt.ktt(407);}
"mode" {return tt.ktt(408);}
"model" {return tt.ktt(409);}
"modification" {return tt.ktt(410);}
"modify" {return tt.ktt(411);}
"monitoring" {return tt.ktt(412);}
"month" {return tt.ktt(413);}
"months" {return tt.ktt(414);}
"mount" {return tt.ktt(415);}
"move" {return tt.ktt(416);}
"movement" {return tt.ktt(417);}
"multiset" {return tt.ktt(418);}
"multivalue" {return tt.ktt(419);}
"name" {return tt.ktt(420);}
"nan" {return tt.ktt(421);}
"narrate" {return tt.ktt(422);}
"natural" {return tt.ktt(423);}
"nav" {return tt.ktt(424);}
"nchar_cs" {return tt.ktt(425);}
"nested" {return tt.ktt(426);}
"never" {return tt.ktt(427);}
"new" {return tt.ktt(428);}
"newline" {return tt.ktt(429);}
"next" {return tt.ktt(430);}
"nextval" {return tt.ktt(431);}
"no" {return tt.ktt(432);}
"noarchivelog" {return tt.ktt(433);}
"noaudit" {return tt.ktt(434);}
"nocache" {return tt.ktt(435);}
"nocheck" {return tt.ktt(436);}
"nocompress" {return tt.ktt(437);}
"nocycle" {return tt.ktt(438);}
"nodelay" {return tt.ktt(439);}
"nodelete" {return tt.ktt(440);}
"noentityescaping" {return tt.ktt(441);}
"noforce" {return tt.ktt(442);}
"noinsert" {return tt.ktt(443);}
"nologging" {return tt.ktt(444);}
"nomapping" {return tt.ktt(445);}
"nomaxvalue" {return tt.ktt(446);}
"nominimize" {return tt.ktt(447);}
"nominvalue" {return tt.ktt(448);}
"nomonitoring" {return tt.ktt(449);}
"none" {return tt.ktt(450);}
"noneditionable" {return tt.ktt(451);}
"nonpartitioned" {return tt.ktt(452);}
"nonschema" {return tt.ktt(453);}
"noorder" {return tt.ktt(454);}
"noparallel" {return tt.ktt(455);}
"normalize" {return tt.ktt(456);}
"norely" {return tt.ktt(457);}
"norepair" {return tt.ktt(458);}
"noresetlogs" {return tt.ktt(459);}
"noreverse" {return tt.ktt(460);}
"norowdependencies" {return tt.ktt(461);}
"noschema" {return tt.ktt(462);}
"noschemacheck" {return tt.ktt(463);}
"nosort" {return tt.ktt(464);}
"noswitch" {return tt.ktt(465);}
"not" {return tt.ktt(466);}
"nothing" {return tt.ktt(467);}
"notification" {return tt.ktt(468);}
"notimeout" {return tt.ktt(469);}
"noupdate" {return tt.ktt(470);}
"novalidate" {return tt.ktt(471);}
"nowait" {return tt.ktt(472);}
"null" {return tt.ktt(473);}
"nulls" {return tt.ktt(474);}
"object" {return tt.ktt(475);}
"of" {return tt.ktt(476);}
"off" {return tt.ktt(477);}
"offline" {return tt.ktt(478);}
"offset" {return tt.ktt(479);}
"oidindex" {return tt.ktt(480);}
"on" {return tt.ktt(481);}
"one_sided_prob_or_less" {return tt.ktt(482);}
"one_sided_prob_or_more" {return tt.ktt(483);}
"one_sided_sig" {return tt.ktt(484);}
"online" {return tt.ktt(485);}
"only" {return tt.ktt(486);}
"opaque" {return tt.ktt(487);}
"open" {return tt.ktt(488);}
"operator" {return tt.ktt(489);}
"optimal" {return tt.ktt(490);}
"optimize" {return tt.ktt(491);}
"option" {return tt.ktt(492);}
"or" {return tt.ktt(493);}
"order" {return tt.ktt(494);}
"ordered" {return tt.ktt(495);}
"ordinality" {return tt.ktt(496);}
"organization" {return tt.ktt(497);}
"outer" {return tt.ktt(498);}
"outline" {return tt.ktt(499);}
"over" {return tt.ktt(500);}
"overflow" {return tt.ktt(501);}
"overlaps" {return tt.ktt(502);}
"package" {return tt.ktt(503);}
"parallel" {return tt.ktt(504);}
"parameters" {return tt.ktt(505);}
"parent" {return tt.ktt(506);}
"partial" {return tt.ktt(507);}
"partition" {return tt.ktt(508);}
"partitions" {return tt.ktt(509);}
"partitionset" {return tt.ktt(510);}
"passing" {return tt.ktt(511);}
"password" {return tt.ktt(512);}
"path" {return tt.ktt(513);}
"pctfree" {return tt.ktt(514);}
"pctincrease" {return tt.ktt(515);}
"pctthreshold" {return tt.ktt(516);}
"pctused" {return tt.ktt(517);}
"pctversion" {return tt.ktt(518);}
"percent" {return tt.ktt(519);}
"performance" {return tt.ktt(520);}
"period" {return tt.ktt(521);}
"phi_coefficient" {return tt.ktt(522);}
"physical" {return tt.ktt(523);}
"pivot" {return tt.ktt(524);}
"plan" {return tt.ktt(525);}
"pluggable" {return tt.ktt(526);}
"policy" {return tt.ktt(527);}
"post_transaction" {return tt.ktt(528);}
"power" {return tt.ktt(529);}
"prebuilt" {return tt.ktt(530);}
"preceding" {return tt.ktt(531);}
"precision" {return tt.ktt(532);}
"prepare" {return tt.ktt(533);}
"present" {return tt.ktt(534);}
"preserve" {return tt.ktt(535);}
"pretty" {return tt.ktt(536);}
"primary" {return tt.ktt(537);}
"prior" {return tt.ktt(538);}
"priority" {return tt.ktt(539);}
"private" {return tt.ktt(540);}
"privilege" {return tt.ktt(541);}
"privileges" {return tt.ktt(542);}
"procedure" {return tt.ktt(543);}
"process" {return tt.ktt(544);}
"profile" {return tt.ktt(545);}
"program" {return tt.ktt(546);}
"project" {return tt.ktt(547);}
"protection" {return tt.ktt(548);}
"public" {return tt.ktt(549);}
"punctuation" {return tt.ktt(550);}
"purge" {return tt.ktt(551);}
"query" {return tt.ktt(552);}
"queue" {return tt.ktt(553);}
"quiesce" {return tt.ktt(554);}
"quota" {return tt.ktt(555);}
"range" {return tt.ktt(556);}
"read" {return tt.ktt(557);}
"reads" {return tt.ktt(558);}
"rebalance" {return tt.ktt(559);}
"rebuild" {return tt.ktt(560);}
"recover" {return tt.ktt(561);}
"recovery" {return tt.ktt(562);}
"recursively" {return tt.ktt(563);}
"recycle" {return tt.ktt(564);}
"redefine" {return tt.ktt(565);}
"reduced" {return tt.ktt(566);}
"ref" {return tt.ktt(567);}
"reference" {return tt.ktt(568);}
"referenced" {return tt.ktt(569);}
"references" {return tt.ktt(570);}
"refresh" {return tt.ktt(571);}
"regexp_like" {return tt.ktt(572);}
"register" {return tt.ktt(573);}
"reject" {return tt.ktt(574);}
"rekey" {return tt.ktt(575);}
"relational" {return tt.ktt(576);}
"rely" {return tt.ktt(577);}
"remainder" {return tt.ktt(578);}
"rename" {return tt.ktt(579);}
"repair" {return tt.ktt(580);}
"repeat" {return tt.ktt(581);}
"replace" {return tt.ktt(582);}
"replication" {return tt.ktt(583);}
"reservable" {return tt.ktt(584);}
"reset" {return tt.ktt(585);}
"resetlogs" {return tt.ktt(586);}
"resize" {return tt.ktt(587);}
"resolve" {return tt.ktt(588);}
"resolver" {return tt.ktt(589);}
"resource" {return tt.ktt(590);}
"restrict" {return tt.ktt(591);}
"restricted" {return tt.ktt(592);}
"result_cache" {return tt.ktt(593);}
"resumable" {return tt.ktt(594);}
"resume" {return tt.ktt(595);}
"retention" {return tt.ktt(596);}
"return" {return tt.ktt(597);}
"returning" {return tt.ktt(598);}
"reuse" {return tt.ktt(599);}
"reverse" {return tt.ktt(600);}
"revoke" {return tt.ktt(601);}
"rewrite" {return tt.ktt(602);}
"right" {return tt.ktt(603);}
"role" {return tt.ktt(604);}
"rollback" {return tt.ktt(605);}
"rollover" {return tt.ktt(606);}
"rollup" {return tt.ktt(607);}
"row" {return tt.ktt(608);}
"rowdependencies" {return tt.ktt(609);}
"rownum" {return tt.ktt(610);}
"rows" {return tt.ktt(611);}
"rule" {return tt.ktt(612);}
"rules" {return tt.ktt(613);}
"salt" {return tt.ktt(614);}
"sample" {return tt.ktt(615);}
"savepoint" {return tt.ktt(616);}
"scan" {return tt.ktt(617);}
"scheduler" {return tt.ktt(618);}
"schemacheck" {return tt.ktt(619);}
"scn" {return tt.ktt(620);}
"scope" {return tt.ktt(621);}
"second" {return tt.ktt(622);}
"securefile" {return tt.ktt(623);}
"seed" {return tt.ktt(624);}
"segment" {return tt.ktt(625);}
"select" {return tt.ktt(626);}
"sentence" {return tt.ktt(627);}
"sequence" {return tt.ktt(628);}
"sequential" {return tt.ktt(629);}
"serializable" {return tt.ktt(630);}
"service" {return tt.ktt(631);}
"session" {return tt.ktt(632);}
"set" {return tt.ktt(633);}
"sets" {return tt.ktt(634);}
"settings" {return tt.ktt(635);}
"share" {return tt.ktt(636);}
"shared" {return tt.ktt(637);}
"shared_pool" {return tt.ktt(638);}
"sharing" {return tt.ktt(639);}
"show" {return tt.ktt(640);}
"showprompt" {return tt.ktt(641);}
"showsql" {return tt.ktt(642);}
"shrink" {return tt.ktt(643);}
"shutdown" {return tt.ktt(644);}
"siblings" {return tt.ktt(645);}
"sid" {return tt.ktt(646);}
"sig" {return tt.ktt(647);}
"single" {return tt.ktt(648);}
"size" {return tt.ktt(649);}
"skip" {return tt.ktt(650);}
"smallfile" {return tt.ktt(651);}
"snapshot" {return tt.ktt(652);}
"some" {return tt.ktt(653);}
"sort" {return tt.ktt(654);}
"source" {return tt.ktt(655);}
"space" {return tt.ktt(656);}
"sparse" {return tt.ktt(657);}
"spatial" {return tt.ktt(658);}
"specification" {return tt.ktt(659);}
"spfile" {return tt.ktt(660);}
"split" {return tt.ktt(661);}
"sql" {return tt.ktt(662);}
"staging" {return tt.ktt(663);}
"standalone" {return tt.ktt(664);}
"standby" {return tt.ktt(665);}
"start" {return tt.ktt(666);}
"statement" {return tt.ktt(667);}
"statistic" {return tt.ktt(668);}
"statistics" {return tt.ktt(669);}
"stop" {return tt.ktt(670);}
"storage" {return tt.ktt(671);}
"store" {return tt.ktt(672);}
"strict" {return tt.ktt(673);}
"submultiset" {return tt.ktt(674);}
"subpartition" {return tt.ktt(675);}
"subpartitions" {return tt.ktt(676);}
"substitutable" {return tt.ktt(677);}
"successful" {return tt.ktt(678);}
"sum_squares_between" {return tt.ktt(679);}
"sum_squares_within" {return tt.ktt(680);}
"supplemental" {return tt.ktt(681);}
"suspend" {return tt.ktt(682);}
"switch" {return tt.ktt(683);}
"switchover" {return tt.ktt(684);}
"synchronous" {return tt.ktt(685);}
"synonym" {return tt.ktt(686);}
"sysbackup" {return tt.ktt(687);}
"sysdba" {return tt.ktt(688);}
"sysdg" {return tt.ktt(689);}
"syskm" {return tt.ktt(690);}
"sysoper" {return tt.ktt(691);}
"system" {return tt.ktt(692);}
"table" {return tt.ktt(693);}
"tables" {return tt.ktt(694);}
"tablespace" {return tt.ktt(695);}
"tempfile" {return tt.ktt(696);}
"template" {return tt.ktt(697);}
"temporary" {return tt.ktt(698);}
"test" {return tt.ktt(699);}
"than" {return tt.ktt(700);}
"then" {return tt.ktt(701);}
"thread" {return tt.ktt(702);}
"through" {return tt.ktt(703);}
"tier" {return tt.ktt(704);}
"ties" {return tt.ktt(705);}
"time" {return tt.ktt(706);}
"time_zone" {return tt.ktt(707);}
"timeout" {return tt.ktt(708);}
"timezone_abbr" {return tt.ktt(709);}
"timezone_hour" {return tt.ktt(710);}
"timezone_minute" {return tt.ktt(711);}
"timezone_region" {return tt.ktt(712);}
"to" {return tt.ktt(713);}
"trace" {return tt.ktt(714);}
"tracking" {return tt.ktt(715);}
"trailing" {return tt.ktt(716);}
"transaction" {return tt.ktt(717);}
"translation" {return tt.ktt(718);}
"trigger" {return tt.ktt(719);}
"triggers" {return tt.ktt(720);}
"truncate" {return tt.ktt(721);}
"trusted" {return tt.ktt(722);}
"tuning" {return tt.ktt(723);}
"two_sided_prob" {return tt.ktt(724);}
"two_sided_sig" {return tt.ktt(725);}
"type" {return tt.ktt(726);}
"u_statistic" {return tt.ktt(727);}
"uid" {return tt.ktt(728);}
"unarchived" {return tt.ktt(729);}
"unbounded" {return tt.ktt(730);}
"unconditional" {return tt.ktt(731);}
"under" {return tt.ktt(732);}
"under_path" {return tt.ktt(733);}
"undrop" {return tt.ktt(734);}
"union" {return tt.ktt(735);}
"unique" {return tt.ktt(736);}
"unlimited" {return tt.ktt(737);}
"unlock" {return tt.ktt(738);}
"unnest" {return tt.ktt(739);}
"unpacked" {return tt.ktt(740);}
"unpivot" {return tt.ktt(741);}
"unprotected" {return tt.ktt(742);}
"unquiesce" {return tt.ktt(743);}
"unrecoverable" {return tt.ktt(744);}
"until" {return tt.ktt(745);}
"unusable" {return tt.ktt(746);}
"unused" {return tt.ktt(747);}
"update" {return tt.ktt(748);}
"updated" {return tt.ktt(749);}
"upgrade" {return tt.ktt(750);}
"upsert" {return tt.ktt(751);}
"usage" {return tt.ktt(752);}
"use" {return tt.ktt(753);}
"user" {return tt.ktt(754);}
"using" {return tt.ktt(755);}
"validate" {return tt.ktt(756);}
"validation" {return tt.ktt(757);}
"value" {return tt.ktt(758);}
"values" {return tt.ktt(759);}
"variants" {return tt.ktt(760);}
"varray" {return tt.ktt(761);}
"varrays" {return tt.ktt(762);}
"version" {return tt.ktt(763);}
"versions" {return tt.ktt(764);}
"widechar" {return tt.ktt(765);}
"view" {return tt.ktt(766);}
"virtual" {return tt.ktt(767);}
"visible" {return tt.ktt(768);}
"vocabulary" {return tt.ktt(769);}
"wait" {return tt.ktt(770);}
"wellformed" {return tt.ktt(771);}
"when" {return tt.ktt(772);}
"whenever" {return tt.ktt(773);}
"where" {return tt.ktt(774);}
"whitespace" {return tt.ktt(775);}
"with" {return tt.ktt(776);}
"within" {return tt.ktt(777);}
"without" {return tt.ktt(778);}
"words" {return tt.ktt(779);}
"work" {return tt.ktt(780);}
"wrapper" {return tt.ktt(781);}
"write" {return tt.ktt(782);}
"xdb" {return tt.ktt(783);}
"xml" {return tt.ktt(784);}
"xmlindex" {return tt.ktt(785);}
"xmlnamespaces" {return tt.ktt(786);}
"xmlschema" {return tt.ktt(787);}
"xmltype" {return tt.ktt(788);}
"year" {return tt.ktt(789);}
"years" {return tt.ktt(790);}
"yes" {return tt.ktt(791);}
"zone" {return tt.ktt(792);}
"zonemap" {return tt.ktt(793);}
"false" {return tt.ktt(794);}
"true" {return tt.ktt(795);}




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
"collect" {return tt.ftt(18);}
"compose" {return tt.ftt(19);}
"concat" {return tt.ftt(20);}
"convert" {return tt.ftt(21);}
"corr" {return tt.ftt(22);}
"corr_k" {return tt.ftt(23);}
"corr_s" {return tt.ftt(24);}
"cos" {return tt.ftt(25);}
"cosh" {return tt.ftt(26);}
"cosine_distance" {return tt.ftt(27);}
"count" {return tt.ftt(28);}
"covar_pop" {return tt.ftt(29);}
"covar_samp" {return tt.ftt(30);}
"cume_dist" {return tt.ftt(31);}
"current_date" {return tt.ftt(32);}
"current_timestamp" {return tt.ftt(33);}
"cv" {return tt.ftt(34);}
"dbtimezone" {return tt.ftt(35);}
"dbtmezone" {return tt.ftt(36);}
"decode" {return tt.ftt(37);}
"decompose" {return tt.ftt(38);}
"deletexml" {return tt.ftt(39);}
"depth" {return tt.ftt(40);}
"deref" {return tt.ftt(41);}
"empty_blob" {return tt.ftt(42);}
"empty_clob" {return tt.ftt(43);}
"existsnode" {return tt.ftt(44);}
"exp" {return tt.ftt(45);}
"extract" {return tt.ftt(46);}
"extractvalue" {return tt.ftt(47);}
"first_value" {return tt.ftt(48);}
"floor" {return tt.ftt(49);}
"from_tz" {return tt.ftt(50);}
"from_vector" {return tt.ftt(51);}
"greatest" {return tt.ftt(52);}
"group_id" {return tt.ftt(53);}
"grouping" {return tt.ftt(54);}
"grouping_id" {return tt.ftt(55);}
"hextoraw" {return tt.ftt(56);}
"initcap" {return tt.ftt(57);}
"inner_product" {return tt.ftt(58);}
"insertchildxml" {return tt.ftt(59);}
"insertchildxmlafter" {return tt.ftt(60);}
"insertchildxmlbefore" {return tt.ftt(61);}
"insertxmlafter" {return tt.ftt(62);}
"insertxmlbefore" {return tt.ftt(63);}
"instr" {return tt.ftt(64);}
"instr2" {return tt.ftt(65);}
"instr4" {return tt.ftt(66);}
"instrb" {return tt.ftt(67);}
"instrc" {return tt.ftt(68);}
"iteration_number" {return tt.ftt(69);}
"json_array" {return tt.ftt(70);}
"json_arrayagg" {return tt.ftt(71);}
"json_dataguide" {return tt.ftt(72);}
"json_object" {return tt.ftt(73);}
"json_objectagg" {return tt.ftt(74);}
"json_query" {return tt.ftt(75);}
"json_serialize" {return tt.ftt(76);}
"json_table" {return tt.ftt(77);}
"json_value" {return tt.ftt(78);}
"l1_distance" {return tt.ftt(79);}
"l2_distance" {return tt.ftt(80);}
"lag" {return tt.ftt(81);}
"last_day" {return tt.ftt(82);}
"last_value" {return tt.ftt(83);}
"lateral" {return tt.ftt(84);}
"lead" {return tt.ftt(85);}
"least" {return tt.ftt(86);}
"length" {return tt.ftt(87);}
"length2" {return tt.ftt(88);}
"length4" {return tt.ftt(89);}
"lengthb" {return tt.ftt(90);}
"lengthc" {return tt.ftt(91);}
"listagg" {return tt.ftt(92);}
"ln" {return tt.ftt(93);}
"lnnvl" {return tt.ftt(94);}
"localtimestamp" {return tt.ftt(95);}
"lower" {return tt.ftt(96);}
"lpad" {return tt.ftt(97);}
"ltrim" {return tt.ftt(98);}
"make_ref" {return tt.ftt(99);}
"max" {return tt.ftt(100);}
"median" {return tt.ftt(101);}
"min" {return tt.ftt(102);}
"mod" {return tt.ftt(103);}
"months_between" {return tt.ftt(104);}
"nanvl" {return tt.ftt(105);}
"nchr" {return tt.ftt(106);}
"new_time" {return tt.ftt(107);}
"next_day" {return tt.ftt(108);}
"nls_charset_decl_len" {return tt.ftt(109);}
"nls_charset_id" {return tt.ftt(110);}
"nls_charset_name" {return tt.ftt(111);}
"nls_initcap" {return tt.ftt(112);}
"nls_lower" {return tt.ftt(113);}
"nls_upper" {return tt.ftt(114);}
"nlssort" {return tt.ftt(115);}
"ntile" {return tt.ftt(116);}
"nullif" {return tt.ftt(117);}
"numtodsinterval" {return tt.ftt(118);}
"numtoyminterval" {return tt.ftt(119);}
"nvl" {return tt.ftt(120);}
"nvl2" {return tt.ftt(121);}
"ora_hash" {return tt.ftt(122);}
"percent_rank" {return tt.ftt(123);}
"percentile_cont" {return tt.ftt(124);}
"percentile_disc" {return tt.ftt(125);}
"powermultiset" {return tt.ftt(126);}
"powermultiset_by_cardinality" {return tt.ftt(127);}
"presentnnv" {return tt.ftt(128);}
"presentv" {return tt.ftt(129);}
"previous" {return tt.ftt(130);}
"rank" {return tt.ftt(131);}
"ratio_to_report" {return tt.ftt(132);}
"rawtohex" {return tt.ftt(133);}
"rawtonhex" {return tt.ftt(134);}
"reftohex" {return tt.ftt(135);}
"regexp_instr" {return tt.ftt(136);}
"regexp_replace" {return tt.ftt(137);}
"regexp_substr" {return tt.ftt(138);}
"regr_avgx" {return tt.ftt(139);}
"regr_avgy" {return tt.ftt(140);}
"regr_count" {return tt.ftt(141);}
"regr_intercept" {return tt.ftt(142);}
"regr_r2" {return tt.ftt(143);}
"regr_slope" {return tt.ftt(144);}
"regr_sxx" {return tt.ftt(145);}
"regr_sxy" {return tt.ftt(146);}
"regr_syy" {return tt.ftt(147);}
"round" {return tt.ftt(148);}
"row_number" {return tt.ftt(149);}
"rowidtochar" {return tt.ftt(150);}
"rowidtonchar" {return tt.ftt(151);}
"rpad" {return tt.ftt(152);}
"rtrim" {return tt.ftt(153);}
"scn_to_timestamp" {return tt.ftt(154);}
"sessiontimezone" {return tt.ftt(155);}
"sign" {return tt.ftt(156);}
"sin" {return tt.ftt(157);}
"sinh" {return tt.ftt(158);}
"soundex" {return tt.ftt(159);}
"sqrt" {return tt.ftt(160);}
"stats_binomial_test" {return tt.ftt(161);}
"stats_crosstab" {return tt.ftt(162);}
"stats_f_test" {return tt.ftt(163);}
"stats_ks_test" {return tt.ftt(164);}
"stats_mode" {return tt.ftt(165);}
"stats_mw_test" {return tt.ftt(166);}
"stats_one_way_anova" {return tt.ftt(167);}
"stats_t_test_indep" {return tt.ftt(168);}
"stats_t_test_indepu" {return tt.ftt(169);}
"stats_t_test_one" {return tt.ftt(170);}
"stats_t_test_paired" {return tt.ftt(171);}
"stats_wsr_test" {return tt.ftt(172);}
"stddev" {return tt.ftt(173);}
"stddev_pop" {return tt.ftt(174);}
"stddev_samp" {return tt.ftt(175);}
"substr" {return tt.ftt(176);}
"substr2" {return tt.ftt(177);}
"substr4" {return tt.ftt(178);}
"substrb" {return tt.ftt(179);}
"substrc" {return tt.ftt(180);}
"sum" {return tt.ftt(181);}
"sys_connect_by_path" {return tt.ftt(182);}
"sys_context" {return tt.ftt(183);}
"sys_dburigen" {return tt.ftt(184);}
"sys_extract_utc" {return tt.ftt(185);}
"sys_guid" {return tt.ftt(186);}
"sys_typeid" {return tt.ftt(187);}
"sys_xmlagg" {return tt.ftt(188);}
"sys_xmlgen" {return tt.ftt(189);}
"sysdate" {return tt.ftt(190);}
"systimestamp" {return tt.ftt(191);}
"tan" {return tt.ftt(192);}
"tanh" {return tt.ftt(193);}
"timestamp_to_scn" {return tt.ftt(194);}
"to_binary_double" {return tt.ftt(195);}
"to_binary_float" {return tt.ftt(196);}
"to_char" {return tt.ftt(197);}
"to_clob" {return tt.ftt(198);}
"to_date" {return tt.ftt(199);}
"to_dsinterval" {return tt.ftt(200);}
"to_lob" {return tt.ftt(201);}
"to_multi_byte" {return tt.ftt(202);}
"to_nchar" {return tt.ftt(203);}
"to_nclob" {return tt.ftt(204);}
"to_number" {return tt.ftt(205);}
"to_single_byte" {return tt.ftt(206);}
"to_timestamp" {return tt.ftt(207);}
"to_timestamp_tz" {return tt.ftt(208);}
"to_vector" {return tt.ftt(209);}
"to_yminterval" {return tt.ftt(210);}
"translate" {return tt.ftt(211);}
"treat" {return tt.ftt(212);}
"trim" {return tt.ftt(213);}
"trunc" {return tt.ftt(214);}
"tz_offset" {return tt.ftt(215);}
"unistr" {return tt.ftt(216);}
"updatexml" {return tt.ftt(217);}
"upper" {return tt.ftt(218);}
"userenv" {return tt.ftt(219);}
"validate_conversion" {return tt.ftt(220);}
"var_pop" {return tt.ftt(221);}
"var_samp" {return tt.ftt(222);}
"variance" {return tt.ftt(223);}
"vector_chunks" {return tt.ftt(224);}
"vector_dims" {return tt.ftt(225);}
"vector_dimension_count" {return tt.ftt(226);}
"vector_dimension_format" {return tt.ftt(227);}
"vector_distance" {return tt.ftt(228);}
"vector_embedding" {return tt.ftt(229);}
"vector_norm" {return tt.ftt(230);}
"vector_serialize" {return tt.ftt(231);}
"vsize" {return tt.ftt(232);}
"width_bucket" {return tt.ftt(233);}
"xmlagg" {return tt.ftt(234);}
"xmlattributes" {return tt.ftt(235);}
"xmlcast" {return tt.ftt(236);}
"xmlcdata" {return tt.ftt(237);}
"xmlcolattval" {return tt.ftt(238);}
"xmlcomment" {return tt.ftt(239);}
"xmlconcat" {return tt.ftt(240);}
"xmldiff" {return tt.ftt(241);}
"xmlelement" {return tt.ftt(242);}
"xmlforest" {return tt.ftt(243);}
"xmlisvalid" {return tt.ftt(244);}
"xmlparse" {return tt.ftt(245);}
"xmlpatch" {return tt.ftt(246);}
"xmlpi" {return tt.ftt(247);}
"xmlquery" {return tt.ftt(248);}
"xmlroot" {return tt.ftt(249);}
"xmlsequence" {return tt.ftt(250);}
"xmlserialize" {return tt.ftt(251);}
"xmltable" {return tt.ftt(252);}
"xmltransform" {return tt.ftt(253);}




"aq_tm_processes" {return tt.ptt(0);}
"archive_lag_target" {return tt.ptt(1);}
"audit_file_dest" {return tt.ptt(2);}
"audit_sys_operations" {return tt.ptt(3);}
"audit_trail" {return tt.ptt(4);}
"background_core_dump" {return tt.ptt(5);}
"background_dump_dest" {return tt.ptt(6);}
"backup_tape_io_slaves" {return tt.ptt(7);}
"bitmap_merge_area_size" {return tt.ptt(8);}
"blank_trimming" {return tt.ptt(9);}
"circuits" {return tt.ptt(10);}
"cluster_database" {return tt.ptt(11);}
"cluster_database_instances" {return tt.ptt(12);}
"cluster_interconnects" {return tt.ptt(13);}
"commit_point_strength" {return tt.ptt(14);}
"compatible" {return tt.ptt(15);}
"composite_limit" {return tt.ptt(16);}
"connect_time" {return tt.ptt(17);}
"control_file_record_keep_time" {return tt.ptt(18);}
"control_files" {return tt.ptt(19);}
"core_dump_dest"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(20);}
"cpu_count" {return tt.ptt(21);}
"cpu_per_call" {return tt.ptt(22);}
"cpu_per_session" {return tt.ptt(23);}
"create_bitmap_area_size" {return tt.ptt(24);}
"create_stored_outlines" {return tt.ptt(25);}
"current_schema" {return tt.ptt(26);}
"cursor_sharing" {return tt.ptt(27);}
"cursor_space_for_time" {return tt.ptt(28);}
"db_block_checking" {return tt.ptt(29);}
"db_block_checksum" {return tt.ptt(30);}
"db_block_size" {return tt.ptt(31);}
"db_cache_advice" {return tt.ptt(32);}
"db_cache_size" {return tt.ptt(33);}
"db_create_file_dest" {return tt.ptt(34);}
"db_create_online_log_dest_"{digit}+ {return tt.ptt(35);}
"db_domain" {return tt.ptt(36);}
"db_file_multiblock_read_count" {return tt.ptt(37);}
"db_file_name_convert" {return tt.ptt(38);}
"db_files" {return tt.ptt(39);}
"db_flashback_retention_target" {return tt.ptt(40);}
"db_keep_cache_size" {return tt.ptt(41);}
"db_name" {return tt.ptt(42);}
"db_nk_cache_size" {return tt.ptt(43);}
"db_recovery_file_dest" {return tt.ptt(44);}
"db_recovery_file_dest_size" {return tt.ptt(45);}
"db_recycle_cache_size" {return tt.ptt(46);}
"db_unique_name" {return tt.ptt(47);}
"db_writer_processes" {return tt.ptt(48);}
"dbwr_io_slaves" {return tt.ptt(49);}
"ddl_wait_for_locks" {return tt.ptt(50);}
"dg_broker_config_filen" {return tt.ptt(51);}
"dg_broker_start" {return tt.ptt(52);}
"disk_asynch_io" {return tt.ptt(53);}
"dispatchers" {return tt.ptt(54);}
"distributed_lock_timeout" {return tt.ptt(55);}
"dml_locks" {return tt.ptt(56);}
"enqueue_resources" {return tt.ptt(57);}
"error_on_overlap_time" {return tt.ptt(58);}
"event" {return tt.ptt(59);}
"failed_login_attempts" {return tt.ptt(60);}
"fal_client" {return tt.ptt(61);}
"fal_server" {return tt.ptt(62);}
"fast_start_mttr_target" {return tt.ptt(63);}
"fast_start_parallel_rollback" {return tt.ptt(64);}
"file_mapping" {return tt.ptt(65);}
"fileio_network_adapters" {return tt.ptt(66);}
"filesystem_like_logging" {return tt.ptt(67);}
"filesystemio_options" {return tt.ptt(68);}
"fixed_date" {return tt.ptt(69);}
"flagger" {return tt.ptt(70);}
"gc_files_to_locks" {return tt.ptt(71);}
"gcs_server_processes" {return tt.ptt(72);}
"global_names" {return tt.ptt(73);}
"hash_area_size" {return tt.ptt(74);}
"hi_shared_memory_address" {return tt.ptt(75);}
"hs_autoregister" {return tt.ptt(76);}
"idle_time" {return tt.ptt(77);}
"ifile" {return tt.ptt(78);}
"instance"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(79);}
"instance_groups" {return tt.ptt(80);}
"instance_name" {return tt.ptt(81);}
"instance_number" {return tt.ptt(82);}
"instance_type" {return tt.ptt(83);}
"isolation_level" {return tt.ptt(84);}
"java_max_sessionspace_size" {return tt.ptt(85);}
"java_pool_size" {return tt.ptt(86);}
"java_soft_sessionspace_limit" {return tt.ptt(87);}
"job_queue_processes" {return tt.ptt(88);}
"large_pool_size" {return tt.ptt(89);}
"ldap_directory_access" {return tt.ptt(90);}
"license_max_sessions" {return tt.ptt(91);}
"license_max_users" {return tt.ptt(92);}
"license_sessions_warning" {return tt.ptt(93);}
"local_listener" {return tt.ptt(94);}
"lock_sga" {return tt.ptt(95);}
"log_archive_config" {return tt.ptt(96);}
"log_archive_dest" {return tt.ptt(97);}
"log_archive_dest_"{digit}+ {return tt.ptt(98);}
"log_archive_dest_state_"{digit}+ {return tt.ptt(99);}
"log_archive_duplex_dest" {return tt.ptt(100);}
"log_archive_format" {return tt.ptt(101);}
"log_archive_local_first" {return tt.ptt(102);}
"log_archive_max_processes" {return tt.ptt(103);}
"log_archive_min_succeed_dest" {return tt.ptt(104);}
"log_archive_trace" {return tt.ptt(105);}
"log_buffer" {return tt.ptt(106);}
"log_checkpoint_interval" {return tt.ptt(107);}
"log_checkpoint_timeout" {return tt.ptt(108);}
"log_checkpoints_to_alert" {return tt.ptt(109);}
"log_file_name_convert" {return tt.ptt(110);}
"logical_reads_per_call" {return tt.ptt(111);}
"logical_reads_per_session" {return tt.ptt(112);}
"logmnr_max_persistent_sessions" {return tt.ptt(113);}
"max_commit_propagation_delay" {return tt.ptt(114);}
"max_dispatchers" {return tt.ptt(115);}
"max_dump_file_size" {return tt.ptt(116);}
"max_shared_servers" {return tt.ptt(117);}
"nls_calendar" {return tt.ptt(118);}
"nls_comp" {return tt.ptt(119);}
"nls_currency" {return tt.ptt(120);}
"nls_date_format" {return tt.ptt(121);}
"nls_date_language" {return tt.ptt(122);}
"nls_dual_currency" {return tt.ptt(123);}
"nls_iso_currency" {return tt.ptt(124);}
"nls_language" {return tt.ptt(125);}
"nls_length_semantics" {return tt.ptt(126);}
"nls_nchar_conv_excp" {return tt.ptt(127);}
"nls_numeric_characters" {return tt.ptt(128);}
"nls_sort" {return tt.ptt(129);}
"nls_territory" {return tt.ptt(130);}
"nls_timestamp_format" {return tt.ptt(131);}
"nls_timestamp_tz_format" {return tt.ptt(132);}
"o7_dictionary_accessibility" {return tt.ptt(133);}
"object_cache_max_size_percent" {return tt.ptt(134);}
"object_cache_optimal_size" {return tt.ptt(135);}
"olap_page_pool_size" {return tt.ptt(136);}
"open_cursors" {return tt.ptt(137);}
"open_links" {return tt.ptt(138);}
"open_links_per_instance" {return tt.ptt(139);}
"optimizer_dynamic_sampling" {return tt.ptt(140);}
"optimizer_features_enable" {return tt.ptt(141);}
"optimizer_index_caching" {return tt.ptt(142);}
"optimizer_index_cost_adj" {return tt.ptt(143);}
"optimizer_mode" {return tt.ptt(144);}
"os_authent_prefix" {return tt.ptt(145);}
"os_roles" {return tt.ptt(146);}
"osm_diskgroups" {return tt.ptt(147);}
"osm_diskstring" {return tt.ptt(148);}
"osm_power_limit" {return tt.ptt(149);}
"parallel_adaptive_multi_user" {return tt.ptt(150);}
"parallel_execution_message_size" {return tt.ptt(151);}
"parallel_instance_group" {return tt.ptt(152);}
"parallel_max_servers" {return tt.ptt(153);}
"parallel_min_percent" {return tt.ptt(154);}
"parallel_min_servers" {return tt.ptt(155);}
"parallel_threads_per_cpu" {return tt.ptt(156);}
"password_grace_time" {return tt.ptt(157);}
"password_life_time" {return tt.ptt(158);}
"password_lock_time" {return tt.ptt(159);}
"password_reuse_max" {return tt.ptt(160);}
"password_reuse_time" {return tt.ptt(161);}
"password_verify_function" {return tt.ptt(162);}
"pga_aggregate_target" {return tt.ptt(163);}
"plsql_code_type" {return tt.ptt(164);}
"plsql_compiler_flags" {return tt.ptt(165);}
"plsql_debug" {return tt.ptt(166);}
"plsql_native_library_dir" {return tt.ptt(167);}
"plsql_native_library_subdir_count" {return tt.ptt(168);}
"plsql_optimize_level" {return tt.ptt(169);}
"plsql_v2_compatibility" {return tt.ptt(170);}
"plsql_warnings" {return tt.ptt(171);}
"pre_page_sga" {return tt.ptt(172);}
"private_sga" {return tt.ptt(173);}
"processes" {return tt.ptt(174);}
"query_rewrite_enabled" {return tt.ptt(175);}
"query_rewrite_integrity" {return tt.ptt(176);}
"rdbms_server_dn" {return tt.ptt(177);}
"read_only_open_delayed" {return tt.ptt(178);}
"recovery_parallelism" {return tt.ptt(179);}
"remote_archive_enable" {return tt.ptt(180);}
"remote_dependencies_mode" {return tt.ptt(181);}
"remote_listener" {return tt.ptt(182);}
"remote_login_passwordfile" {return tt.ptt(183);}
"remote_os_authent" {return tt.ptt(184);}
"remote_os_roles" {return tt.ptt(185);}
"replication_dependency_tracking" {return tt.ptt(186);}
"resource_limit" {return tt.ptt(187);}
"resource_manager_plan" {return tt.ptt(188);}
"resumable_timeout" {return tt.ptt(189);}
"rollback_segments" {return tt.ptt(190);}
"serial_reuse" {return tt.ptt(191);}
"service_names" {return tt.ptt(192);}
"session_cached_cursors" {return tt.ptt(193);}
"session_max_open_files" {return tt.ptt(194);}
"sessions" {return tt.ptt(195);}
"sessions_per_user" {return tt.ptt(196);}
"sga_max_size" {return tt.ptt(197);}
"sga_target" {return tt.ptt(198);}
"shadow_core_dump" {return tt.ptt(199);}
"shared_memory_address" {return tt.ptt(200);}
"shared_pool_reserved_size" {return tt.ptt(201);}
"shared_pool_size" {return tt.ptt(202);}
"shared_server_sessions" {return tt.ptt(203);}
"shared_servers" {return tt.ptt(204);}
"skip_unusable_indexes" {return tt.ptt(205);}
"smtp_out_server" {return tt.ptt(206);}
"sort_area_retained_size" {return tt.ptt(207);}
"sort_area_size" {return tt.ptt(208);}
"spfile"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(209);}
"sql_trace" {return tt.ptt(210);}
"sql92_security" {return tt.ptt(211);}
"sqltune_category" {return tt.ptt(212);}
"standby_archive_dest" {return tt.ptt(213);}
"standby_file_management" {return tt.ptt(214);}
"star_transformation_enabled" {return tt.ptt(215);}
"statement_id" {return tt.ptt(216);}
"statistics_level" {return tt.ptt(217);}
"streams_pool_size" {return tt.ptt(218);}
"tape_asynch_io" {return tt.ptt(219);}
"thread"{wso}"=" { yybegin(YYINITIAL); yypushback(1); return tt.ptt(220);}
"timed_os_statistics" {return tt.ptt(221);}
"timed_statistics" {return tt.ptt(222);}
"trace_enabled" {return tt.ptt(223);}
"tracefile_identifier" {return tt.ptt(224);}
"transactions" {return tt.ptt(225);}
"transactions_per_rollback_segment" {return tt.ptt(226);}
"undo_management" {return tt.ptt(227);}
"undo_retention" {return tt.ptt(228);}
"undo_tablespace" {return tt.ptt(229);}
"use_indirect_data_buffers" {return tt.ptt(230);}
"use_private_outlines" {return tt.ptt(231);}
"use_stored_outlines" {return tt.ptt(232);}
"user_dump_dest" {return tt.ptt(233);}
"utl_file_dir" {return tt.ptt(234);}
"workarea_size_policy" {return tt.ptt(235);}


{CT_SIZE_CLAUSE} {return tt.getTokenType("CT_SIZE_CLAUSE");}

{INTEGER}     { return stt.getInteger(); }
{NUMBER}      { return stt.getNumber(); }
{STRING}      { return stt.getString(); }

{IDENTIFIER}         { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER}  { return stt.getQuotedIdentifier(); }

{WHITE_SPACE}        { return stt.getWhiteSpace(); }
.                    { return stt.getIdentifier(); }
}
