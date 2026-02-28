package com.portal.universe.paymentservice.intent.service;

import com.portal.universe.paymentservice.intent.dto.CreateIntentRequest;
import com.portal.universe.paymentservice.intent.dto.IntentResponse;

public interface IntentService {

    IntentResponse createIntent(CreateIntentRequest request);

    IntentResponse getIntent(String intentId);

    void cancelIntent(String intentId, String userId);
}
