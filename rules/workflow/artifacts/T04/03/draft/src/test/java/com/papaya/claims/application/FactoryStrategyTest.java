package com.papaya.claims.application;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.papaya.claims.contract.Model.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class FactoryStrategyTest {
    @Test void rejectionAndInformationStrategiesCreateDifferentRecipientPayloads() throws Exception {
        var factory = new MockEffectFactory(JsonMapper.builder().findAndAddModules().build());
        ClaimSnapshot rejected = new ClaimSnapshot(UUID.randomUUID(), "member-001", "UNDER_ASSESSMENT", BigDecimal.TEN, "USD",
                Map.of("rejectionReason", "duplicate", "missingInfoDescription", "upload invoice"), 0, 1, null, null);
        String rejection = factory.create("notify_rejection", rejected, new Actor("assessor", "assessor"), "").payload();
        String info = factory.create("request_information", rejected, new Actor("assessor", "assessor"), "").payload();
        assertThat(rejection).contains("duplicate", "appealInstructions", "recipientRole");
        assertThat(info).contains("upload invoice", "information_request");
        assertThat(factory.supports("create_payment_request")).isTrue();
        assertThatThrownBy(() -> factory.strategy("unknown")).hasMessageContaining("No asynchronous strategy");
    }
}
