package application.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.jwt.JsonWebToken;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import model.Order;
import utils.OrderDAOImpl;

@DeclareRoles({"Admin", "User"})
@RequestScoped
@Path("/orders")
public class OrderService {

    @Inject
    private JsonWebToken jwt;

    private final static String QueueName = "stock";

    @GET
    @Path("/check")
    @Produces(MediaType.TEXT_PLAIN)
    public String check() {
        return "it works!";
    }

    @GET
    @Produces("application/json")
    @Fallback(fallbackMethod = "returnEmptyOrdersList")
    public Response getOrders() {

        try {
            final String customerId = jwt.getName();
            if (customerId == null) {
                // if no user passed in, this is a bad request
                // return "Invalid Bearer Token: Missing customer ID";
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID").build();
            }

            System.out.println("caller: " + customerId);

            OrderDAOImpl ordersRepo = new OrderDAOImpl();

            final List<Order> orders = ordersRepo.findByCustomerIdOrderByDateDesc(customerId);

            return Response.ok(orders).build();

        } catch (Exception e) {
            System.err.println(e.getMessage() + "" + e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Fallback(fallbackMethod = "returnEmptyOrdersByIdList")
    public Response getById(@PathParam("id") String id) {

        try {
            final String customerId = jwt.getName();
            if (customerId == null) {
                // if no user passed in, this is a bad request
                // return "Invalid Bearer Token: Missing customer ID";
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Bearer Token: Missing customer ID").build();
            }

            OrderDAOImpl ordersRepo = new OrderDAOImpl();

            final List<Order> orders = ordersRepo.findByOrderId(id);
            List<Order> findByOrderId = new ArrayList<>();

            for (Order temp : orders) {
                if (temp.getCustomerId().equals(customerId)) {
                    findByOrderId.add(temp);
                }

            }

            return Response.ok(findByOrderId).build();

        } catch (Exception e) {
            System.err.println(e.getMessage() + "" + e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(Order payload, @Context UriInfo uriInfo) throws IOException, TimeoutException {

        try {

            final String customerId = jwt.getName();
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

            UriBuilder builder = uriInfo.getAbsolutePathBuilder();
            builder.path(payload.getId());
            System.out.println(builder.build().toString());

            //Using RabbitMQ to update stock

            notifyShipping(payload);

            return javax.ws.rs.core.Response.created(builder.build()).build();

        } catch (Exception ex) {
            System.err.println("Error creating order: " + ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating order: " + ex.toString()).build();
        }

    }

    private void notifyShipping(Order payload) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        Config config = ConfigProvider.getConfig();
        String rabbit_host = config.getValue("rabbit", String.class);
        factory.setHost(rabbit_host);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QueueName, false, false, false, null);

        String id = Integer.toString(payload.getItemId());
        String stock = Integer.toString(payload.getCount());
        String update = id + " " + stock;
        channel.basicPublish("", QueueName, null, update.getBytes());
        System.out.println("Sent the message '" + update + "'");
        String inv_url = config.getValue("inventory_url", String.class);
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(inv_url);
        String s = target.request().get(String.class);
        System.out.println(s);

        channel.close();
        connection.close();
    }

    @Produces(MediaType.APPLICATION_JSON)
    public Response returnEmptyOrdersList() {
        return Response.ok(new ArrayList<>()).build();
    }

    // TODO: Find a better solution to write two different fallback methods in Orders.
    // Second method required to match fallback with different parameters
    @Produces(MediaType.APPLICATION_JSON)
    public Response returnEmptyOrdersByIdList(@PathParam("id") String id) {
        return Response.ok(new ArrayList<>()).build();
    }
}
