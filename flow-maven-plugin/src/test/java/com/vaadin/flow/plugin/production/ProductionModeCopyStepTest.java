/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.vaadin.flow.plugin.production;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.plugin.TestUtils;
import com.vaadin.flow.plugin.common.JarContentsManager;
import com.vaadin.flow.plugin.common.WebJarData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Vaadin Ltd.
 */
public class ProductionModeCopyStepTest {
    private WebJarData getWebJarData(String version, String artifactId) {
        return new WebJarData(TestUtils.getTestJar(String.format("%s-%s.jar", artifactId, version)), artifactId, version);
    }

    @Rule
    public TemporaryFolder testDirectory = new TemporaryFolder();

    @Test(expected = IllegalArgumentException.class)
    public void webJarsWithDifferentVersions_fail() {
        File testJar = TestUtils.getTestJar();
        String version1 = "2.0.0";
        String version2 = "2.0.1";
        String artifactId = "paper-button";

        JarContentsManager noBowerJsonManager = mock(JarContentsManager.class);
        when(noBowerJsonManager.findFiles(any(File.class), anyString(), anyString())).thenReturn(Collections.singletonList("test"));
        when(noBowerJsonManager.getFileContents(any(File.class), any())).thenReturn(String.format("{'name' : '%s'}", artifactId).getBytes(StandardCharsets.UTF_8));

        new ProductionModeCopyStep(
                noBowerJsonManager,
                new HashSet<>(Arrays.asList(new WebJarData(testJar, artifactId, version1), new WebJarData(testJar, artifactId, version2))),
                Collections.emptySet()
        );
    }

    /**
     * WebJars' issues with incorrect prefixes should be treated normally.
     *
     * @see <a href="https://github.com/webjars/webjars/issues/1656">https://github.com/webjars/webjars/issues/1656</a>
     */
    @Test
    public void webJarsWithSameBowerNamesAndDifferentArtifactIds_work() {
        String version = "2.0.0";
        String artifactId = "paper-button";

        String prefixedArtifactId = "github-com-polymerelements-" + artifactId;

        new ProductionModeCopyStep(
                new JarContentsManager(),
                new HashSet<>(Arrays.asList(getWebJarData(version, artifactId), getWebJarData(version, prefixedArtifactId))),
                Collections.emptySet()
        );
    }

    @Test
    public void webJarsWithMultiplePackages_work() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning", TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(
                new JarContentsManager(),
                Collections.singleton(getWebJarData("6.0.0-alpha3", "vaadin-charts-webjar")),
                Collections.emptySet()
        ).copyWebApplicationFiles(outputDirectory, null, null);

        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);
        assertFalse("Files should be copied from the test WebJar", resultingFiles.isEmpty());
        assertEquals("WebJar with multiple bower.json are handled correctly and copied",
                2, resultingFiles.stream().filter(path -> path.endsWith(File.separator + "bower.json")).count());
    }

    @Test(expected = NullPointerException.class)
    public void copyWebApplicationFiles_nullOutputDirectory() {
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.emptySet())
                .copyWebApplicationFiles(null, testDirectory.getRoot(), "sss");
    }

    @Test(expected = UncheckedIOException.class)
    public void copyWebApplicationFiles_fileInsteadOfOutputDirectory() throws IOException {
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.emptySet())
                .copyWebApplicationFiles(testDirectory.newFile("test"), testDirectory.getRoot(), "sss");
    }

    @Test
    public void copyWebApplicationFiles_nothingSpecified() {
        File outputDirectory = testDirectory.getRoot();
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, null, null);
        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertTrue("Output directory should not contain any files since no frontend directory or jars are specified",
                resultingFiles.isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_copyFrontendDirectory_noExclusions() {
        File outputDirectory = testDirectory.getRoot();
        File frontendOutputDirectory = new File(".").getAbsoluteFile();
        SortedSet<String> originalFiles = new TreeSet<>(TestUtils.listFilesRecursively(frontendOutputDirectory));

        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, frontendOutputDirectory, null);
        assertEquals("Output directory should contain all files from frontend directory '%s' and only them",
                originalFiles, new TreeSet<>(TestUtils.listFilesRecursively(outputDirectory)));
    }

    @Test
    public void copyWebApplicationFiles_copyFrontendDirectory_withExclusions() {
        File outputDirectory = testDirectory.getRoot();
        File frontendOutputDirectory = new File(".").getAbsoluteFile();
        List<String> originalFiles = TestUtils.listFilesRecursively(frontendOutputDirectory);

        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, frontendOutputDirectory, "*.jar, *.class");

        SortedSet<String> filteredPaths = originalFiles.stream().filter(path -> !path.endsWith(".jar") && !path.endsWith(".class"))
                .collect(Collectors.toCollection(TreeSet::new));
        assertFalse("Original directory should contain files that are not filtered", filteredPaths.isEmpty());
        assertEquals("Output directory should contain filtered files from frontend directory '%s' and only them",
                filteredPaths, new TreeSet<>(TestUtils.listFilesRecursively(outputDirectory)));
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_noExclusions() {
        String version = "2.0.0";
        String artifactId = "paper-button";
        File outputDirectory = testDirectory.getRoot();

        new ProductionModeCopyStep(new JarContentsManager(), Collections.singleton(getWebJarData(version, artifactId)), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, null, null);

        String expectedPathPrefix = "bower_components" + File.separator + artifactId;
        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertFalse("WebJar files should be present in output directory",
                resultingFiles.isEmpty());
        assertTrue("All WebJar files should be put into (bower_components + File.separator + bower name for WebJar) directory",
                resultingFiles.stream().allMatch(path -> path.startsWith(expectedPathPrefix)));
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_excludeAll() {
        String version = "2.0.0";
        String artifactId = "paper-button";
        File outputDirectory = testDirectory.getRoot();

        new ProductionModeCopyStep(new JarContentsManager(), Collections.singleton(getWebJarData(version, artifactId)), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, null, "*");

        assertTrue("WebJar files should not be copied due to exclusions",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());
    }

    @Test
    public void copyWebApplicationFiles_copyWebJar_bowerJsonShouldBePresent() throws IOException {
        String version = "2.0.0";
        String artifactId = "github-com-polymerelements-paper-button";
        WebJarData webJarToCopy = getWebJarData(version, artifactId);

        JarContentsManager noBowerJsonManager = mock(JarContentsManager.class);
        String expectedFilePath = "bower.json";
        when(noBowerJsonManager.findFiles(webJarToCopy.getJarFile(), WebJarData.WEB_JAR_FILES_BASE, expectedFilePath))
                .thenReturn(Collections.emptyList());

        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        new ProductionModeCopyStep(noBowerJsonManager, Collections.singleton(webJarToCopy), Collections.emptySet())
                .copyWebApplicationFiles(outputDirectory, null, null);

        assertTrue("WebJar with no bower.json is not unpacked into output directory.",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());

        verify(noBowerJsonManager, only()).findFiles(webJarToCopy.getJarFile(), WebJarData.WEB_JAR_FILES_BASE, expectedFilePath);
        verify(noBowerJsonManager, times(1)).findFiles(webJarToCopy.getJarFile(), WebJarData.WEB_JAR_FILES_BASE, expectedFilePath);
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_noFrontendFiles() {
        File outputDirectory = testDirectory.getRoot();
        File noFrontendFilesJar = TestUtils.getTestJar("jar-without-frontend-resources.jar");
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.singleton(noFrontendFilesJar))
                .copyWebApplicationFiles(outputDirectory, null, null);
        assertEquals("Non WebJar with no web resources should not be copied to output directory",
                TestUtils.listFilesRecursively(outputDirectory).size(), 0);
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_withFrontendFiles_noExclusions() {
        File outputDirectory = testDirectory.getRoot();
        File noFrontendFilesJar = TestUtils.getTestJar("jar-with-frontend-resources.jar");
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.singleton(noFrontendFilesJar))
                .copyWebApplicationFiles(outputDirectory, null, null);
        assertTrue("Non WebJar with web resources should be copied to output directory",
                TestUtils.listFilesRecursively(outputDirectory).size() > 0);
    }

    @Test
    public void copyWebApplicationFiles_copyNonWebJar_withFrontendFiles_withExclusions() throws IOException {
        File noFrontendFilesJar = TestUtils.getTestJar("jar-with-frontend-resources.jar");

        File noExclusionsDirectory = testDirectory.newFolder("noExclusions");
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.singleton(noFrontendFilesJar))
                .copyWebApplicationFiles(noExclusionsDirectory, null, null);
        List<String> allFiles = TestUtils.listFilesRecursively(noExclusionsDirectory);
        assertTrue("Files copied without filters should contain *.html and *.json files",
                allFiles.stream().anyMatch(path -> path.endsWith(".json") || path.endsWith(".html")));

        File exclusionsDirectory = testDirectory.newFolder("exclusions");
        new ProductionModeCopyStep(new JarContentsManager(), Collections.emptySet(), Collections.singleton(noFrontendFilesJar))
                .copyWebApplicationFiles(exclusionsDirectory, null, "*.json, *.html");
        List<String> filteredFiles = TestUtils.listFilesRecursively(exclusionsDirectory);

        assertTrue("Files copied without filter should contain more files than the filtered ones",
                allFiles.size() > filteredFiles.size());
        assertTrue("Files copied without filters should not contain *.html and *.json files", filteredFiles.stream().noneMatch(path -> path.endsWith(".json") || path.endsWith(".html")));
        assertTrue("Files copied without filter should contain all filtered files", allFiles.containsAll(filteredFiles));
    }

    /**
     * WebJar tested has a name github-com-PolymerElements-iron-behaviors-2.0.0.jar but all paths inside are lower cased.
     * Looks like an exception rather than a regular WebJar, but we should be able to bypass it anyway.
     *
     * @see <a href="https://github.com/webjars/webjars/issues/1668">https://github.com/webjars/webjars/issues/1668</a>
     */
    @Test
    public void copyWebApplicationFiles_webJarWithWrongCasedInside() {
        File outputDirectory = testDirectory.getRoot();
        assertTrue("No files should be in output directory before the beginning",
                TestUtils.listFilesRecursively(outputDirectory).isEmpty());
        String version = "2.0.0";
        String artifactId = "github-com-PolymerElements-iron-behaviors";

        new ProductionModeCopyStep(
                new JarContentsManager(),
                new HashSet<>(Arrays.asList(getWebJarData(version, artifactId), getWebJarData(version, artifactId))),
                Collections.emptySet()
        ).copyWebApplicationFiles(outputDirectory, null, null);

        List<String> resultingFiles = TestUtils.listFilesRecursively(outputDirectory);

        assertFalse("WebJar files should be present in output directory",
                resultingFiles.isEmpty());
        assertTrue("All WebJar files should be put into (bower_components + File.separator + bower name for WebJar) directory",
                resultingFiles.stream().allMatch(path -> path.startsWith("bower_components" + File.separator + "iron-behaviors")));
    }
}
