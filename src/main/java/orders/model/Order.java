package orders.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "orders")
/*
 * define O-R mapping of order table
 */
public class Order {

	@Id //primary key
	@Column(name = "orderId")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	long id;
	
	@Basic
	@Column(name = "itemId")
	int itemId;
	
	@Basic
	@Column(name = "customerId")
	String customerId;
	
	@Basic
	@Column(name = "count")
	int count;
	
	public long getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getItemId() {
		return itemId;
	}
	public void setItemId(int itemId) {
		this.itemId = itemId;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customer_id) {
		this.customerId = customer_id;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	@Override
	public String toString() {
		return "{id = " + id + ", itemId=" + itemId + ", customerId=" + customerId
				+ ", count=" + count + "}";
	}


	
}
