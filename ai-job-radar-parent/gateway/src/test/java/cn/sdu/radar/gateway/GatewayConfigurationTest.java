package cn.sdu.radar.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayConfigurationTest {

    @Test
    void shouldExposeEveryBusinessServiceThroughGateway() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("bootstrap.yml"));
        Properties properties = yaml.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("server.port")).isEqualTo("9000");
        assertThat(properties).containsValue("lb://user-service");
        assertThat(properties).containsValue("lb://job-service");
        assertThat(properties).containsValue("lb://match-service");
        assertThat(properties).containsValue("lb://application-service");
        assertThat(properties).containsValue("lb://interview-service");
        assertThat(properties).containsValue("lb://notification-service");
    }
}
