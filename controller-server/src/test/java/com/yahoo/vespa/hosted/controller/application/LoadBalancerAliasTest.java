// Copyright 2019 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.controller.application;

import com.yahoo.config.provision.ApplicationId;
import com.yahoo.config.provision.ClusterSpec;
import com.yahoo.vespa.hosted.controller.api.integration.zone.ZoneId;
import org.junit.Test;

import static com.yahoo.vespa.hosted.controller.application.LoadBalancerAlias.createAlias;
import static org.junit.Assert.*;

/**
 * @author mpolden
 */
public class LoadBalancerAliasTest {

    @Test
    public void test_endpoint_names() {
        ZoneId zoneId = ZoneId.from("prod", "us-north-1");
        ApplicationId withInstanceName = ApplicationId.from("tenant", "application", "instance");
        testAlias("instance--application--tenant--prod.us-north-1.vespa.oath.cloud", "default", withInstanceName, zoneId);
        testAlias("cluster--instance--application--tenant--prod.us-north-1.vespa.oath.cloud", "cluster", withInstanceName, zoneId);

        ApplicationId withDefaultInstance = ApplicationId.from("tenant", "application", "default");
        testAlias("application--tenant--prod.us-north-1.vespa.oath.cloud", "default", withDefaultInstance, zoneId);
        testAlias("cluster--application--tenant--prod.us-north-1.vespa.oath.cloud", "cluster", withDefaultInstance, zoneId);
    }

    private void testAlias(String expected, String clusterName, ApplicationId applicationId, ZoneId zoneId) {
        assertEquals(expected, createAlias(ClusterSpec.Id.from(clusterName), applicationId, zoneId));
    }

}
