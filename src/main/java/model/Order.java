package model;

import java.util.Date;

public class Order {
	
	String id;
	
	Date date;
	
	int itemId;

	String customerId;
	
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
