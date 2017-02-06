package com.ibm.ws.msdemo.rest;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.msdemo.rest.pojo.Order;

@Path("/health")
public class HealthCheckService {

    /** Simple logging */
    private final static Logger logger = Logger.getLogger(HealthCheckService.class.getName());
 
	 /**
     * GET health
     * @return Response object
     */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        logger.log(Level.INFO, "health check");
        String json = "{\"status\": \"UP\"}";
        return Response.ok(json).build();
    }
	
}
