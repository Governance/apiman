/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.apiman.manager.test;

import io.apiman.manager.test.server.MockGatewayServlet;
import io.apiman.manager.test.util.AbstractTestPlanTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Runs the "all" test plan.
 *
 * @author eric.wittmann@redhat.com
 */
@SuppressWarnings("nls")
public class AllTest extends AbstractTestPlanTest {

    private static final String EXPECTED_GATEWAY_LOG = 
            "GET:/mock-gateway/system/status\n" + 
            "PUT:/mock-gateway/services\n" +
            "GET:/mock-gateway/system/status\n" + 
            "PUT:/mock-gateway/applications\n";

    @Test
    public void test() {
        runTestPlan("test-plans/all-testPlan.xml", AllTest.class.getClassLoader());

        // This test includes publishing of a service to the gateway REST API.  The
        // test framework incldues a mock gateway API to test that the REST calls were
        // properly make.  Here is where we assert the result.
        String actualGatewayLog = MockGatewayServlet.getRequestLog();
        Assert.assertEquals(EXPECTED_GATEWAY_LOG, actualGatewayLog);
    }

}
