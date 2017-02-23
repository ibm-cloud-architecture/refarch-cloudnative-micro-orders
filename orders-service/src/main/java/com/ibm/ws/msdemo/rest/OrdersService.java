package com.ibm.ws.msdemo.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.ws.msdemo.rest.pojo.Order;

/**
 * This class handles post or get requests from UI application
 * and publishes order information to the Shipping application.
 *
 */
//Mapped to /orders via web.xml
@Path("/orders")
public class OrdersService {
	
	private UserTransaction utx;
	private EntityManager em;
	private static KafkaProducer<String, String> kafkaProducer;
	
    /** The topic on which the Orders app publishes data as needed */
    private static final String PUBLISH_TOPIC = "orders";
   
    /** Simple logging */
    private final static Logger logger = Logger.getLogger(OrdersService.class.getName());
    
	public OrdersService(){
		utx = getUserTransaction();
		em = getEm();

		logger.log(Level.INFO,"Initialising...");

        // Create Kafka connection
        if (System.getProperty("java.security.auth.login.config") == null) {
            System.setProperty("java.security.auth.login.config", "");
        }
        
        logger.log(Level.INFO,"Completed initialisation.");
	}
	
	private static Producer<String, String> getProducer() {
		// producer configuration is in /config/producer.properties
		if (kafkaProducer == null) {
			final Properties producerProperties = getClientConfiguration();
			
			// Initialise Kafka Producer
			kafkaProducer = new KafkaProducer<String, String>(producerProperties);
		}
        
        return kafkaProducer;
	}

	@Context UriInfo uriInfo;
	
	 /**
     * GET all orders
     * @return Response object
     */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response get(@HeaderParam("ibm-app-user") String user) {
		if (user == null) {
			// if caller header is not set, it's a bad request
			return Response.status(Status.BAD_REQUEST).build();
		}
	
		System.out.println("get order as " + user);
		// scope orders by customerId
		final List<Order> list = em.createQuery("SELECT t FROM Order t where t.customerId=:custId", Order.class)
				.setParameter("custId", user)
				.getResultList();
		
		final ObjectMapper mapper = new ObjectMapper();
		try {
			final String orderJSON = mapper.writeValueAsString(list);
			System.out.println(orderJSON);
			return Response.ok(orderJSON).build();
		} catch (JsonProcessingException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
     * GET a specific order
     * @param id
     * @return Order
     */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{id}")
	public Response get(@HeaderParam("ibm-app-user") String user, @PathParam("id") long id) {
		if (user == null) {
			// if caller header is not set, it's a bad request
			return Response.status(Status.BAD_REQUEST).build();
		}
		
		System.out.println("Searching for id : " + id + " as user " + user);
		Order order = null;
		try {
			order = em.find(Order.class, id);
			
			if (order != null && !order.getCustomerId().equals(user)) {
				// the caller doesn't own the order, return HTTP 401
				return Response.status(Status.UNAUTHORIZED).build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build();
		} 
		
		if (order != null) {
        	try {
				final ObjectMapper mapper = new ObjectMapper();
				final String orderJSON = mapper.writeValueAsString(order);

				System.out.println(orderJSON);
				return Response.ok(orderJSON).build();
			} catch (JsonProcessingException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		} else {
			return Response.status(Status.NOT_FOUND).build();
		}
	}
	
	/**
     * Create new order
     * @param order
     * @return id of created order
     */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response create(@HeaderParam("ibm-app-user") String user, Order order) {
		if (user == null) {
			// if caller header is not set, it's a bad request
			return Response.status(Status.BAD_REQUEST).build();
		}

		System.out.println("New order: " + order.toString());
		
		try {
			utx.begin();
			// set the customerId to the user
			order.setCustomerId(user);
			em.persist(order);
			utx.commit();
			
			//Notify Order information to the Shipping app to proceed. 
			notifyShipping(order);
			
			final UriBuilder builder = uriInfo.getAbsolutePathBuilder();
			builder.path(Long.toString(order.getId()));
			
			return Response.created(builder.build()).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			try {
				if (utx.getStatus() == javax.transaction.Status.STATUS_ACTIVE) {
					utx.rollback();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}


	}
	
	// There are two ways of obtaining the connection information for some services in Java 
	
	// Method 1: Auto-configuration and JNDI
	// The Liberty buildpack automatically generates server.xml configuration 
	// stanzas for the SQL Database service which contain the credentials needed to 
	// connect to the service. The buildpack generates a JNDI name following  
	// the convention of "jdbc/<service_name>" where the <service_name> is the 
	// name of the bound service. 
	// Below we'll do a JNDI lookup for the EntityManager whose persistence 
	// context is defined in web.xml. It references a persistence unit defined 
	// in persistence.xml. In these XML files you'll see the "jdbc/<service name>"
	// JNDI name used.

	private EntityManager getEm() {
		InitialContext ic;
		try {
			ic = new InitialContext();
			return (EntityManager) ic.lookup("java:comp/env/openjpa-order/entitymanager");
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return null;
	}

	// Method 2: Parsing VCAP_SERVICES environment variable
    // The VCAP_SERVICES environment variable contains all the credentials of 
	// services bound to this application. You can parse it to obtain the information 
	// needed to connect to the SQL Database service. SQL Database is a service
	// that the Liberty buildpack auto-configures as described above, so parsing
	// VCAP_SERVICES is not a best practice.
	
	// see HelloResource.getInformation() for an example
	
	private UserTransaction getUserTransaction() {
		InitialContext ic;
		try {
			ic = new InitialContext();
			return (UserTransaction) ic.lookup("java:comp/UserTransaction");
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Sending order information to the Shipping app
	 * @param order
	 */
    private void notifyShipping(Order order) {
        logger.log(Level.INFO,"Publishing order to shipping app: " + order);

        String fieldName = "order";
        // Push a message into the list to be sent.

        try {
        	
        	// get JSON object string
        	final ObjectMapper mapper = new ObjectMapper();
        	final String orderJSON = mapper.writeValueAsString(order);
        	
            // Create a producer record which will be sent
            // to the Message Hub service, providing the topic
            // name, field name and message. The field name and
            // message are converted to UTF-8.
            final ProducerRecord<String, String> record = new ProducerRecord<String, String>(PUBLISH_TOPIC,
                    fieldName, orderJSON);

            // Synchronously wait for a response from Message Hub / Kafka.
            final RecordMetadata m = getProducer().send(record).get();
            logger.log(Level.WARNING, "Message produced, offset: " + m.offset());

            logger.log(Level.INFO,"Publishing order, Done");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception sending message to Message Hub", e);
            e.printStackTrace();
        }
    }
    
    private static final Properties getClientConfiguration() {
        Properties props = new Properties();
        InputStream propsStream;
        String fileName = "/config"+ File.separator;

		fileName += "producer.properties";

        try {
            logger.log(Level.WARNING, "Reading properties file from: " + fileName);

            propsStream = new FileInputStream(fileName);
            props.load(propsStream);
            propsStream.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
            return props;
        }

        logger.log(Level.WARNING, "Using properties: " + props);

        return props;
    }

}
