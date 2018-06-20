package application.rest;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeoutException;

import javax.annotation.security.DeclareRoles;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
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

    @Timeout(value = 2, unit = ChronoUnit.SECONDS)
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Fallback(fallbackMethod = "returnDummyOrder")
    public Response getOrders() throws Exception {
        try {
            System.out.println("I am in getOrders");
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
            System.err.println("Entering the Fallback Method.");
            throw new Exception(e.toString());
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
    public Response returnDummyOrder() {
        Order order = new Order();
        order.setId("999");
        order.setItemId(999);
        order.setCustomerId("999");
        order.setCount(-1);

        Date time = new GregorianCalendar(1776, Calendar.JULY, 5).getTime();
        order.setDate(time);

        List<Order> fakeOrderData = new ArrayList<>();
        fakeOrderData.add(order);

        return Response.ok(fakeOrderData).build();
    }
}
