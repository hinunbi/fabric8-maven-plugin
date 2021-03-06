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

import io.fabric8.maven.core.util.Configs;
import io.fabric8.maven.docker.config.AssemblyConfiguration;
import io.fabric8.maven.docker.config.BuildImageConfiguration;
import io.fabric8.maven.docker.config.ImageConfiguration;
import io.fabric8.maven.generator.api.FromSelector;
import io.fabric8.maven.generator.api.MavenGeneratorContext;
import io.fabric8.utils.Strings;

/**
 * @author roland
 * @since 21/09/16
 */

abstract public class JavaRunGenerator extends BaseGenerator {

    public JavaRunGenerator(MavenGeneratorContext context, String name) {
        super(context, name, new Java(context));
    }

    private enum Config implements Configs.Key {
        enabled        {{ d = "false"; }},
        webPort        {{ d = "8080"; }},
        jolokiaPort    {{ d = "8778"; }},
        prometheusPort {{ d = "9779"; }},
        baseDir        {{ d = "/deployments"; }},
        assemblyRef    {{ d = "artifact-with-includes"; }};

        public String def() { return d; } protected String d;
    }

    @Override
    public List<ImageConfiguration> customize(List<ImageConfiguration> configs) {
        ImageConfiguration.Builder imageBuilder = new ImageConfiguration.Builder();
        BuildImageConfiguration.Builder buildBuilder = new BuildImageConfiguration.Builder()
            .assembly(createAssembly())
            .from(getFrom())
            .ports(extractPorts());
        Map<String, String> envMap = getEnv();
        envMap.put("JAVA_APP_DIR", getConfig(Config.baseDir));
        buildBuilder.env(envMap);
        addLatestTagIfSnapshot(buildBuilder);
        imageBuilder
            .name(getImageName())
            .alias(getAlias())
                .buildConfig(buildBuilder.build());
        configs.add(imageBuilder.build());
        return configs;
    }

    /**
     * Hook for adding extra environment vars
     *
     * @return map with environment variables to use
     */
    protected Map<String, String> getEnv() {
        return new HashMap<>();
    }

    protected AssemblyConfiguration createAssembly() {
        return
            new AssemblyConfiguration.Builder()
                .basedir(getConfig(Config.baseDir))
                .descriptorRef(getAssemblyRef())
                .build();
    }

    protected String getAssemblyRef() {
        return getConfig(Config.assemblyRef);
    }

    protected List<String> extractPorts() {
        // TODO would rock to look at the base image and find the exposed ports!
        List<String> answer = new ArrayList<>();
        addPortIfValid(answer, getConfig(Config.webPort));
        addPortIfValid(answer, getConfig(Config.jolokiaPort));
        addPortIfValid(answer, getConfig(Config.prometheusPort));
        return answer;
    }

    @Override
    protected boolean shouldAddDefaultImage(List<ImageConfiguration> configs) {
        return super.shouldAddDefaultImage(configs) || Configs.asBoolean(getConfig(Config.enabled));
    }

    private void addPortIfValid(List<String> list, String port) {
        if (Strings.isNotBlank(port) && Integer.parseInt(port) != 0) {
            list.add(port);
        }
    }

    /**
     * Default selector for plain Java apps started with a run script
     */
    static class Java extends FromSelector.Default {

        private static final Properties defaultImageProps;

        public static final String JAVA_DEFAULT_IMAGES_PROPERTIES = "/META-INF/fabric8/java-default-images.properties";

        static {
            defaultImageProps = new Properties();
            try {
                defaultImageProps.load(Java.class.getResourceAsStream(JAVA_DEFAULT_IMAGES_PROPERTIES));
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot load default images properties " + JAVA_DEFAULT_IMAGES_PROPERTIES + ": " + e,e);
            }
        }

        public Java(MavenGeneratorContext context) {
            super(context,
                  getImageProp("generator.java.docker.upstream"),
                  getImageProp("generator.java.s2i.upstream"),
                  getImageProp("generator.java.docker.redhat"),
                  getImageProp("generator.java.s2i.redhat"));
        }

        private static String getImageProp(String key) {
            String val = defaultImageProps.getProperty(key);
            if (val == null) {
                throw new IllegalArgumentException("Cannot retrieve default value " + key + " from " + JAVA_DEFAULT_IMAGES_PROPERTIES);
            }
            return val;
        }
    }
}
