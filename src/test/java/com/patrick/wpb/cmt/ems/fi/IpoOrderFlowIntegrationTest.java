package com.patrick.wpb.cmt.ems.fi;

import static org.assertj.core.api.Assertions.assertThat;

import com.patrick.wpb.cmt.ems.fi.dto.ClientAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalPricedAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.dto.FinalRegionalAllocationRequest;
import com.patrick.wpb.cmt.ems.fi.dto.RegionalAllocationBreakdownRequest;
import com.patrick.wpb.cmt.ems.fi.entity.ClientAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalPricedAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.FinalRegionalAllocationEntity;
import com.patrick.wpb.cmt.ems.fi.entity.RegionalAllocationBreakdownEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import com.patrick.wpb.cmt.ems.fi.entity.TraderSubOrderEntity;
import com.patrick.wpb.cmt.ems.fi.enums.ClientAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderStatus;
import com.patrick.wpb.cmt.ems.fi.enums.IPOOrderSubStatus;
import com.patrick.wpb.cmt.ems.fi.enums.RegionalAllocationStatus;
import com.patrick.wpb.cmt.ems.fi.repo.ClientAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.RegionalAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalPricedAllocationBreakdownRepository;
import com.patrick.wpb.cmt.ems.fi.repo.FinalRegionalAllocationRepository;
import com.patrick.wpb.cmt.ems.fi.repo.TraderOrderRepository;
import com.patrick.wpb.cmt.ems.fi.service.ClientAllocationService;
import com.patrick.wpb.cmt.ems.fi.service.RegionalAllocationService;
import com.patrick.wpb.cmt.ems.fi.service.TraderOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class IpoOrderFlowIntegrationTest {

    @Autowired
    private TraderOrderRepository traderOrderRepository;

    @Autowired
    private ClientAllocationBreakdownRepository clientAllocationBreakdownRepository;

    @Autowired
    private RegionalAllocationBreakdownRepository regionalAllocationBreakdownRepository;

    @Autowired
    private FinalPricedAllocationBreakdownRepository finalPricedAllocationBreakdownRepository;

    @Autowired
    private FinalRegionalAllocationRepository finalRegionalAllocationRepository;

    @Autowired
    private TraderOrderService traderOrderService;

    @Autowired
    private RegionalAllocationService regionalAllocationService;

    @Autowired
    private ClientAllocationService clientAllocationService;

    @BeforeEach
    void setUp() {
        finalPricedAllocationBreakdownRepository.deleteAll();
        finalRegionalAllocationRepository.deleteAll();
        regionalAllocationBreakdownRepository.deleteAll();
        clientAllocationBreakdownRepository.deleteAll();
        traderOrderRepository.deleteAll();

        traderOrderRepository.save(buildOrder("ORDER-1", "HK"));
        traderOrderRepository.save(buildOrder("ORDER-2", "SG"));
    }

    @Test
    @Transactional
    void endToEndFlow_succeeds() {
        List<TraderOrderEntity> collectionOrders = traderOrderService.fetchOrderCollectionBlotter();
        assertThat(collectionOrders).extracting(TraderOrderEntity::getClientOrderId)
                .containsExactlyInAnyOrder("ORDER-1", "ORDER-2");

        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1", "ORDER-2"), "test-user");
        assertThat(groupedOrder.getOrderQuantity()).isEqualByComparingTo("200");
        assertThat(groupedOrder.getStatus()).isEqualTo(IPOOrderStatus.NEW);

        TraderOrderEntity proceededOrder = traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "test-user", "Proceed to regional");
        assertThat(proceededOrder.getStatus()).isEqualTo(IPOOrderStatus.REGIONAL_ALLOCATION);
        assertThat(proceededOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);

        regionalAllocationService.upsertAllocation(
                groupedOrder.getClientOrderId(),
                new BigDecimal("120"),
                new BigDecimal("80"),
                new BigDecimal("1.50"),
                "YIELD",
                new BigDecimal("200")
        );

        // Prepare breakdown data for submission
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "120", "120"),
                buildRegionalAllocationRequest("SG", "ACCOUNT-SG", "80", "80")
        );
        
        List<FinalPricedAllocationBreakdownRequest> pricedBreakdowns = List.of(
                buildFinalPricedAllocationRequest("HK", "YIELD", new BigDecimal("99.5")),
                buildFinalPricedAllocationRequest("SG", "YIELD", new BigDecimal("99.5"))
        );
        
        List<FinalRegionalAllocationRequest> regionalAllocations = List.of(
                buildFinalRegionalAllocationRequest("ASIA", new BigDecimal("200"))
        );

        TraderOrderEntity pendingApproval = regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                pricedBreakdowns,
                regionalAllocations,
                "approver",
                "Submit regional allocation");
        assertThat(pendingApproval.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION_APPROVAL);

        // Verify Regional Allocation Breakdowns are created with status=NEW
        List<RegionalAllocationBreakdownEntity> breakdownsAfterSubmit = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterSubmit).hasSize(2);
        assertThat(breakdownsAfterSubmit).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);

        // Verify Final Priced Allocation Breakdown is created
        List<FinalPricedAllocationBreakdownEntity> pricedBreakdownsResult = finalPricedAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(pricedBreakdownsResult).hasSize(2);

        // Verify Final Regional Allocation is created
        List<FinalRegionalAllocationEntity> finalRegionalList = finalRegionalAllocationRepository
                .findByClientOrderId(groupedOrder.getClientOrderId());
        assertThat(finalRegionalList).hasSize(1);
        assertThat(finalRegionalList.get(0).getAsiaAllocation()).isEqualByComparingTo("200");

        TraderOrderEntity clientAllocationPending = regionalAllocationService.approve(
                groupedOrder.getClientOrderId(), "approver", "Approve regional allocation");
        assertThat(clientAllocationPending.getStatus()).isEqualTo(IPOOrderStatus.CLIENT_ALLOCATION);
        assertThat(clientAllocationPending.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION);

        // Verify Regional Allocation Breakdowns are updated to ACCEPTED
        List<RegionalAllocationBreakdownEntity> breakdownsAfterApprove = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterApprove).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.ACCEPTED);

        List<ClientAllocationBreakdownRequest> draftBreakdowns = List.of(
                buildClientAllocationRequest("HK", "ACC-1", "120", "120"),
                buildClientAllocationRequest("SG", "ACC-2", "80", "80")
        );

        clientAllocationService.saveDraftAllocations(groupedOrder.getClientOrderId(), draftBreakdowns);
        assertThat(clientAllocationBreakdownRepository.findByOrderClientOrderId(groupedOrder.getClientOrderId()))
                .hasSize(2);

        TraderOrderEntity pendingClientApproval = clientAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(), draftBreakdowns, "allocator", "Submit client allocation");
        assertThat(pendingClientApproval.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_CLIENT_ALLOCATION_APPROVAL);

        TraderOrderEntity completedOrder = clientAllocationService.approve(
                groupedOrder.getClientOrderId(), "allocator", "Approve client allocation");
        assertThat(completedOrder.getStatus()).isEqualTo(IPOOrderStatus.CLIENT_ALLOCATION);
        assertThat(completedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.DONE);

        List<ClientAllocationBreakdownEntity> approvedBreakdowns =
                clientAllocationBreakdownRepository.findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(approvedBreakdowns)
                .hasSize(2)
                .extracting(ClientAllocationBreakdownEntity::getFinalAllocation)
                .containsExactlyInAnyOrder(new BigDecimal("120"), new BigDecimal("80"));
        assertThat(approvedBreakdowns).extracting(ClientAllocationBreakdownEntity::getClientAllocationStatus)
                .containsOnly(ClientAllocationStatus.ACCEPTED);
    }

    @Test
    @Transactional
    void rejectRegionalAllocation_revertsStatusAndBreakdowns() {
        TraderOrderEntity order1 = buildOrder("ORDER-1", "HK");
        traderOrderRepository.save(order1);

        TraderOrderEntity groupedOrder = traderOrderService.groupOrders(
                List.of("ORDER-1"), "test-user");

        traderOrderService.proceedToRegionalAllocation(
                groupedOrder.getClientOrderId(), "test-user", "Proceed to regional");

        regionalAllocationService.upsertAllocation(
                groupedOrder.getClientOrderId(),
                new BigDecimal("100"),
                BigDecimal.ZERO,
                new BigDecimal("1.50"),
                "YIELD",
                new BigDecimal("100")
        );

        // Prepare breakdown data for submission
        List<RegionalAllocationBreakdownRequest> regionalBreakdowns = List.of(
                buildRegionalAllocationRequest("HK", "ACCOUNT-HK", "100", "100")
        );
        
        List<FinalPricedAllocationBreakdownRequest> pricedBreakdowns = List.of(
                buildFinalPricedAllocationRequest("HK", "YIELD", new BigDecimal("99.5"))
        );
        
        List<FinalRegionalAllocationRequest> regionalAllocations = List.of(
                buildFinalRegionalAllocationRequest("ASIA", new BigDecimal("100"))
        );

        regionalAllocationService.submitForApproval(
                groupedOrder.getClientOrderId(),
                regionalBreakdowns,
                pricedBreakdowns,
                regionalAllocations,
                "approver",
                "Submit regional allocation");

        // Verify Regional Allocation Breakdowns have status NEW
        List<RegionalAllocationBreakdownEntity> breakdownsBeforeReject = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsBeforeReject).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);

        // Reject
        TraderOrderEntity rejectedOrder = regionalAllocationService.reject(
                groupedOrder.getClientOrderId(), "approver", "Reject regional allocation");
        assertThat(rejectedOrder.getStatus()).isEqualTo(IPOOrderStatus.REGIONAL_ALLOCATION);
        assertThat(rejectedOrder.getSubStatus()).isEqualTo(IPOOrderSubStatus.PENDING_REGIONAL_ALLOCATION);

        // Verify Regional Allocation Breakdowns still have status NEW (not changed on reject, but order status reverted)
        List<RegionalAllocationBreakdownEntity> breakdownsAfterReject = regionalAllocationBreakdownRepository
                .findByOrderClientOrderId(groupedOrder.getClientOrderId());
        assertThat(breakdownsAfterReject).extracting(RegionalAllocationBreakdownEntity::getRegionalAllocationStatus)
                .containsOnly(RegionalAllocationStatus.NEW);
    }

    private TraderOrderEntity buildOrder(String clientOrderId, String countryCode) {
        TraderOrderEntity order = TraderOrderEntity.builder()
                .clientOrderId(clientOrderId)
                .tradeDate(LocalDate.of(2024, 12, 1))
                .countryCode(countryCode)
                .status(IPOOrderStatus.NEW)
                .subStatus(IPOOrderSubStatus.NONE)
                .securityId("BOND-123")
                .orderQuantity(new BigDecimal("100"))
                .cleanPrice(new BigDecimal("99.5"))
                .build();

        TraderSubOrderEntity subOrder = TraderSubOrderEntity.builder()
                .countryCode(countryCode)
                .order(order)
                .accountId("ACCOUNT-" + countryCode)
                .issueIPOFlag(Boolean.TRUE)
                .build();

        order.getSubOrders().add(subOrder);
        return order;
    }

    private RegionalAllocationBreakdownRequest buildRegionalAllocationRequest(String countryCode,
                                                                              String accountNumber,
                                                                              String orderQuantity,
                                                                              String finalAllocation) {
        RegionalAllocationBreakdownRequest request = new RegionalAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setAccountNumber(accountNumber);
        request.setOrderQuantity(new BigDecimal(orderQuantity));
        request.setFinalAllocation(new BigDecimal(finalAllocation));
        request.setAllocationPercentage(new BigDecimal("0.50"));
        request.setEstimatedOrderSize(new BigDecimal(orderQuantity));
        request.setYieldLimit(new BigDecimal("1.50"));
        request.setSpreadLimit(new BigDecimal("0.25"));
        request.setSizeLimit(new BigDecimal(orderQuantity));
        return request;
    }

    private ClientAllocationBreakdownRequest buildClientAllocationRequest(String countryCode,
                                                                          String accountNumber,
                                                                          String orderQuantity,
                                                                          String finalAllocation) {
        ClientAllocationBreakdownRequest request = new ClientAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setAccountNumber(accountNumber);
        request.setOrderQuantity(new BigDecimal(orderQuantity));
        request.setFinalAllocation(new BigDecimal(finalAllocation));
        request.setAllocationPercentage(new BigDecimal("0.50"));
        request.setEstimatedOrderSize(new BigDecimal(orderQuantity));
        request.setYieldLimit(new BigDecimal("1.50"));
        request.setSpreadLimit(new BigDecimal("0.25"));
        request.setSizeLimit(new BigDecimal(orderQuantity));
        return request;
    }

    private FinalPricedAllocationBreakdownRequest buildFinalPricedAllocationRequest(String countryCode,
                                                                                    String limitType,
                                                                                    BigDecimal finalPrice) {
        FinalPricedAllocationBreakdownRequest request = new FinalPricedAllocationBreakdownRequest();
        request.setCountryCode(countryCode);
        request.setLimitType(limitType);
        request.setFinalPrice(finalPrice);
        return request;
    }

    private FinalRegionalAllocationRequest buildFinalRegionalAllocationRequest(String market,
                                                                               BigDecimal allocation) {
        FinalRegionalAllocationRequest request = new FinalRegionalAllocationRequest();
        request.setMarket(market);
        request.setAsiaAllocation(allocation);
        request.setAllocation(allocation);
        request.setEffectiveOrder(allocation);
        request.setProRata(new BigDecimal("1.0000"));
        request.setAllocationAmount(allocation);
        return request;
    }
}

