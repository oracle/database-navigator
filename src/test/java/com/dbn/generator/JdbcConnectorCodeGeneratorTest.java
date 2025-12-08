package com.dbn.generator;

/*
import com.dbn.common.compatibility.Compatibility;
import com.dbn.test.util.FileUtil;
import com.dbn.test.util.TextCompare;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
*/
// TODO use intellij velocity template engine (compatibility issues with earlier verisons of intellij)
public class JdbcConnectorCodeGeneratorTest {

/*

    public static final String BASE_TEMPLATE_FILE_NAME = "DBN - JDBC Connector.java.ft";
    public static final String TEMPLATE_FILE_BASE_PATH = "fileTemplates/";
    public static final String CHECK_FILE_BASE_PATH = "generator/conn_settings_cases";
    public static final String VARIABLES_PROPERTIES_FILE_NAME = "/variables.properties";
    public static final String JDBCDRIVER_CONN_JAVA_CHECK = "/JDBCDriverConn.java.check";
    private final String name;
    private final String useCaseDir;
    private Properties properties;
    private VelocityContext context;
    private InputStreamReader reader;
    private String templatePath;
    private VelocityEngine ve;

    public JdbcConnectorCodeGeneratorTest(String name, String useCaseDir) {
        this.name = name;
        this.useCaseDir = useCaseDir;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"OCI API Key Without Scope Ids", "oci_api_key_without_scope_ids"},
            {"OCI API Key With Compartment Ids", "oci_api_key_with_compartment_id"},
            {"OCI API Key With Compartment and Database Ids", "oci_api_key_with_compartment_and_database_ids"},
            {"OCI Interactive Without Scope Ids", "oci_interactive_without_scope_ids"},
            {"OCI Interactive With Compartment Ids", "oci_interactive_with_compartment_id"},
            {"OCI Interactive With Compartment and Database Ids", "oci_interactive_with_compartment_and_database_ids"},
            {"Azure Secret File", "azure_secret_token"},
            {"Azure Certificate File", "azure_cert_nopass"},
            {"Azure Interactive", "azure_interactive"},
            {"Azure Certificate File with Password", "azure_cert_with_pass"}
        });
    }
    @Before
    public void before() throws IOException {
        this.ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        ve.setProperty("resource.loader.classpath.class", MyClasspathResourceLoader.class.getName());
        ve.setProperty("resource.loader.classpath.path", "fileTemplates/includes");
        ve.init();
        this.templatePath = TEMPLATE_FILE_BASE_PATH + BASE_TEMPLATE_FILE_NAME;
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(templatePath);
        if (input == null) {
            throw new IOException("Template file doesn't exist");
        }
        this.reader = new InputStreamReader(input);
        this.context = new VelocityContext();
    }

    @Test
    public void test() throws Exception {
        // load template inputs
        File propFile = FileUtil.getFileFromClasspath(
                JdbcConnectorCodeGeneratorTest.class,
                CHECK_FILE_BASE_PATH+"/"+useCaseDir+ VARIABLES_PROPERTIES_FILE_NAME);
        assertTrue(propFile.isFile());
        this.properties = FileUtil.loadFromFile(propFile);
        if (properties != null) {
            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                context.put((String)property.getKey(), property.getValue());
            }
        }

        // set up a buffered string writer to capture to output of the evaluated template.
        StringWriter stringWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);

        // ** evaluate the template
        if (!ve.evaluate(context, writer, templatePath, reader)) {
            throw new Exception("Failed to convert the template into html.");
        }

        // make sure the template out put gets flushed
        writer.flush();

        // load into a line reader so we check line by line.
        StringReader stringReader = new StringReader(stringWriter.toString());
        LineNumberReader lnr = new LineNumberReader(stringReader);

        String resourcePath = CHECK_FILE_BASE_PATH + "/" + useCaseDir + "/" + JDBCDRIVER_CONN_JAVA_CHECK;
        File checkFile = FileUtil.getFileFromClasspath(
                JdbcConnectorCodeGeneratorTest.class,
                resourcePath,
                false);
        if (checkFile == null || !checkFile.isFile()) {
            System.out.println(stringWriter.toString());
            fail("Missing check file: "+resourcePath);
        }
        // load the check file
        FileReader expectFileReader = new FileReader(checkFile);
        LineNumberReader expectedReader = new LineNumberReader(expectFileReader);

        String expectedLine;
        String actualLine;
        Pattern p = Pattern.compile("\\$\\{(.*)\\}");
        boolean failed = false;
        while ((expectedLine = expectedReader.readLine()) != null) {
            actualLine = lnr.readLine();
            if (actualLine == null) {
                failed = true;
                System.err.println("The actual file is too short");
            }
            if (!failed) {
                Matcher m = p.matcher(actualLine);
                if (m.find()) {
                    System.err.printf("At %d: %s\n", lnr.getLineNumber(), actualLine);
                    failed = true;
                } else {
                    TextCompare.Diff diff = TextCompare.diff(expectedLine, actualLine);
                    if (diff != TextCompare.NO_DIFF) {
                        failed = true;
                        System.err.printf("Expected line mismatches actual at: %d\n", diff.getStartOfMismatch());
                        System.err.printf("Expected %d: %s\n", expectedReader.getLineNumber(), expectedLine);
                        System.err.printf("Actual   %d: %s\n", lnr.getLineNumber(), actualLine);
                    }
                }
            }
        }
        assertFalse("An error occurred with the test.  See stderr for more info.", failed);
        writer.close();
    }

    public static class MyClasspathResourceLoader extends ClasspathResourceLoader {
        public MyClasspathResourceLoader() {
            super();
        }

        @Override
        public Reader getResourceReader(String name, String encoding) throws ResourceNotFoundException {
            if (name.startsWith("DBN - JDBC")) {
                name = "fileTemplates/includes/"+ name + ".ft";
            }
            return super.getResourceReader(name, encoding);
        }
    }
*/

    /**
     * context.put("DATABASE_TYPE", "ORACLE");
     *         context.put("CLASS_NAME", "MyClassName");
     *         context.put("PACKAGE_NAME", "com.test");
     *         context.put("DATABASE_TYPE", DatabaseType.ORACLE);
     *         context.put("JDBC_URL", "jdbc://foo");
     *         context.put("JDBC_DRIVER", "OracleDriver");
     *         context.put("JDBC_URL_PATTERN", "fleuh");
     *         context.put("JDBC_URL_TYPE", DatabaseUrlType.TNS);
     *         context.put("JDBC_URL_TYPE_NAME", DatabaseUrlType.TNS.getName());
     *
     *         context.put("TNS_FOLDER", "/tnsFolder");
     *         context.put("TNS_PROFILE", "tns_high");
     *
     *         context.put("AUTH_TYPE", AuthenticationType.TOKEN);
     *         context.put("AUTH_TYPE_NAME", AuthenticationType.TOKEN.getName());
     *         context.put("AUTH_TOKEN_TYPE", AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERT);
     *         context.put("AUTH_TOKEN_TYPE_NAME", AuthenticationTokenType.AZURE_SERVICE_PRINCIPAL_CERT.getName());
     *
     *         //context.put("AZURE_TOKEN_CLIENT_ID", "fffff-11010-0101-1010");
     *         context.put("AZURE_TOKEN_TENANT_ID", "ggggg-29292-3333-1234");
     *         context.put("AZURE_TOKEN_DATABASE_ID_URI", "https://oracledevelopment.onmicrosoft.com/38aaaaaa-ppppp-qqqqqq-b932-xxxxxxxxx");
     *         context.put("AZURE_TOKEN_CLIENT_SECRET_FILE", "/Users/chuck/foo.pem");
     *         char[] azureClientSecretFilePassword = new char[] {'a', 'b', 'c', 'd', 'e'};
     *         if (Chars.isNotEmpty(azureClientSecretFilePassword)) {
     *             context.put("AZURE_TOKEN_CLIENT_SECRET_FILE_PASSWORD", azureClientSecretFilePassword);
     *         }
     *         context.put("AZURE_TOKEN_CLIENT_SECRET_TOKEN", "Foo");
     */
}
