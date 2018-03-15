package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import model.Order;



public class OrderDAOImpl {
	
   public List<Order> findByCustomerIdOrderByDateDesc(String customerId)
     {
		
		List<Order> orderData = new ArrayList<>();
		
		JDBCConnection jdbcConnection = new JDBCConnection();
		
		Connection connection = jdbcConnection.getConnection();

		try {
		 PreparedStatement ps = connection.prepareStatement(
		 "select orderId,itemId,customerId,count,date from ordersdb.orders where customerId=?");
		 ps.setString(1, customerId);
		 ResultSet rs = ps.executeQuery();

		while (rs.next()) {
		 Order order = new Order();
		 order.setId(rs.getString("orderId"));
		 order.setItemId(rs.getInt("itemId"));
		 order.setCustomerId(rs.getString("customerId"));
		 order.setCount(rs.getInt("count"));
		 order.setDate(rs.getTimestamp("date"));
		 orderData.add(order);

		}

		} catch (SQLException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		 }

		return orderData;
      }
   

   public List<Order> findByOrderId(String id)
     {
	    System.out.println("I am in"); 
	   
	    List<Order> orderData = new ArrayList<>();
		
		JDBCConnection jdbcConnection = new JDBCConnection();
		
		Connection connection = jdbcConnection.getConnection();

		try {
		 PreparedStatement ps = connection.prepareStatement(
		 "select orderId,itemId,customerId,count,date from ordersdb.orders where orderId=?");
		 ps.setString(1, id);
		 ResultSet rs = ps.executeQuery();

		     while (rs.next()) {
		      Order order = new Order();
		      order.setId(rs.getString("orderId"));
		      order.setItemId(rs.getInt("itemId"));
		      order.setCustomerId(rs.getString("customerId"));
		      order.setCount(rs.getInt("count"));
		      order.setDate(rs.getTimestamp("date"));
		      System.out.println("added");
		      orderData.add(order);
             }
        } 
		catch (SQLException e) {
		 // TODO Auto-generated catch block
		 e.printStackTrace();
		}
	
		return orderData;
     }

   public void putOrderDetails(Order order){

	JDBCConnection jdbcConnection = new JDBCConnection();
	
	Connection connection = jdbcConnection.getConnection();
	try
    {
      String query = " insert into orders (orderId,itemId,customerId,count,date)"
        + " values (?, ?, ?, ?, ?)";

      PreparedStatement preparedStmt = connection.prepareStatement(query);
      preparedStmt.setString(1, order.getId());
      preparedStmt.setInt(2, order.getItemId());
      preparedStmt.setString(3, order.getCustomerId());
      preparedStmt.setInt(4, order.getCount());
      preparedStmt.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
      
      preparedStmt.execute();
      
      connection.close();
    }
    catch (Exception e)
    {
      System.err.println("Got an exception!");
      System.err.println(e.getMessage());
    }
}

}

