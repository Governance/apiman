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

package io.apiman.gateway.api.rest;

import io.apiman.common.logging.ApimanLoggerFactory;
import io.apiman.gateway.engine.beans.GatewayEndpoint;
import io.apiman.gateway.engine.beans.LoggingChangeRequest;
import io.apiman.gateway.engine.beans.SystemStatus;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import io.swagger.annotations.Api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The System API.
 *
 * @author eric.wittmann@redhat.com
 */
@Path("system")
@Api
public interface ISystemResource {

    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_JSON)
    SystemStatus getStatus();

    @GET
    @Path("endpoint")
    @Produces(MediaType.APPLICATION_JSON)
    GatewayEndpoint getEndpoint();

    @POST
    @Path("logging")
    default Response setLoggingDynamically(LoggingChangeRequest request) {
        if (request.getLoggerConfig() != null) {
            try {
                File loggerConfigTmp = File.createTempFile("ApimanLoggerConfig", "temp");
                loggerConfigTmp.deleteOnExit();
                Files.write(loggerConfigTmp.toPath(), request.getLoggerConfig());
                ApimanLoggerFactory.overrideLoggerConfig(loggerConfigTmp);
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }
        return Response.ok().build();
    }
}
