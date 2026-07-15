package cn.sdu.radar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiApplicationTest {

    @Test
    void startsWithoutADataSource() {
        new WebApplicationContextRunner()
                .withUserConfiguration(AiApplication.class)
                .withPropertyValues(
                        "spring.cloud.nacos.discovery.enabled=false",
                        "spring.cloud.nacos.config.enabled=false",
                        "ai.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
