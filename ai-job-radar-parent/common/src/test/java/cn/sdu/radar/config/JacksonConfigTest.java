package cn.sdu.radar.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonConfigTest {
    @Test
    void serializesLongIdsAsStringsForJavaScriptSafety() throws Exception {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().longToStringCustomizer().customize(builder);
        ObjectMapper mapper = builder.build();

        String json = mapper.writeValueAsString(
                Collections.singletonMap("id", 2076675828967198721L));

        assertEquals("{\"id\":\"2076675828967198721\"}", json);
    }
}
