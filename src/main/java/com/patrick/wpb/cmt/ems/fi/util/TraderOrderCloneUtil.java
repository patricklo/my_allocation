package com.patrick.wpb.cmt.ems.fi.util;

import com.patrick.wpb.cmt.ems.fi.entity.TraderOrderEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanUtils;

/**
 * Utility class for cloning TraderOrderEntity objects using BeanUtils.copyProperties.
 * Provides methods to create copies of trader orders with customizable field overrides.
 */
@UtilityClass
public class TraderOrderCloneUtil {

    /**
     * Creates a shallow clone of the source TraderOrderEntity.
     * The cloned entity will have:
     * - A new clientOrderId (generated UUID)
     * - originalClientOrderId set to null
     * - All other fields copied from source using BeanUtils.copyProperties
     * - Relationships (subOrders, regionalAllocation, clientAllocations) excluded
     * - Audit fields (createdAt, updatedAt) will be set by JPA auditing
     * 
     * @param source The source TraderOrderEntity to clone
     * @return A new TraderOrderEntity with copied fields
     */
    public static TraderOrderEntity clone(TraderOrderEntity source) {
        return clone(source, null, null, null);
    }

    /**
     * Creates a clone of the source TraderOrderEntity with optional field overrides.
     * Uses BeanUtils.copyProperties to copy all matching properties from source to target.
     * 
     * @param source The source TraderOrderEntity to clone
     * @param newClientOrderId Optional new client order ID. If null, a UUID will be generated
     * @param newCountryCode Optional new country code. If null, source country code will be used
     * @param newOrderQuantity Optional new order quantity. If null, source quantity will be used
     * @return A new TraderOrderEntity with copied fields and optional overrides
     */
    public static TraderOrderEntity clone(TraderOrderEntity source,
                                          String newClientOrderId,
                                          String newCountryCode,
                                          BigDecimal newOrderQuantity) {
        if (source == null) {
            throw new IllegalArgumentException("Source TraderOrderEntity cannot be null");
        }

        // Create new instance
        TraderOrderEntity cloned = new TraderOrderEntity();
        
        // Copy all properties from source to cloned using BeanUtils
        BeanUtils.copyProperties(source, cloned);
        
        // Override specific fields
        String clientOrderId = newClientOrderId != null ? newClientOrderId : UUID.randomUUID().toString();
        cloned.setClientOrderId(clientOrderId);
        cloned.setOriginalClientOrderId(null); // New group order, no original reference
        
        if (newCountryCode != null) {
            cloned.setCountryCode(newCountryCode);
        }
        
        if (newOrderQuantity != null) {
            cloned.setOrderQuantity(newOrderQuantity);
        }
        
        // Exclude relationships - they should be managed separately
        cloned.setSubOrders(new ArrayList<>());
        cloned.setRegionalAllocation(null);
        cloned.setClientAllocations(new ArrayList<>());
        
        return cloned;
    }

    /**
     * Creates a clone with a new client order ID and country code.
     * Useful for creating regional group orders.
     * 
     * @param source The source TraderOrderEntity to clone
     * @param newCountryCode The new country code for the cloned order
     * @return A new TraderOrderEntity with new country code
     */
    public static TraderOrderEntity cloneWithNewCountryCode(TraderOrderEntity source, String newCountryCode) {
        return clone(source, null, newCountryCode, null);
    }

    /**
     * Creates a clone with a new client order ID and order quantity.
     * Useful for adjusting quantities when splitting orders.
     * 
     * @param source The source TraderOrderEntity to clone
     * @param newOrderQuantity The new order quantity for the cloned order
     * @return A new TraderOrderEntity with new order quantity
     */
    public static TraderOrderEntity cloneWithNewQuantity(TraderOrderEntity source, BigDecimal newOrderQuantity) {
        return clone(source, null, null, newOrderQuantity);
    }

    /**
     * Creates a clone with both new country code and order quantity.
     * 
     * @param source The source TraderOrderEntity to clone
     * @param newCountryCode The new country code for the cloned order
     * @param newOrderQuantity The new order quantity for the cloned order
     * @return A new TraderOrderEntity with new country code and quantity
     */
    public static TraderOrderEntity cloneWithNewCountryAndQuantity(TraderOrderEntity source,
                                                                    String newCountryCode,
                                                                    BigDecimal newOrderQuantity) {
        return clone(source, null, newCountryCode, newOrderQuantity);
    }
}

