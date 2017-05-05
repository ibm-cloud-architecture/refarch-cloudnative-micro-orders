package orders.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "spring.application.messagehub")
public class MessageHubPropertiesBean {
    private String topic;
    private String user;
    private String password;
    private String api_key;
    private String kafka_rest_url;
    private List<String> kafka_brokers_sasl = new ArrayList<String>();

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getApi_key() {
        return api_key;
    }

    public void setApi_key(String api_key) {
        this.api_key = api_key;
    }

    public String getKafka_rest_url() {
        return kafka_rest_url;
    }

    public void setKafka_rest_url(String kafka_rest_url) {
        this.kafka_rest_url = kafka_rest_url;
    }

    public List<String> getKafka_brokers_sasl() {
        return kafka_brokers_sasl;
    }

    public void setKafka_brokers_sasl(List<String> kafka_brokers_sasl) {
        this.kafka_brokers_sasl = kafka_brokers_sasl;
    }

    public String getServers() {
        return StringUtils.join(kafka_brokers_sasl, ',');
    }
}
