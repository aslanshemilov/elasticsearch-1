/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.security.ingest;

import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.core.security.SecurityContext;
import org.elasticsearch.xpack.security.ingest.SetSecurityUserProcessor.Property;
import org.junit.Before;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.test.TestMatchers.throwableWithMessage;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class SetSecurityUserProcessorFactoryTests extends ESTestCase {

    private SecurityContext securityContext;

    @Before
    public void setupContext() {
        securityContext = new SecurityContext(Settings.EMPTY, new ThreadContext(Settings.EMPTY));
    }

    public void testProcessor() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(() -> securityContext);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        SetSecurityUserProcessor processor = factory.create(null, "_tag", config);
        assertThat(processor.getField(), equalTo("_field"));
        assertThat(processor.getProperties(), equalTo(EnumSet.allOf(Property.class)));
    }

    public void testProcessor_noField() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(() -> securityContext);
        Map<String, Object> config = new HashMap<>();
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> factory.create(null, "_tag", config));
        assertThat(e.getMetadata("es.property_name").get(0), equalTo("field"));
        assertThat(e.getMetadata("es.processor_type").get(0), equalTo(SetSecurityUserProcessor.TYPE));
        assertThat(e.getMetadata("es.processor_tag").get(0), equalTo("_tag"));
    }

    public void testProcessor_validProperties() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(() -> securityContext);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        config.put("properties", Arrays.asList(Property.USERNAME.name(), Property.ROLES.name()));
        SetSecurityUserProcessor processor = factory.create(null, "_tag", config);
        assertThat(processor.getField(), equalTo("_field"));
        assertThat(processor.getProperties(), equalTo(EnumSet.of(Property.USERNAME, Property.ROLES)));
    }

    public void testProcessor_invalidProperties() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(() -> securityContext);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        config.put("properties", Arrays.asList("invalid"));
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> factory.create(null, "_tag", config));
        assertThat(e.getMetadata("es.property_name").get(0), equalTo("properties"));
        assertThat(e.getMetadata("es.processor_type").get(0), equalTo(SetSecurityUserProcessor.TYPE));
        assertThat(e.getMetadata("es.processor_tag").get(0), equalTo("_tag"));
    }

    public void testNullSecurityContextThrowsException() throws Exception {
        SetSecurityUserProcessor.Factory factory = new SetSecurityUserProcessor.Factory(() -> null);
        Map<String, Object> config = new HashMap<>();
        config.put("field", "_field");
        NullPointerException e = expectThrows(NullPointerException.class, () -> factory.create(null, "_tag", config));
        assertThat(e, throwableWithMessage(containsString("security context")));
    }

}
