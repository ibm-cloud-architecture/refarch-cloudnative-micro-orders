package orders;

import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import orders.messagehub.MessageHubProducer;
import orders.model.Order;

/**
 * REST Controller to manage Customer database
 *
 */
@RestController
public class OrdersController {
    
    private static Logger logger =  LoggerFactory.getLogger(OrdersController.class);
    private static final String PUBLISH_TOPIC = "orders";
    
    @Autowired
    private OrdersRepository ordersRepo;
    
    @Autowired
    private MessageHubProducer producer;
    
    /**
     * check
     */
    @RequestMapping("/check")
    protected @ResponseBody String check() {
        return "it works!";
    }
    
    /**
     * @return customer by username
     */
    @RequestMapping(value = "/orders", method = RequestMethod.GET)
    protected @ResponseBody ResponseEntity<?> getOrders() {
        try {
         	final String customerId = getCustomerId();
        	if (customerId == null) {
        		// if no user passed in, this is a bad request
        		return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
        	}
        	
        	logger.debug("caller: " + customerId);
            
        	final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);
        	
            return  ResponseEntity.ok(orders);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    private String getCustomerId() {
    	final SecurityContext ctx = SecurityContextHolder.getContext();
    	if (ctx.getAuthentication() == null) {
    		return null;
    	};
    	
    	if (!ctx.getAuthentication().isAuthenticated()) {
    		return null;
    	}
    	
    	final OAuth2Authentication oauth = (OAuth2Authentication)ctx.getAuthentication();
    	
    	logger.debug("CustomerID: " + oauth.getName());
    	
    	return oauth.getName();
    }

    /**
     * @return customer by id
     */
    @RequestMapping(value = "/orders/{id}", method = RequestMethod.GET)
    protected ResponseEntity<?> getById(@RequestHeader Map<String, String> headers, @PathVariable String id) {
		final String customerId = getCustomerId();
		if (customerId == null) {
			// if no user passed in, this is a bad request
			return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
		}
		
		logger.debug("caller: " + customerId);
		
		final Order order = ordersRepo.findOne(id);
		
		if (order.getCustomerId().equals(customerId)) {
			return ResponseEntity.ok(order);
		}
		
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + id + " not found");
	}

    /**
     * Add order
     * @return transaction status
     */
    @RequestMapping(value = "/orders", method = RequestMethod.POST, consumes = "application/json")
    protected ResponseEntity<?> create(@RequestBody Order payload) {
        try {
			
    		final String customerId = getCustomerId();
			if (customerId == null) {
				// if no user passed in, this is a bad request
				return ResponseEntity.badRequest().body("Invalid Bearer Token: Missing customer ID");
			}
	         
			payload.setDate(Calendar.getInstance().getTime());
			payload.setCustomerId(customerId);
    		System.out.println("New order: " + payload.toString());
			
			ordersRepo.save(payload);
			
			final URI location =  ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(payload.getId()).toUri();
			
			notifyShipping(payload);
			
			return ResponseEntity.created(location).build();
        } catch (Exception ex) {
            logger.error("Error creating order: " + ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating order: " + ex.toString());
        }
        
    }

    /**
     * @return Circuit breaker tripped
     */
    @HystrixCommand(fallbackMethod="failGood")
    @RequestMapping("/circuitbreaker")
    @ResponseBody
    public String tripCircuitBreaker() {
        System.out.println("Circuitbreaker Service invoked");
        return "";
    }
    
   
	/**
	 * Sending order information to the Shipping app
	 * @param order
	 */
    private void notifyShipping(Order order) {
    	if (!producer.isEnabled()) {
    		return;
    	}
    	
        logger.info("Publishing order to shipping app: " + order);

        String fieldName = "order";
        // Push a message into the list to be sent.

        try {
        	// get JSON object string
        	final ObjectMapper mapper = new ObjectMapper();
        	final String orderJSON = mapper.writeValueAsString(order);
        	
        	producer.writeMessage(PUBLISH_TOPIC, fieldName, orderJSON);
            logger.info("Publishing order, Done");
            
        } catch (Exception e) {
            logger.error("Exception sending message to Message Hub", e);
            e.printStackTrace();
        }
    }
 
}
