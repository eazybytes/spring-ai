package com.eazybytes.mcpserverstdio.tool;

import com.eazybytes.mcpserverstdio.entity.HelpDeskTicket;
import com.eazybytes.mcpserverstdio.model.TicketRequest;
import com.eazybytes.mcpserverstdio.service.HelpDeskTicketService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HelpDeskTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskTools.class);

    private final HelpDeskTicketService service;

    @Tool(name = "createTicket", description = "Create the Support Ticket")
    String createTicket(@ToolParam(description = "Details to create a Support ticket")
    TicketRequest ticketRequest) {
        LOGGER.info("Creating support ticket for user: {} with details: {}", ticketRequest);
        HelpDeskTicket savedTicket = service.createTicket(ticketRequest);
        LOGGER.info("Ticket created successfully. Ticket ID: {}, Username: {}", savedTicket.getId(), savedTicket.getUsername());
        return "Ticket #" + savedTicket.getId() + " created successfully for user " + savedTicket.getUsername();
    }

    @Tool(name="getTicketStatus", description = "Fetch the status of the tickets based on a given username")
    List<HelpDeskTicket> getTicketStatus(@ToolParam(description =
            "Username to fetch the status of the help desk tickets") String username) {
        LOGGER.info("Fetching tickets for user: {}", username);
        List<HelpDeskTicket> tickets =  service.getTicketsByUsername(username);
        LOGGER.info("Found {} tickets for user: {}", tickets.size(), username);
        return tickets;
    }

}
