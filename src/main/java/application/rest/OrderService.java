package application.rest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import com.google.gson.Gson;
import com.ibm.websphere.security.openidconnect.PropagationHelper;
import com.ibm.websphere.security.openidconnect.token.IdToken;

import config.JwtConfig;
import model.Order;
import utils.OrderDAOImpl;


@Path("/orders")
public class OrderService {
	
	@GET
	 @Path("/check")
	 @Produces("application/json")
	 public String check() {
	 return "it works!";
	}
	
	@GET
	 @Produces("application/json")
	 public Response getOrders() {
		
		JwtConfig jwt = getJwt();
		
		//String orderDetails = null;
		
		try {
         	final String customerId = jwt.getUser_name();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		// return "Invalid Bearer Token: Missing customer ID";
        		return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID").build();
        	}
        	
        	System.out.println("caller: " + customerId);
        	
        	OrderDAOImpl ordersRepo = new OrderDAOImpl();
            
        	final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);
        	
        	//Gson gson = new Gson();
        	//orderDetails = gson.toJson(orders);
   		    //return orderDetails;
        	
        	return Response.ok(orders).build();
            
        } catch (Exception e) {
            System.err.println(e.getMessage() +""+ e);
            // Include Http client later
            // return "Status not found";
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
	 
	 }
	
	@GET
	 @Path("{id}")
	 @Produces("application/json")
	public Response getById(@PathParam("id") String id) {
		
		JwtConfig jwt = getJwt();
        
		//String orderDetails = null;
		
		try {
         	final String customerId = jwt.getUser_name();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		// return "Invalid Bearer Token: Missing customer ID";
        		return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID").build();
        	}
        	
        	System.out.println("caller: hello " + id);
        	
        	OrderDAOImpl ordersRepo = new OrderDAOImpl();
        	
        	System.out.println("repo initiated");
            
        	final List<Order> orders = ordersRepo.findByOrderId(id);
        	List<Order> findByOrderId = new ArrayList<>();
        	
        	for (Order temp : orders) {
        		System.out.println("temp cust id"+temp.getCustomerId());
        		System.out.println("our cust id"+customerId);
    			if(temp.getCustomerId().equals(customerId))
    			{
    				findByOrderId.add(temp);
    			}
    				
    		} 
        	
        	//Gson gson = new Gson();
        	//orderDetails = gson.toJson(findByOrderId);
   		    //return orderDetails;
        	
        	return Response.ok(findByOrderId).build();
            
        } catch (Exception e) {
            System.err.println(e.getMessage() +""+ e);
            // Include Http client later
            // return "Status not found";
            return Response.status(Response.Status.NOT_FOUND).build();
        }
	}
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public Response create(Order payload, @Context UriInfo uriInfo) {
		
		JwtConfig jwt = getJwt();
		
        try {
			
    		final String customerId = jwt.getUser_name();
			if (customerId == null) {
				// if no user passed in, this is a bad request
				//return "Invalid Bearer Token: Missing customer ID";
				return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID").build();
			}
	         
			payload.setDate(Calendar.getInstance().getTime());
			payload.setCustomerId(customerId);
			
			String id = UUID.randomUUID().toString();
			
			payload.setId(id);
			
			System.out.println("New order: " + payload.toString());
			
    		OrderDAOImpl ordersRepo = new OrderDAOImpl();
			ordersRepo.putOrderDetails(payload);
			
			//return payload.toString() + " created";
			
            return Response.status(Response.Status.OK).entity(payload +" posted").build();
			
        } catch (Exception ex) {
            System.err.println("Error creating order: " + ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating order: " + ex.toString()).build();
        }
        
    }
	
	private JwtConfig getJwt(){
    	IdToken token = PropagationHelper.getIdToken();
    	String claims = token.getAllClaimsAsJson();
        Gson g = new Gson();
        JwtConfig jwt = g.fromJson(claims, JwtConfig.class);
    	return jwt;
    }

}
