package orders.messagehub;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import orders.config.MessageHubPropertiesBean;

@Component
public class MessageHubProducer {
    private static Logger logger =  LoggerFactory.getLogger(MessageHubProducer.class);
    
    @Autowired
    private MessageHubPropertiesBean messageHubProps;
    
	private KafkaProducer<String, String> kafkaProducer;
	
	private boolean isEnabled = false;
	
 
    @PostConstruct
    private void init() throws IOException {
    	// Check if any of the properties are not set.  if they are, then disable messagehub
    	if (StringUtils.isEmpty(messageHubProps.getServers())) {
    		logger.warn("Kafka brokers are not set; disabling MessageHub integration.");
    		return;
    	}
    	
    	if (StringUtils.isEmpty(messageHubProps.getTopic())) {
    		logger.warn("MessageHub topic is not set; disabling MessageHub integration.");
    		return;
    	}
    	
    	if (StringUtils.isEmpty(messageHubProps.getUser()) || StringUtils.isEmpty(messageHubProps.getPassword())) {
    		logger.warn("MessageHub credentials are not set; disabling MessageHub integration.");
    		return;
    	}
    	
    	// MessageHub is enabled
    	isEnabled = true;
    	
		// Creates JAAS configuration file to interact with Kafka servers securely
		// Also sets path to configuration file in Java properties

        /*
            This is what the jass.conf file should look like

            KafkaClient {
                org.apache.kafka.common.security.plain.PlainLoginModule required
                serviceName="kafka"
                username="USERNAME"
                password="PASSWORD";
            };
        */

        // Create JAAS file path
        final String jaas_file_path = System.getProperty("java.io.tmpdir") + "jaas.conf";

        // Set JAAS file path in Java settings
        System.setProperty("java.security.auth.login.config", jaas_file_path);

        // Build JAAS file contents
        final StringBuilder jaas = new StringBuilder();
        jaas.append("KafkaClient {\n");
        jaas.append("\torg.apache.kafka.common.security.plain.PlainLoginModule required\n");
        jaas.append("\tserviceName=\"kafka\"\n");
        jaas.append(String.format("\tusername=\"%s\"\n", messageHubProps.getUser()));
        jaas.append(String.format("\tpassword=\"%s\";\n", messageHubProps.getPassword()));
        jaas.append("};");

        // Write to JAAS file
        OutputStream jaasOutStream = null;

        try {
            jaasOutStream = new FileOutputStream(jaas_file_path, false);
            jaasOutStream.write(jaas.toString().getBytes(Charset.forName("UTF-8")));
            System.out.println("Successfully wrote to JAAS configuration file: " + jaas_file_path);
        } catch (final IOException e) {
            System.out.println("Error: Failed accessing to JAAS config file:");
            System.out.println(e);
            throw e;
        } finally {
            if (jaasOutStream != null) {
                try {
                    jaasOutStream.close();
                    System.out.println("Closed JAAS file");
                } catch (final Exception e) {
                    System.out.println("Error closing generated JAAS config file:");
                    System.out.println(e);
                }
            }
        }
    }
    
	public boolean isEnabled() {
		return isEnabled;
	}

    private final Properties getClientConfiguration() {
        final Properties props = new Properties();

        try {
            final InputStream propsStream = this.getClass().getClassLoader().getResourceAsStream("producer.properties");
            props.load(propsStream);
            propsStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return props;
        }
        
        // use the list of servers from the spring configuration
        props.setProperty("bootstrap.servers", messageHubProps.getServers());

        logger.info("Using properties: " + props);

        return props;
    }
    
	private Producer<String, String> getProducer() {
		// producer configuration is in /config/producer.properties
		if (kafkaProducer == null) {
			final Properties producerProperties = getClientConfiguration();
			
			// Initialise Kafka Producer
			kafkaProducer = new KafkaProducer<String, String>(producerProperties);
		}
        
        return kafkaProducer;
	}
	
	public void writeMessage(String topicName, String fieldName, String msg) throws InterruptedException, ExecutionException {
		// Create a producer record which will be sent
		// to the Message Hub service, providing the topic
		// name, field name and message. The field name and
		// message are converted to UTF-8.
		final ProducerRecord<String, String> record = new ProducerRecord<String, String>(topicName,
				fieldName, msg);

		// Synchronously wait for a response from Message Hub / Kafka.
		final RecordMetadata m = this.getProducer().send(record).get();
		logger.info("Message produced, offset: " + m.offset());
	}

}
