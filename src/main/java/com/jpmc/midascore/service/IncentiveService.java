package com.jpmc.midascore.service;

import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class IncentiveService {

    private static final Logger logger = LoggerFactory.getLogger(IncentiveService.class);
    private static final String INCENTIVE_API_URL = "http://localhost:8080/incentive";

    private final RestTemplate restTemplate;

    public IncentiveService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches incentive amount for a given transaction from the Incentive API.
     * 
     * @param transaction The transaction to get incentive for
     * @return The incentive amount (>= 0)
     * @throws RuntimeException if API call fails
     */
    public float getIncentive(Transaction transaction) {
        try {
            logger.info("Fetching incentive for transaction: {}", transaction);
            Incentive incentive = restTemplate.postForObject(
                INCENTIVE_API_URL, 
                transaction, 
                Incentive.class
            );
            
            if (incentive == null) {
                logger.warn("Incentive API returned null response, defaulting to 0");
                return 0.0f;
            }
            
            float incentiveAmount = incentive.getAmount();
            logger.info("Received incentive amount: {}", incentiveAmount);
            return incentiveAmount;
            
        } catch (Exception e) {
            logger.error("Failed to fetch incentive from API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch incentive from Incentive API", e);
        }
    }
}
