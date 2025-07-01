package com.prasanth.training.ems.bmsfrontend.service;


import com.prasanth.training.ems.bmsfrontend.models.BusInventory;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class BusSearchService {
    private static final Logger logger = LoggerFactory.getLogger(BusSearchService.class);

    private static final String searchUrl = "http://localhost:9091/api/v1/compositor/searchBus?from=%s&to=%s";

    @Autowired
    WebClient.Builder webClientBuilder;

    public List<BusInventory> searchbus(String from, String to, HttpSession session) {

        String searchEndpoint = searchUrl.formatted(from,to);
        logger.info("Search bus url: {}",searchEndpoint);
        WebClient searchClient = webClientBuilder.build();
        String authToken = (String) session.getAttribute("Authorization-token");
        ResponseEntity<List<BusInventory>> searchResultMono= searchClient.get()
                .uri(searchEndpoint)
                .header("Authorization", authToken)
                .retrieve()
                .toEntityList(BusInventory.class)
                .block();

        logger.info("Search bus found: "+searchResultMono);

        if(!searchResultMono.getStatusCode().is2xxSuccessful()) {
            logger.error("Search bus not found, return code {}",searchResultMono.getStatusCode());
        }
        return searchResultMono.getBody();
    }

}
