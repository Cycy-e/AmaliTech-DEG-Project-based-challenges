package com.finsafe.gateway;

import com.finsafe.gateway.service.PaymentProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "payment.processing-delay-ms=500")
@AutoConfigureMockMvc
class PaymentControllerTest {

    private static final String BODY = "{\"amount\": 100, \"currency\": \"GHS\"}";

    @Autowired
    MockMvc mvc;

    @Autowired
    PaymentProcessor processor;

    private MvcResult send(String key, String body) throws Exception {
        var req = post("/process-payment").contentType(MediaType.APPLICATION_JSON).content(body);
        if (key != null) {
            req.header("Idempotency-Key", key);
        }
        return mvc.perform(req).andReturn();
    }

    @Test
    void firstRequestIsProcessed() throws Exception {
        String key = UUID.randomUUID().toString();
        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("X-Cache-Hit"))
                .andExpect(jsonPath("$.message").value("Charged 100 GHS"));
    }

    @Test
    void duplicateRequestIsReplayedWithoutReprocessing() throws Exception {
        String key = UUID.randomUUID().toString();
        MvcResult first = send(key, BODY);
        int chargesAfterFirst = processor.getChargeCount();

        long start = System.currentTimeMillis();
        MvcResult second = send(key, BODY);
        long took = System.currentTimeMillis() - start;

        assertThat(second.getResponse().getStatus()).isEqualTo(first.getResponse().getStatus());
        assertThat(second.getResponse().getContentAsString()).isEqualTo(first.getResponse().getContentAsString());
        assertThat(second.getResponse().getHeader("X-Cache-Hit")).isEqualTo("true");
        assertThat(processor.getChargeCount()).isEqualTo(chargesAfterFirst);
        assertThat(took).isLessThan(300);
    }

    @Test
    void reorderedJsonFieldsCountAsTheSameRequest() throws Exception {
        String key = UUID.randomUUID().toString();
        send(key, BODY);
        MvcResult second = send(key, "{\"currency\":\"GHS\",\"amount\":100}");
        assertThat(second.getResponse().getStatus()).isEqualTo(201);
        assertThat(second.getResponse().getHeader("X-Cache-Hit")).isEqualTo("true");
    }

    @Test
    void sameKeyDifferentBodyIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        send(key, BODY);

        mvc.perform(post("/process-payment")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 500, \"currency\": \"GHS\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Idempotency key already used for a different request body."));
    }

    @Test
    void concurrentRequestsWithSameKeyOnlyChargeOnce() throws Exception {
        String key = UUID.randomUUID().toString();
        int chargesBefore = processor.getChargeCount();

        ExecutorService pool = Executors.newFixedThreadPool(5);
        try {
            Callable<MvcResult> call = () -> send(key, BODY);
            var futures = new java.util.ArrayList<Future<MvcResult>>();
            for (int i = 0; i < 5; i++) {
                futures.add(pool.submit(call));
            }

            String expectedBody = null;
            int replayed = 0;
            for (Future<MvcResult> f : futures) {
                MvcResult r = f.get();
                assertThat(r.getResponse().getStatus()).isEqualTo(201);
                String body = r.getResponse().getContentAsString();
                if (expectedBody == null) {
                    expectedBody = body;
                }
                assertThat(body).isEqualTo(expectedBody);
                if ("true".equals(r.getResponse().getHeader("X-Cache-Hit"))) {
                    replayed++;
                }
            }
            assertThat(replayed).isEqualTo(4);
            assertThat(processor.getChargeCount()).isEqualTo(chargesBefore + 1);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void missingKeyIsRejected() throws Exception {
        assertThat(send(null, BODY).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void invalidAmountIsRejectedAndDoesNotBurnTheKey() throws Exception {
        String key = UUID.randomUUID().toString();
        assertThat(send(key, "{\"amount\": -5, \"currency\": \"GHS\"}").getResponse().getStatus()).isEqualTo(400);
        assertThat(send(key, BODY).getResponse().getStatus()).isEqualTo(201);
    }
}
