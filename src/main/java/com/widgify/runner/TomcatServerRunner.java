package com.widgify.runner;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;

public class TomcatServerRunner {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        String contextPath = "/widgify";
        
        File workDir = new File("target/tomcat");
        if (!workDir.exists()) {
            workDir.mkdirs();
        }

        File webappDir = new File("src/main/webapp");
        File targetWebapp = new File("target/widgify");
        String docBase = targetWebapp.exists() ? targetWebapp.getAbsolutePath() : webappDir.getAbsolutePath();

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        tomcat.setBaseDir(workDir.getAbsolutePath());
        tomcat.getHost().setAppBase(new File("target").getAbsolutePath());
        tomcat.getConnector();

        Context ctx = tomcat.addWebapp(contextPath, docBase);
        
        // Ensure webapp class loader delegates to current classloader for filter & servlet classes
        ctx.setParentClassLoader(TomcatServerRunner.class.getClassLoader());

        System.out.println("==================================================");
        System.out.println("Widgify Apache Tomcat Web Server Running!");
        System.out.println("URL: http://localhost:" + port + contextPath + "/");
        System.out.println("DocBase: " + docBase);
        System.out.println("==================================================");

        tomcat.start();
        tomcat.getServer().await();
    }
}
