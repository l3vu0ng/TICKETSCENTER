package vn.ticketscenter.architecture;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/** Checks shared source boundaries and the deployment mappings selected by CI. */
class FeaturePackageStructureTest {
    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+([\\w.]+)\\s*;");

    @Test
    void packagesMatchTheirSourceDirectoriesAndUseJakarta() throws Exception {
        try (var paths = Files.walk(SOURCES)) {
            var files = paths.filter(path -> path.toString().endsWith(".java")).toList();
            assertFalse(files.isEmpty(), "No application source was checked");
            for (var file : files) {
                String source = Files.readString(file);
                var declaration = PACKAGE.matcher(source);
                assertTrue(declaration.find(), "Missing package: " + file);
                String expected =
                        SOURCES.relativize(file.getParent())
                                .toString()
                                .replace('\\', '.')
                                .replace('/', '.');
                assertEquals(expected, declaration.group(1), file.toString());
                assertTrue(expected.startsWith("vn.ticketscenter."), file.toString());
                assertFalse(
                        Pattern.compile("\\bjavax\\.(servlet|persistence)\\b")
                                .matcher(source)
                                .find(),
                        file.toString());
            }
        }
    }

    @Test
    void domainModelsDoNotDependOnHttpOrEntityManagers() throws Exception {
        try (var paths = Files.walk(SOURCES)) {
            var models =
                    paths.filter(
                                    path ->
                                            path.toString().replace('\\', '/').contains("/model/")
                                                    && path.toString().endsWith(".java"))
                            .toList();
            assertFalse(models.isEmpty(), "No domain model was checked");
            var forbidden =
                    Pattern.compile(
                            "\\b(?:jakarta\\.servlet|EntityManager|EntityManagerFactory|vn\\.ticketscenter\\.(?:controller|repository|service))\\b");
            // IdentityNormalizer is the existing pure input validator, not a transactional service.
            for (var model : models) {
                String source =
                        Files.readString(model)
                                .replace(
                                        "import vn.ticketscenter.service.identity.IdentityNormalizer;",
                                        "");
                assertFalse(forbidden.matcher(source).find(), "Domain dependency: " + model);
            }
        }
    }

    @Test
    void servletMappingsAreUniqueAndReferenceDeclaredClasses() throws Exception {
        var parser = DocumentBuilderFactory.newInstance();
        parser.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        parser.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        parser.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var document =
                parser.newDocumentBuilder()
                        .parse(Path.of("src/main/webapp/WEB-INF/web.xml").toFile());
        var declarations = document.getElementsByTagName("servlet");
        var names = new HashSet<String>();
        for (int index = 0; index < declarations.getLength(); index++) {
            var element = (org.w3c.dom.Element) declarations.item(index);
            String name =
                    element.getElementsByTagName("servlet-name").item(0).getTextContent().strip();
            String type =
                    element.getElementsByTagName("servlet-class").item(0).getTextContent().strip();
            assertTrue(names.add(name), "Duplicate servlet: " + name);
            assertTrue(
                    jakarta.servlet.http.HttpServlet.class.isAssignableFrom(Class.forName(type)),
                    type);
        }
        var mappings = document.getElementsByTagName("servlet-mapping");
        assertTrue(mappings.getLength() > 0, "No servlet mapping was checked");
        var patterns = new HashSet<String>();
        for (int index = 0; index < mappings.getLength(); index++) {
            var element = (org.w3c.dom.Element) mappings.item(index);
            String name =
                    element.getElementsByTagName("servlet-name").item(0).getTextContent().strip();
            assertTrue(names.contains(name), "Undeclared servlet: " + name);
            var urls = element.getElementsByTagName("url-pattern");
            for (int url = 0; url < urls.getLength(); url++) {
                String pattern = urls.item(url).getTextContent().strip();
                assertTrue(patterns.add(pattern), "Duplicate URL pattern: " + pattern);
            }
        }
    }
}
