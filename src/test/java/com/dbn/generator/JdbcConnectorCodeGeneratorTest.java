package com.dbn.generator;

import com.dbn.test.util.FileUtil;
import org.apache.velocity.Template;
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
@RunWith(Parameterized.class)
public class JdbcConnectorCodeGeneratorTest {

    private final String templateVariablesFile;
    private final String name;
    private Properties properties;

    public JdbcConnectorCodeGeneratorTest(String name, String templateVariablesFile) {
        this.name = name;
        this.templateVariablesFile = templateVariablesFile;
    }
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Secret File", "generator/azure_token_secret_file.properties"}
        });
    }
    @Before
    public void before() throws IOException {
        File propFile = FileUtil.getFileFromClasspath(
                JdbcConnectorCodeGeneratorTest.class,
                templateVariablesFile);
        assertTrue(propFile.isFile());
        this.properties = FileUtil.loadFromFile(propFile);

    }

    @Test
    public void test() throws Exception {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADERS, "classpath");
        ve.setProperty("resource.loader.classpath.class", MyClasspathResourceLoader.class.getName());
        ve.setProperty("resource.loader.classpath.path", "fileTemplates/includes");
        ve.init();
        String templateName = "DBN - JDBC Connector.java.ft";
        final String templatePath = "fileTemplates/" + templateName;
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(templatePath);
        if (input == null) {
            throw new IOException("Template file doesn't exist");
        }

        InputStreamReader reader = new InputStreamReader(input);

        VelocityContext context = new VelocityContext();


        if (properties != null) {
            //stringfyNulls(properties);
            for (Map.Entry<Object, Object> property : properties.entrySet()) {
                context.put((String)property.getKey(), property.getValue());
            }
        }

        Template template = ve.getTemplate(templatePath, "UTF-8");
        String outFileName = File.createTempFile("report", ".html").getAbsolutePath();
        StringWriter stringWriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(stringWriter);

        if (!ve.evaluate(context, writer, templatePath, reader)) {
            throw new Exception("Failed to convert the template into html.");
        }

        //template.merge(context, writer);
        writer.flush();;
        //        System.out.println(stringWriter.toString());
        StringReader stringReader = new StringReader(stringWriter.toString());
        LineNumberReader lnr = new LineNumberReader(stringReader);
        String line;
        Pattern p = Pattern.compile("\\$\\{(.*)\\}");
        boolean failed = false;
        while ((line = lnr.readLine()) != null) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                System.err.printf("At %d: %s\n", lnr.getLineNumber(), line);
                failed = true;
            }
            else {
                System.out.printf("%d: %s\n", lnr.getLineNumber(), line);
            }
        }
        assertFalse("Not all required template properties were provided", failed);
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
