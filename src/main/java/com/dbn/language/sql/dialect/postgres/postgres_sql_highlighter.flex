package com.dbn.language.sql.dialect.postgres;

import com.dbn.language.common.TokenTypeBundle;
import com.dbn.language.common.lexer.DBLanguageLexerBase;
import com.intellij.psi.tree.IElementType;

%%

%class PostgresSQLHighlighterFlexLexer
%extends DBLanguageLexerBase
%public
%final
%unicode
%ignorecase
%function advance
%type IElementType
%eof{ return;
%eof}


%{
    public PostgresSQLHighlighterFlexLexer(TokenTypeBundle tt) {
        super(tt);
    }
%}
%include ../../../common/lexer/shared_elements.flext

IDENTIFIER = [:jletter:] [:jletterdigit:]*
QUOTED_IDENTIFIER = "\""[^\"]*"\""?

CHARSET ="armscii8"|"ascii"|"big5"|"binary"|"cp1250"|"cp1251"|"cp1256"|"cp1257"|"cp850"|"cp852"|"cp866"|"cp932"|"dec8"|"eucjpms"|"euckr"|"gb2312"|"gbk"|"geostd8"|"greek"|"hebrew"|"hp8"|"keybcs2"|"koi8r"|"koi8u"|"latin1"|"latin2"|"latin5"|"latin7"|"macce"|"macroman"|"sjis"|"swe7"|"tis620"|"ucs2"|"ujis"|"utf8"

string_simple_quoted      = "'"([^\']|"''"|{WHITE_SPACE})*"'"?
STRING = ("n"|"_"{CHARSET})?{wso}{string_simple_quoted}

VARIABLE = ":"{wso}({IDENTIFIER}|{INTEGER})

operator_equal               = "="
operator_not_equal           = (("!"|"^"){wso}"=")|("<"{wso}">")
operator_greater_than        = ">"
operator_greater_equal_than  = ">"{wso}"="
operator_less_than           = "<"
operator_less_equal_than     = ">"{wso}"="
operator_assignment          = ":="
operator_concatenation       = "||"
operator_like                = "~~"
operator_not_like            = "!~~"
operator_match               = "~"
operator_match_ci            = "~*"
operator_no_match            = "!~"
operator_no_match_ci         = "!~*"
operator_cast                = "::"
OPERATOR  = {operator_equal}|{operator_not_equal}|{operator_greater_than}|{operator_greater_equal_than}|{operator_less_than}|{operator_less_equal_than}|{operator_cast}|{operator_assignment}|{operator_concatenation}|{operator_like}|{operator_not_like}|{operator_match}|{operator_match_ci}|{operator_no_match}|{operator_no_match_ci}


SQL_KEYWORD   = "a"|"abort"|"absent"|"absolute"|"access"|"according"|"action"|"ada"|"add"|"admin"|"after"|"aggregate"|"all"|"allocate"|"also"|"alter"|"always"|"analyse"|"analyze"|"and"|"any"|"are"|"array_max_cardinality"|"as"|"asc"|"asensitive"|"assertion"|"assignment"|"asymmetric"|"at"|"atomic"|"attribute"|"attributes"|"authorization"|"backward"|"base64"|"before"|"begin"|"begin_frame"|"begin_partition"|"bernoulli"|"between"|"binary"|"blob"|"blocked"|"bom"|"both"|"breadth"|"buffers"|"by"|"bypassrls"|"c"|"cache"|"call"|"called"|"cardinality"|"cascade"|"cascaded"|"case"|"cast"|"catalog"|"catalog_name"|"chain"|"characteristics"|"characters"|"character_length"|"character_set_catalog"|"character_set_name"|"character_set_schema"|"check"|"checkpoint"|"class"|"class_origin"|"clob"|"close"|"cluster"|"coalesce"|"cobol"|"collate"|"collation"|"collation_catalog"|"collation_name"|"collation_schema"|"collect"|"column"|"columns"|"column_name"|"command_function"|"command_function_code"|"comment"|"comments"|"commit"|"committed"|"concurrently"|"condition"|"condition_number"|"configuration"|"connect"|"connection"|"connection_name"|"constraint"|"constraints"|"constraint_catalog"|"constraint_name"|"constraint_schema"|"constructor"|"contains"|"content"|"continue"|"control"|"conversion"|"copy"|"corr"|"corresponding"|"cost"|"costs"|"covar_pop"|"covar_samp"|"create"|"createdb"|"createrole"|"createuser"|"cross"|"csv"|"cube"|"current"|"current_default_transform_group"|"current_path"|"current_role"|"current_row"|"current_transform_group_for_type"|"cursor"|"cursor_name"|"cycle"|"data"|"database"|"datalink"|"datetime_interval_code"|"datetime_interval_precision"|"day"|"db"|"deallocate"|"dec"|"declare"|"default"|"defaults"|"deferrable"|"deferred"|"defined"|"definer"|"degree"|"delete"|"delimiter"|"delimiters"|"depth"|"deref"|"derived"|"desc"|"describe"|"descriptor"|"deterministic"|"diagnostics"|"dictionary"|"disable"|"discard"|"disconnect"|"dispatch"|"distinct"|"dlnewcopy"|"dlpreviouscopy"|"dlurlcomplete"|"dlurlcompleteonly"|"dlurlcompletewrite"|"dlurlpath"|"dlurlpathonly"|"dlurlpathwrite"|"dlurlscheme"|"dlurlserver"|"dlvalue"|"do"|"document"|"domain"|"double"|"drop"|"dynamic"|"dynamic_function"|"dynamic_function_code"|"each"|"element"|"else"|"empty"|"enable"|"encoding"|"encrypted"|"end"|"end-exec"|"end_frame"|"end_partition"|"enforced"|"enum"|"equals"|"escape"|"event"|"except"|"exception"|"exclude"|"excluding"|"exclusive"|"exec"|"execute"|"exists"|"explain"|"expression"|"extension"|"external"|"false"|"fetch"|"file"|"filter"|"final"|"first"|"flag"|"float"|"following"|"for"|"force"|"foreign"|"fortran"|"forward"|"found"|"frame_row"|"free"|"freeze"|"from"|"fs"|"full"|"function"|"functions"|"fusion"|"g"|"general"|"generated"|"get"|"global"|"go"|"goto"|"grant"|"granted"|"greatest"|"group"|"grouping"|"groups"|"handler"|"having"|"header"|"hex"|"hierarchy"|"hold"|"hour"|"id"|"identity"|"if"|"ignore"|"ilike"|"immediate"|"immediately"|"immutable"|"implementation"|"implicit"|"import"|"in"|"including"|"increment"|"indent"|"index"|"indexes"|"indicator"|"inherit"|"inherits"|"initially"|"inline"|"inner"|"inout"|"input"|"insensitive"|"insert"|"instance"|"instantiable"|"instead"|"integrity"|"intersect"|"intersection"|"into"|"invoker"|"is"|"isnull"|"isolation"|"join"|"k"|"key"|"key_member"|"key_type"|"label"|"language"|"large"|"last"|"lateral"|"lc_collate"|"lc_ctype"|"leading"|"leakproof"|"least"|"level"|"library"|"like"|"like_regex"|"limit"|"link"|"listen"|"load"|"local"|"location"|"locator"|"lock"|"login"|"m"|"map"|"mapping"|"match"|"matched"|"materialized"|"maxvalue"|"max_cardinality"|"member"|"merge"|"message_length"|"message_octet_length"|"message_text"|"method"|"minute"|"minvalue"|"mode"|"modifies"|"module"|"month"|"more"|"move"|"multiset"|"mumps"|"names"|"namespace"|"national"|"natural"|"nchar"|"nclob"|"nesting"|"new"|"next"|"nfc"|"nfd"|"nfkc"|"nfkd"|"nil"|"no"|"nobypassrls"|"nocreatedb"|"nocreaterole"|"nocreateuser"|"noinherit"|"nologin"|"nosuperuser"|"noreplication"|"none"|"normalize"|"normalized"|"not"|"nothing"|"notify"|"notnull"|"nowait"|"null"|"nullable"|"nullif"|"nulls"|"number"|"object"|"occurrences_regex"|"octets"|"of"|"off"|"offset"|"oids"|"old"|"on"|"only"|"open"|"operator"|"option"|"options"|"or"|"order"|"ordering"|"ordinality"|"others"|"out"|"outer"|"output"|"over"|"overlaps"|"overriding"|"owned"|"owner"|"p"|"pad"|"parameter"|"parameter_mode"|"parameter_name"|"parameter_ordinal_position"|"parameter_specific_catalog"|"parameter_specific_name"|"parameter_specific_schema"|"parser"|"partial"|"partition"|"pascal"|"passing"|"passthrough"|"password"|"percent"|"percentile_cont"|"percentile_disc"|"period"|"permission"|"placing"|"plans"|"pli"|"portion"|"position_regex"|"precedes"|"preceding"|"precision"|"prepare"|"prepared"|"preserve"|"primary"|"prior"|"privileges"|"procedural"|"procedure"|"program"|"public"|"quote"|"range"|"read"|"reads"|"reassign"|"recheck"|"recovery"|"recursive"|"ref"|"references"|"referencing"|"refresh"|"regr_avgx"|"regr_avgy"|"regr_count"|"regr_intercept"|"regr_r2"|"regr_slope"|"regr_sxx"|"regr_sxy"|"regr_syy"|"reindex"|"relative"|"release"|"rename"|"repeatable"|"replica"|"replication"|"requiring"|"reset"|"respect"|"restart"|"restore"|"restrict"|"result"|"return"|"returned_cardinality"|"returned_length"|"returned_octet_length"|"returned_sqlstate"|"returning"|"returns"|"revoke"|"role"|"rollback"|"rollup"|"routine"|"routine_catalog"|"routine_name"|"routine_schema"|"row"|"rows"|"row_count"|"rule"|"savepoint"|"scale"|"schema"|"schema_name"|"scope"|"scope_catalog"|"scope_name"|"scope_schema"|"scroll"|"search"|"second"|"section"|"security"|"select"|"selective"|"self"|"sensitive"|"sequence"|"sequences"|"serializable"|"server"|"server_name"|"session"|"set"|"setof"|"sets"|"share"|"show"|"similar"|"simple"|"size"|"snapshot"|"some"|"source"|"space"|"specific"|"specifictype"|"specific_name"|"sql"|"sqlcode"|"sqlerror"|"sqlexception"|"sqlstate"|"sqlwarning"|"stable"|"standalone"|"start"|"state"|"statement"|"static"|"statistics"|"stddev_pop"|"stddev_samp"|"stdin"|"stdout"|"storage"|"strict"|"structure"|"style"|"subclass_origin"|"submultiset"|"substring_regex"|"succeeds"|"superuser"|"symmetric"|"sysid"|"system"|"system_time"|"system_user"|"t"|"table"|"tables"|"tablesample"|"tablespace"|"table_name"|"temp"|"template"|"temporary"|"then"|"ties"|"timezone_hour"|"timezone_minute"|"timing"|"to"|"token"|"top_level_count"|"trailing"|"transaction"|"transactions_committed"|"transactions_rolled_back"|"transaction_active"|"transform"|"transforms"|"translate_regex"|"translation"|"treat"|"trigger"|"trigger_catalog"|"trigger_name"|"trigger_schema"|"trim_array"|"true"|"truncate"|"trusted"|"type"|"types"|"uescape"|"unbounded"|"uncommitted"|"under"|"unencrypted"|"union"|"unique"|"unknown"|"unlink"|"unlisten"|"unlogged"|"unnamed"|"until"|"untyped"|"update"|"uri"|"usage"|"user"|"user_defined_type_catalog"|"user_defined_type_code"|"user_defined_type_name"|"user_defined_type_schema"|"using"|"vacuum"|"valid"|"validate"|"validator"|"value"|"values"|"value_of"|"varbinary"|"variadic"|"varying"|"var_pop"|"var_samp"|"verbose"|"versioning"|"view"|"volatile"|"when"|"whenever"|"where"|"whitespace"|"window"|"with"|"within"|"without"|"work"|"wrapper"|"write"|"xmlattributes"|"xmlbinary"|"xmlcast"|"xmldeclaration"|"xmldocument"|"xmliterate"|"xmlnamespaces"|"xmlparse"|"xmlquery"|"xmlschema"|"xmlserialize"|"xmltable"|"xmltext"|"xmlvalidate"|"year"|"yes"|"zone"
SQL_FUNCTION  = "abbrev"|"abs"|"acos"|"age"|"area"|"array_agg"|"array_append"|"array_cat"|"array_dims"|"array_fill"|"array_length"|"array_lower"|"array_ndims"|"array_prepend"|"array_remove"|"array_replace"|"array_to_json"|"array_to_string"|"array_upper"|"ascii"|"asin"|"atan"|"atan2"|"avg"|"bit"|"bit_and"|"bit_length"|"bit_or"|"bool_and"|"bool_or"|"broadcast"|"btrim"|"cbrt"|"ceil"|"ceiling"|"center"|"char_length"|"chr"|"clock_timestamp"|"col_description"|"concat"|"concat_ws"|"convert"|"convert_from"|"convert_to"|"cos"|"cot"|"count"|"cume_dist"|"current_catalog"|"current_database"|"current_date"|"current_query"|"current_schema"|"current_schemas"|"current_setting"|"current_time"|"current_timestamp"|"current_user"|"currval"|"cursor_to_xml"|"date_part"|"date_trunc"|"decode"|"degrees"|"dense_rank"|"diameter"|"div"|"encode"|"enum_first"|"enum_last"|"enum_range"|"every"|"exp"|"extract"|"family"|"first_value"|"floor"|"format"|"format_type"|"generate_series"|"generate_subscripts"|"get_bit"|"get_byte"|"get_current_ts_config"|"has_any_column_privilege"|"has_column_privilege"|"has_database_privilege"|"has_foreign_data_wrapper_privilege"|"has_function_privilege"|"has_language_privilege"|"has_schema_privilege"|"has_sequence_privilege"|"has_server_privilege"|"has_tablespace_privilege"|"has_table_privilege"|"height"|"host"|"hostmask"|"inet_client_addr"|"inet_client_port"|"inet_server_addr"|"inet_server_port"|"initcap"|"isclosed"|"isempty"|"isfinite"|"isopen"|"json_agg"|"json_array_elements"|"json_array_length"|"json_each"|"json_each_text"|"json_extract_path"|"json_extract_path_text"|"json_object_keys"|"json_populate_record"|"json_populate_recordset"|"justify_days"|"justify_hours"|"justify_interval"|"lag"|"lastval"|"last_value"|"lead"|"left"|"length"|"ln"|"localtime"|"localtimestamp"|"log"|"lower"|"lower_inc"|"lower_inf"|"lpad"|"ltrim"|"masklen"|"max"|"md5"|"min"|"mod"|"netmask"|"network"|"nextval"|"now"|"npoints"|"nth_value"|"ntile"|"numnode"|"obj_description"|"octet_length"|"overlay"|"pclose"|"percent_rank"|"pg_advisory_lock"|"pg_advisory_lock_shared"|"pg_advisory_unlock"|"pg_advisory_unlock_all"|"pg_advisory_unlock_shared"|"pg_advisory_xact_lock"|"pg_advisory_xact_lock_shared"|"pg_backend_pid"|"pg_backup_start_time"|"pg_cancel_backend"|"pg_client_encoding"|"pg_collation_is_visible"|"pg_column_size"|"pg_conf_load_time"|"pg_conversion_is_visible"|"pg_create_restore_point"|"pg_current_xlog_insert_location"|"pg_current_xlog_location"|"pg_database_size"|"pg_describe_object"|"pg_export_snapshot"|"pg_function_is_visible"|"pg_get_constraintdef"|"pg_get_expr"|"pg_get_functiondef"|"pg_get_function_arguments"|"pg_get_function_identity_arguments"|"pg_get_function_result"|"pg_get_indexdef"|"pg_get_keywords"|"pg_get_ruledef"|"pg_get_serial_sequence"|"pg_get_triggerdef"|"pg_get_userbyid"|"pg_get_viewdef"|"pg_has_role"|"pg_identify_object"|"pg_indexes_size"|"pg_is_in_backup"|"pg_is_in_recovery"|"pg_is_other_temp_schema"|"pg_is_xlog_replay_paused"|"pg_last_xact_replay_timestamp"|"pg_last_xlog_receive_location"|"pg_last_xlog_replay_location"|"pg_listening_channels"|"pg_ls_dir"|"pg_my_temp_schema"|"pg_opclass_is_visible"|"pg_operator_is_visible"|"pg_opfamily_is_visible"|"pg_options_to_table"|"pg_postmaster_start_time"|"pg_read_binary_file"|"pg_read_file"|"pg_relation_filenode"|"pg_relation_filepath"|"pg_relation_size"|"pg_reload_conf"|"pg_rotate_logfile"|"pg_size_pretty"|"pg_start_backup"|"pg_stat_file"|"pg_stop_backup"|"pg_switch_xlog"|"pg_tablespace_databases"|"pg_tablespace_location"|"pg_tablespace_size"|"pg_table_is_visible"|"pg_table_size"|"pg_terminate_backend"|"pg_total_relation_size"|"pg_trigger_depth"|"pg_try_advisory_lock"|"pg_try_advisory_lock_shared"|"pg_try_advisory_xact_lock"|"pg_try_advisory_xact_lock_shared"|"pg_ts_config_is_visible"|"pg_ts_dict_is_visible"|"pg_ts_parser_is_visible"|"pg_ts_template_is_visible"|"pg_typeof"|"pg_type_is_visible"|"pg_xlogfile_name"|"pg_xlogfile_name_offset"|"pg_xlog_location_diff"|"pg_xlog_replay_pause"|"pg_xlog_replay_resume"|"pi"|"plainto_tsquery"|"popen"|"position"|"power"|"querytree"|"query_to_xml"|"quote_ident"|"quote_literal"|"quote_nullable"|"radians"|"radius"|"random"|"rank"|"regexp_matches"|"regexp_replace"|"regexp_split_to_array"|"regexp_split_to_table"|"repeat"|"replace"|"reverse"|"right"|"round"|"row_number"|"row_to_json"|"rpad"|"rtrim"|"session_user"|"setseed"|"setval"|"setweight"|"set_bit"|"set_byte"|"set_config"|"set_masklen"|"shobj_description"|"sign"|"sin"|"split_part"|"sqrt"|"statement_timestamp"|"string_agg"|"string_to_array"|"strip"|"strpos"|"substr"|"substring"|"sum"|"table_to_xml"|"tan"|"timeofday"|"to_ascii"|"to_char"|"to_date"|"to_hex"|"to_json"|"to_number"|"to_timestamp"|"to_tsquery"|"to_tsvector"|"transaction_timestamp"|"translate"|"trim"|"trunc"|"tsvector_update_trigger"|"tsvector_update_trigger_column"|"ts_headline"|"ts_rank"|"ts_rank_cd"|"ts_rewrite"|"txid_current"|"txid_current_snapshot"|"txid_snapshot_xip"|"txid_snapshot_xmax"|"txid_snapshot_xmin"|"txid_visible_in_snapshot"|"unnest"|"upper"|"upper_inc"|"upper_inf"|"version"|"width"|"width_bucket"|"xip_list"|"xmax"|"xmin"|"xmlagg"|"xmlcomment"|"xmlconcat"|"xmlelement"|"xmlexists"|"xmlforest"|"xmlpi"|"xmlroot"|"xml_is_well_formed"|"xpath"|"xpath_exists"
SQL_DATATYPE = "array"|"bigint"|"bigserial"|"bool"|"boolean"|"box"|"bpchar"|"bytea"|"char"|"character"|"cid"|"cidr"|"circle"|"date"|"decimal"|"double_precision"|"inet"|"int"|"int2"|"int2vector"|"int4"|"int8"|"integer"|"interval"|"json"|"jsonb"|"line"|"lseg"|"macaddr"|"money"|"name"|"numeric"|"oid"|"path"|"point"|"polygon"|"real"|"serial"|"serial8"|"smallint"|"smallserial"|"text"|"tid"|"time"|"timestamp"|"tsquery"|"tsvector"|"uuid"|"varbit"|"varchar"|"xid"|"xml"|"yaml"
SQL_PARAMETER = "security_barrier"|"check_option"

%state DIV
%%

{VARIABLE}          { return tt.getTokenType("VARIABLE"); }

{WHITE_SPACE}+      { return stt.getWhiteSpace(); }

{BLOCK_COMMENT}     { return stt.getBlockComment(); }
{LINE_COMMENT}      { return stt.getLineComment(); }

{INTEGER}           { return stt.getInteger(); }
{NUMBER}            { return stt.getNumber(); }
{STRING}            { return stt.getString(); }

{SQL_FUNCTION}      { return tt.getTokenType("FUNCTION");}
{SQL_PARAMETER}     { return tt.getTokenType("PARAMETER");}
{SQL_DATATYPE}      { return tt.getTokenType("DATA_TYPE"); }
{SQL_KEYWORD}       { return tt.getTokenType("KEYWORD"); }

{OPERATOR}          { return tt.getTokenType("OPERATOR"); }

{IDENTIFIER}        { return stt.getIdentifier(); }
{QUOTED_IDENTIFIER} { return stt.getQuotedIdentifier(); }


"("                 { return stt.getChrLeftParenthesis(); }
")"                 { return stt.getChrRightParenthesis(); }
"["                 { return stt.getChrLeftBracket(); }
"]"                 { return stt.getChrRightBracket(); }

.                   { return stt.getIdentifier(); }
