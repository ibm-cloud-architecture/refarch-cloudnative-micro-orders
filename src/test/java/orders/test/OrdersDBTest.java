package orders.test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import orders.OrdersRepository;
import orders.model.Order;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class OrdersDBTest {
	
	@Resource 
	private OrdersRepository ordersRepo;
	
	@Test
	public void insertOrderTest() throws Exception {
		final Order newOrder = new Order();
		
		final Calendar now = Calendar.getInstance();
		newOrder.setCustomerId("abcd");
		newOrder.setItemId(13401);
		newOrder.setDate(now.getTime());
		newOrder.setCount(1);
		final Order o = ordersRepo.save(newOrder);
		
		assert(ordersRepo.count() == 1);
		
		final Order savedOrder = ordersRepo.findOne(o.getId());
		
		System.out.println("Saved order = " + savedOrder);
		
		assert(savedOrder.getCustomerId().equals(newOrder.getCustomerId()));
		assert(savedOrder.getCount() == newOrder.getCount());
		assert(savedOrder.getItemId() == newOrder.getItemId());
		assert(savedOrder.getDate().equals(newOrder.getDate()));
	}
	
	@Test
	public void orderByCustomerTest() throws Exception {
		final Random rnd = new Random();
		
		for (int i = 0; i < 5; i++) {
			final Order newOrder = new Order();

			final Calendar now = Calendar.getInstance();
			now.add(Calendar.HOUR_OF_DAY, rnd.nextInt(23));
			now.add(Calendar.MINUTE, rnd.nextInt(59));
			newOrder.setCustomerId("abcd" + i);
			newOrder.setItemId(13401);
			newOrder.setDate(now.getTime());
			newOrder.setCount(i);
			ordersRepo.save(newOrder);
		}
		
		assert(ordersRepo.count() == 5);
		
		final List<Order> savedOrders = ordersRepo.findByCustomerIdOrderByDateDesc("abcd" + 3);
		assert(savedOrders.size() == 1);
		
		System.out.println("Saved orders count = " + savedOrders.size());
		
		assert(savedOrders.get(0).getCustomerId().equals("abcd3"));
		assert(savedOrders.get(0).getCount() == 3);
		assert(savedOrders.get(0).getItemId() == 13401);
	}
	
	@Test
	public void orderSortTest() throws Exception {
		final Random rnd = new Random();
		
		for (int i = 0; i < 5; i++) {
			final Order newOrder = new Order();

			final Calendar now = Calendar.getInstance();
			now.add(Calendar.HOUR_OF_DAY, rnd.nextInt(23));
			now.add(Calendar.MINUTE, rnd.nextInt(59));
			newOrder.setCustomerId("abcd");
			newOrder.setItemId(13401);
			newOrder.setDate(now.getTime());
			newOrder.setCount(1);
			ordersRepo.save(newOrder);
		}
		
		assert(ordersRepo.count() == 5);
		
		final List<Order> savedOrders = ordersRepo.findByCustomerIdOrderByDateDesc("abcd");
		
		System.out.println("Saved orders = " + savedOrders.size());
		assert(savedOrders.size() == 5);
		
		Date lastDate = null;
		for (int i = 0; i < 5; i++) {
			final Date currDate = savedOrders.get(i).getDate();
			
			if (lastDate == null) {
				lastDate = currDate;
				continue;
			}
			
			// make sure sorted order
			assert(currDate.before(lastDate));
			lastDate = currDate;
			
		}
		
	}
}
