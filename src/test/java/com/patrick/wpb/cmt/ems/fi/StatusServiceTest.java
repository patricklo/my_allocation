package com.patrick.wpb.cmt.ems.fi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.service.StatusService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class StatusServiceTest {

    @Autowired
    private StatusService statusService;

    @Autowired
    private TraderOrderRepository traderOrderRepository;

    @BeforeEach
    void setUp() {
        traderOrderRepository.deleteAll();
    }

    @Test
    @Transactional
    void exemptStatus_cancelled_allowsTransitionFromAnyStatus() {
        // Test cancellation from NEW status
        TraderOrderEntity newOrder = createTestOrder("ORDER-1", IPOOrderStatus.NEW, IPOOrderSubStatus.NONE);
        
        TraderOrderEntity cancelledFromNew = statusService.cancelOrder("ORDER-1", "admin", "Cancelled by admin");
        assertThat(cancelledFromNew.getStatus()).isEqualTo(IPOOrderStatus.CANCELLED);
        assertThat(cancelledFromNew.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);

        // Test cancellation from REGIONAL_ALLOCATION status
        TraderOrderEntity regionalOrder = createTestOrder("ORDER-2", IPOOrderStatus.REGIONAL_ALLOCATION, IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);
        
        TraderOrderEntity cancelledFromRegional = statusService.cancelOrder("ORDER-2", "admin", "Cancelled by admin");
        assertThat(cancelledFromRegional.getStatus()).isEqualTo(IPOOrderStatus.CANCELLED);
        assertThat(cancelledFromRegional.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);

        // Test cancellation from CLIENT_ALLOCATION status
        TraderOrderEntity clientOrder = createTestOrder("ORDER-3", IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL);
        
        TraderOrderEntity cancelledFromClient = statusService.cancelOrder("ORDER-3", "admin", "Cancelled by admin");
        assertThat(cancelledFromClient.getStatus()).isEqualTo(IPOOrderStatus.CANCELLED);
        assertThat(cancelledFromClient.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);
    }

    @Test
    @Transactional
    void exemptStatus_cancelled_usingUpdateStatusDirectly() {
        // Test that CANCELLED can be reached directly via updateStatus from any status
        TraderOrderEntity order = createTestOrder("ORDER-1", IPOOrderStatus.CLIENT_ALLOCATION, IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL);
        
        // This should succeed even though there's no explicit transition rule
        TraderOrderEntity cancelled = statusService.updateStatus(
                "ORDER-1", 
                IPOOrderStatus.CANCELLED, 
                IPOOrderSubStatus.NONE, 
                "admin", 
                "Emergency cancellation"
        );
        
        assertThat(cancelled.getStatus()).isEqualTo(IPOOrderStatus.CANCELLED);
        assertThat(cancelled.getSubStatus()).isEqualTo(IPOOrderSubStatus.NONE);
    }

    @Test
    @Transactional
    void normalStatusTransition_invalidTransition_throwsException() {
        // Test that normal status transitions still require validation
        TraderOrderEntity order = createTestOrder("ORDER-1", IPOOrderStatus.NEW, IPOOrderSubStatus.NONE);
        
        // This should fail because there's no direct transition from NEW to CLIENT_ALLOCATION
        assertThatThrownBy(() -> statusService.updateStatus(
                "ORDER-1", 
                IPOOrderStatus.CLIENT_ALLOCATION, 
                IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION, 
                "user", 
                "Invalid transition"
        ))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Transition from NEW/NONE to CLIENT_ALLOCATION/PENDING_CLIENT_ALLOCATION is not allowed");
    }

    @Test
    @Transactional
    void normalStatusTransition_validTransition_succeeds() {
        // Test that valid transitions still work
        TraderOrderEntity order = createTestOrder("ORDER-1", IPOOrderStatus.NEW, IPOOrderSubStatus.NONE);
        
        TraderOrderEntity updated = statusService.updateStatus(
                "ORDER-1", 
                IPOOrderStatus.REGIONAL_ALLOCATION, 
                IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION, 
                "user", 
                "Valid transition"
        );
        
        assertThat(updated.getStatus()).isEqualTo(IPOOrderStatus.REGIONAL_ALLOCATION);
        assertThat(updated.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);
    }

    private TraderOrderEntity createTestOrder(String clientOrderId, IPOOrderStatus status, IPOOrderSubStatus subStatus) {
        TraderOrderEntity order = TraderOrderEntity.builder()
                .clientOrderId(clientOrderId)
                .tradeDate(LocalDate.now())
                .countryCode("HK")
                .status(status)
                .subStatus(subStatus)
                .securityId("SEC-123")
                .orderQuantity(BigDecimal.valueOf(1000))
                .cleanPrice(BigDecimal.valueOf(100))
                .build();
        return traderOrderRepository.save(order);
    }
}
