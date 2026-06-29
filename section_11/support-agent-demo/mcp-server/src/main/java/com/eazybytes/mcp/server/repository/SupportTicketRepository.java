package com.eazybytes.mcp.server.repository;

import com.eazybytes.mcp.server.domain.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String email);
}
