package application.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.google.gson.Gson;

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
	 public String getOrders() {
		
		String orderDetails = null;
		
		try {
         	final String customerId = getCustomerId();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		return "Invalid Bearer Token: Missing customer ID";
        	}
        	
        	System.out.println("caller: " + customerId);
        	
        	OrderDAOImpl ordersRepo = new OrderDAOImpl();
            
        	final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);
        	
        	Gson gson = new Gson();
        	orderDetails = gson.toJson(orders);
   		    return orderDetails;
            
        } catch (Exception e) {
            System.err.println(e.getMessage() +""+ e);
            // Include Http client later
            return "Status not found";
        }
	 
	 }
	
	@GET
	 @Path("{id}")
	 @Produces("application/json")
	public String getById(@PathParam("id") String id) {
        
		String orderDetails = null;
		
		try {
         	final String customerId = getCustomerId();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		return "Invalid Bearer Token: Missing customer ID";
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
        	
        	Gson gson = new Gson();
        	orderDetails = gson.toJson(findByOrderId);
   		    return orderDetails;
            
        } catch (Exception e) {
            System.err.println(e.getMessage() +""+ e);
            // Include Http client later
            return "Status not found";
        }
	}
	
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public String create(Order payload) {
        try {
			
    		final String customerId = getCustomerId();
			if (customerId == null) {
				// if no user passed in, this is a bad request
				return "Invalid Bearer Token: Missing customer ID";
			}
	         
			payload.setDate(Calendar.getInstance().getTime());
			payload.setCustomerId(customerId);
    		
			System.out.println("New order: " + payload.toString());
			
    		OrderDAOImpl ordersRepo = new OrderDAOImpl();
			ordersRepo.putOrderDetails(payload);
			
			return payload.toString() + " created";
			
        } catch (Exception ex) {
            System.err.println("Error creating order: " + ex);
            return "Error creating order: " + ex.toString();
        }
        
    }
	
	private String getCustomerId() {
		//to be replaced by the customer using the security context
		return "1";
    }

}
