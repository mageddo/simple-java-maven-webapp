package com.mageddo.jetty.junit.tests.servlets;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;
import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.FragmentConfiguration;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

/**
 * @author elvis
 * @version $Revision: $<br/>
 *          $Id: $
 * @since 6/22/16 1:58 PM
 */

public class Main {

    public static void main(String[] args) throws Exception {

        final Server server = new Server(8585);
//        URI webResourceBase = findWebResourceBase(server.getClass().getClassLoader());
        // System.err.println("Using BaseResource: " + webResourceBase);
        WebAppContext context = new WebAppContext();
        final URL resource = server.getClass().getResource("/webapp");
        context.setBaseResource(Resource.newResource(resource));
        context.setConfigurations(new Configuration[] {
            new AnnotationConfiguration(),
            new WebInfConfiguration(),
            new WebXmlConfiguration(),
            new MetaInfConfiguration(),
            new FragmentConfiguration(),
            new EnvConfiguration(),
            new PlusConfiguration(),
            new JettyWebXmlConfiguration()

        });

        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        context.setConfigurationDiscovered(true);
        context.addServerClass("com.mageddo.jetty.junit.tests.servlets");
        server.setHandler(context);
        server.start();
        server.dump(System.err);
        server.join();
    }

    private static URI findWebResourceBase(ClassLoader classLoader) {
        String webResourceRef = "webapp/index.html";

        try {
            // Look for resource in classpath (best choice when working with archive jar/war file)
            URL webXml = classLoader.getResource('/' + webResourceRef);
            if (webXml != null) {
                URI uri = webXml.toURI().resolve("..").normalize();
                System.err.printf("WebResourceBase (Using ClassLoader reference) %s%n", uri);
                return uri;
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad ClassPath reference for: " + webResourceRef, e);
        }

        // Look for resource in common file system paths
        try {
            Path pwd = new File(System.getProperty("user.dir")).toPath().toRealPath();
            FileSystem fs = pwd.getFileSystem();

            // Try the generated maven path first
            PathMatcher matcher = fs.getPathMatcher("glob:**/embedded-servlet-*");
            try (DirectoryStream<Path> dir = Files.newDirectoryStream(pwd.resolve("target"))) {
                for (Path path : dir) {
                    if (Files.isDirectory(path) && matcher.matches(path)) {
                        // Found a potential directory
                        Path possible = path.resolve(webResourceRef);
                        // Does it have what we need?
                        if (Files.exists(possible)) {
                            URI uri = path.toUri();
                            System.err.printf("WebResourceBase (Using discovered /target/ Path) %s%n", uri);
                            return uri;
                        }
                    }
                }
            }

            // Try the source path next
            Path srcWebapp = pwd.resolve("src/main/webapp/" + webResourceRef);
            if (Files.exists(srcWebapp)) {
                URI uri = srcWebapp.getParent().toUri();
                System.err.printf("WebResourceBase (Using /src/main/webapp/ Path) %s%n", uri);
                return uri;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Unable to find web resource in file system: " + webResourceRef, t);
        }

        throw new RuntimeException("Unable to find web resource ref: " + webResourceRef);

    }

}
