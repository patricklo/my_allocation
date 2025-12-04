package com.patrick.wpb.cmt.ems.fi.repo;

import com.patrick.wpb.cmt.ems.fi.entity.TsoxClientOrderMappingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TsoxClientOrderMappingRepository extends JpaRepository<TsoxClientOrderMappingEntity, String> {

    /**
     * Finds the latest active record for the given client order ID.
     * Returns the record with the most recent update_at timestamp where active_flag = 'Y'.
     * 
     * @param clientOrderId Client order ID to search for
     * @return Optional containing the latest active TsoxClientOrderMappingEntity, or empty if not found
     */
    @Query(value = """
        SELECT t1.*
        FROM tsox_client_order_mapping t1
        WHERE t1.client_order_id = :clientOrderId
          AND t1.active_flag = 'Y'
          AND t1.update_at = (
              SELECT MAX(update_at)
              FROM tsox_client_order_mapping
              WHERE client_order_id = :clientOrderId
                AND active_flag = 'Y'
          )
        ORDER BY t1.update_at DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<TsoxClientOrderMappingEntity> findLatestActiveRecordsByClientOrderId(
            @Param("clientOrderId") String clientOrderId);

    /**
     * Updates active_flag to 'N' for records matching the given tsox_client_order_id
     * or previous_tsox_client_order_id.
     * 
     * @param tsoxClientOrderId The tsox_client_order_id to match
     * @param previousTsoxClientOrderId The previous_tsox_client_order_id to match
     * @return Number of records updated
     */
    @Modifying
    @Transactional
    @Query(value = """
        UPDATE tsox_client_order_mapping
        SET active_flag = 'N',
            update_at = CURRENT_TIMESTAMP
        WHERE (tsox_client_order_id = :tsoxClientOrderId
           OR previous_tsox_client_order_id = :previousTsoxClientOrderId)
          AND active_flag = 'Y'
        """, nativeQuery = true)
    int deactivateByTsoxClientOrderIdOrPreviousTsoxClientOrderId(
            @Param("tsoxClientOrderId") String tsoxClientOrderId,
            @Param("previousTsoxClientOrderId") String previousTsoxClientOrderId);
}

