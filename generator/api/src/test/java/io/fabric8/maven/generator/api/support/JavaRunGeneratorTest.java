package io.fabric8.maven.generator.api.support;
/*
 *
 * Copyright 2016 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.*;

import io.fabric8.maven.core.config.OpenShiftBuildStrategy;
import io.fabric8.maven.core.config.PlatformMode;
import io.fabric8.maven.generator.api.FromSelector;
import io.fabric8.maven.generator.api.MavenGeneratorContext;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 22/09/16
 */
@RunWith(JMockit.class)
public class JavaRunGeneratorTest {

    @Mocked
    MavenGeneratorContext ctx;

    @Mocked
    MavenProject project;

    @Mocked
    Plugin plugin;

    @Test
    public void fromSelector() throws IOException {
        Object[] data = {
            "3.1.123", PlatformMode.kubernetes, null, "generator.java.docker.upstream",
            "3.1.redhat-101", PlatformMode.kubernetes, null, "generator.java.docker.redhat",
            "3.1.123", PlatformMode.openshift, OpenShiftBuildStrategy.docker, "generator.java.docker.upstream",
            "3.1.redhat-101", PlatformMode.openshift, OpenShiftBuildStrategy.docker, "generator.java.docker.redhat",
            "3.1.123", PlatformMode.openshift, OpenShiftBuildStrategy.s2i, "generator.java.s2i.upstream",
            "3.1.redhat-101", PlatformMode.openshift, OpenShiftBuildStrategy.s2i, "generator.java.s2i.redhat",
        };

        Properties imageProps = getDefaultImageProps();

        for (int i = 0; i < data.length; i += 4) {
            prepareExpectation((String) data[i], (PlatformMode) data[i+1], (OpenShiftBuildStrategy) data[i+2]);
            FromSelector selector = new JavaRunGenerator.Java(ctx);
            String from = selector.getFrom();
            assertEquals(imageProps.getProperty((String) data[i+3]), from);
        }
    }

    private Expectations prepareExpectation(final String version, final PlatformMode mode, final OpenShiftBuildStrategy strategy) {
        return new Expectations() {{
            ctx.getProject(); result = project;
            project.getPlugin("io.fabric8:fabric8-maven-plugin"); result = plugin;
            plugin.getVersion(); result = version;
            ctx.getMode(); result = mode;
            ctx.getStrategy(); result = strategy;
        }};
    }

    private Properties getDefaultImageProps() throws IOException {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream("/META-INF/fabric8/java-default-images.properties"));
        return props;
    }
}
