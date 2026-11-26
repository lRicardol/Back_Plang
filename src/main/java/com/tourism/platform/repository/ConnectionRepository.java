package com.tourism.platform.repository;

import com.tourism.platform.model.Connection;
import com.tourism.platform.model.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findByIdAndRequesterId(Long connectionId, Long requesterId);
    Optional<Connection> findByIdAndRecipientId(Long connectionId, Long recipientId);
    List<Connection> findByRequesterId(Long requesterId);
    List<Connection> findByRecipientId(Long recipientId);
    void deleteByIdAndRequesterId(Long connectionId, Long requesterId);
    void deleteByIdAndRecipientId(Long connectionId, Long recipientId);
    
    // Methods for connection requests
    Optional<Connection> findByRequesterIdAndRecipientId(Long requesterId, Long recipientId);
    List<Connection> findByRecipientIdAndStatus(Long recipientId, ConnectionStatus status);
    List<Connection> findByRequesterIdAndStatus(Long requesterId, ConnectionStatus status);

    @Query("""
            SELECT COUNT(c) > 0
            FROM Connection c
            WHERE c.status = :status
              AND ((c.requesterId = :firstUserId AND c.recipientId = :secondUserId)
                OR (c.requesterId = :secondUserId AND c.recipientId = :firstUserId))
            """)
    boolean existsConnectionBetweenUsersWithStatus(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId,
            @Param("status") ConnectionStatus status);
}