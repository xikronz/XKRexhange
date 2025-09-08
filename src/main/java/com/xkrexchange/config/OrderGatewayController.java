package com.xkrexchange.gateway;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.xkrexchange.common.dto.OrderRequestDto;
import com.xkrexchange.common.dto.OrderResponseDto;

/**
 * REST Controller for Order Gateway Service
 * This is the main entry point for all order submissions from clients
 * 
 * Responsibilities:
 * 1. Receive order requests from frontend
 * 2. Validate request format and basic parameters
 * 3. Delegate to OrderGatewayService for processing
 * 4. Return appropriate responses to client
 */
@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin(origins = "*") // TODO: Configure proper CORS in production
public class OrderGatewayController {
    
    // TODO: Inject OrderGatewayService dependency
    // private final OrderGatewayService orderGatewayService;
    
    /**
     * Submit a new order (any type: MARKET, LIMIT, STOP, STOP_LIMIT)
     * 
     * @param orderRequest - Contains all order details from frontend
     * @param authToken - JWT token from Authorization header
     * @return OrderResponseDto with order ID or error details
     */
    @PostMapping("/submit")
    public ResponseEntity<OrderResponseDto> submitOrder(
            @RequestBody OrderRequestDto orderRequest,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement order submission logic
        // 1. Extract and validate JWT token
        // 2. Call orderGatewayService.processOrder()
        // 3. Return appropriate response
        
        return ResponseEntity.ok(new OrderResponseDto("TODO: Implement"));
    }
    
    /**
     * Cancel an existing order
     * 
     * @param orderId - ID of order to cancel
     * @param authToken - JWT token for authorization
     * @return Success/failure response
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement order cancellation logic
        // 1. Validate user owns this order
        // 2. Check if order is still cancellable
        // 3. Send cancellation to matching engine
        // 4. Release reserved funds
        
        return ResponseEntity.ok("TODO: Implement cancellation");
    }
    
    /**
     * Get order status and details
     * 
     * @param orderId - ID of order to query
     * @param authToken - JWT token for authorization
     * @return Order details and current status
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderStatus(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement order status retrieval
        // 1. Validate user owns this order or has admin privileges
        // 2. Query order details from database
        // 3. Return current status and execution details
        
        return ResponseEntity.ok(new OrderResponseDto("TODO: Implement"));
    }
} 