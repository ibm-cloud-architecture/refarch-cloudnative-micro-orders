package orders;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import orders.model.Order;

public interface OrdersRepository extends CrudRepository<Order, String> {
	List<Order> findByCustomerIdOrderByDateDesc(String customerId);
}
