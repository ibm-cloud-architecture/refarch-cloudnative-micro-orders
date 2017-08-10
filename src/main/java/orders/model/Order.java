package orders.model;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "orders")
/*
 * define O-R mapping of order table
 */
public class Order {

	@Id //primary key
	@Column(name = "orderId")
	@GeneratedValue(generator="uuid-generator")
	@GenericGenerator(name="uuid-generator", strategy="uuid")
	String id;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "date")
	Date date;
	
	@Basic
	@Column(name = "itemId")
	int itemId;

	@Basic
	@Column(name = "customerId")
	String customerId;
	
	@Basic
	@Column(name = "count")
	int count;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
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
		return "{id = " + id + ", itemId=" + itemId + ", date=" + date.toString() + ", customerId=" + customerId
				+ ", count=" + count + "}";
	}


	
}
