package orders.test;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import orders.model.Order;

public class OrdersJSONTest {
	
	
	@Test
	public void testMarshalToJson() throws Exception {
		final Order inv = new Order();
		final Random rnd = new Random();
		
		String id = UUID.randomUUID().toString();
		String customerId = UUID.randomUUID().toString();
		int stock = rnd.nextInt();
		int itemId = rnd.nextInt();
		Date currDate = Calendar.getInstance().getTime();
		
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
		
		inv.setDate(currDate);
		inv.setItemId(itemId);
		inv.setId(id);
		inv.setCustomerId(customerId);
		inv.setCount(stock);
		
		final String json = mapper.writeValueAsString(inv);
		
		// construct a json string with the above properties
		
		final StringBuilder myJsonStr = new StringBuilder();
		
		myJsonStr.append("{");
		myJsonStr.append("\"id\":").append("\"").append(id).append("\"").append(",");
		myJsonStr.append("\"itemId\":").append(itemId).append(",");
		myJsonStr.append("\"date\":").append(currDate.getTime()).append(",");
		myJsonStr.append("\"customerId\":").append("\"").append(customerId).append("\"").append(",");
		myJsonStr.append("\"count\":").append(stock);
		myJsonStr.append("}");
		
		final String myJson = myJsonStr.toString();
		System.out.println("Marshalled Order to JSON:" + json);
		System.out.println("My JSON String:" + myJson);
		
		final JsonNode jsonObj = mapper.readTree(json);
		final JsonNode myJsonObj = mapper.readTree(myJson);
		
		
		assert(jsonObj.equals(myJsonObj));
		
		
	}
	
	@Test
	public void testMarshalFromJson() throws Exception {
		final Random rnd = new Random();
		
		String id = UUID.randomUUID().toString();
		String customerId = UUID.randomUUID().toString();
		int stock = rnd.nextInt();
		int itemId = rnd.nextInt();
		Date currDate = Calendar.getInstance().getTime();
		
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
		
		// construct a json string with the above properties
		
		final StringBuilder myJsonStr = new StringBuilder();
		
		myJsonStr.append("{");
		myJsonStr.append("\"id\":").append("\"").append(id).append("\"").append(",");
		myJsonStr.append("\"itemId\":").append(itemId).append(",");
		myJsonStr.append("\"date\":").append(currDate.getTime()).append(",");
		myJsonStr.append("\"customerId\":").append("\"").append(customerId).append("\"").append(",");
		myJsonStr.append("\"count\":").append(stock);
		myJsonStr.append("}");
		
		final String myJson = myJsonStr.toString();
		System.out.println("My JSON String:" + myJson);
		
		// marshall json to Order object
		
		final Order inv = mapper.readValue(myJson, Order.class);
		
		// make sure all the properties match up
		assert(inv.getId().equals(id));
		assert(inv.getItemId() == itemId);
		assert(inv.getDate().equals(currDate));
		assert(inv.getCustomerId().equals(customerId));
		assert(inv.getCount() == stock);
		
		
	}
}