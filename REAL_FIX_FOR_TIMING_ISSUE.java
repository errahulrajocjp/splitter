// ============================================================================
// THE REAL FIX - Replace your validateSplitAddress method with this
// ============================================================================

public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("=== Starting validateSplitAddress ===");
    logger.info("Request received with {} customer addresses", 
            fiveGCoverageCheckRequest.getCustomerAddress() != null ? 
                    fiveGCoverageCheckRequest.getCustomerAddress().size() : 0);
    
    // Fixed: Inverted the condition to process VALID addresses through split address service
    if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
        logger.info("Customer addresses are VALID. Proceeding with split address service calls.");
        
        // CRITICAL FIX: Instead of List<Mono<Void>>, we need List<Mono<CustomerAddress>>
        // This ensures the updated addresses are returned and can be collected
        List<Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress>> addressProcessingTasks = new ArrayList<>();
        
        for (onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress customerAddress : fiveGCoverageCheckRequest.getCustomerAddress()) {
            logger.info("Processing address: {}, {}, {}", 
                    customerAddress.getAddressLine1(), 
                    customerAddress.getCity(), 
                    customerAddress.getZipcode());
            
            SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);
            logger.info("Built split address request for: {}", customerAddress.getAddressLine1());
            
            // CRITICAL FIX: Return the updated CustomerAddress, not Void
            Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> task = spectrumAdapterService.splitAddress(splitAddressRequest)
                    .doOnSubscribe(subscription -> 
                            logger.info(">>> Calling split address service for: {}", customerAddress.getAddressLine1()))
                    .flatMap(addressResponse -> {
                        logger.info("<<< Received response from split address service for: {}", customerAddress.getAddressLine1());
                        logger.debug("Split address response: {}", addressResponse);
                        
                        if (isValidSplitAddressResponse(addressResponse)) {
                            logger.info("Split address response is VALID");
                            onevz.soe.smbenrollment.responses.spectrumadapter.Address address = extractSplitAddress(addressResponse);
                            if (address != null) {
                                logger.info("Extracted address: streetNum={}, streetName={}, city={}, state={}, zip={}", 
                                        address.getStreetNum(), address.getStreetName(), 
                                        address.getCity(), address.getState(), address.getZipcode());
                                
                                // Update the customer address
                                updateCustomerAddress(customerAddress, address);
                                
                                logger.info("Customer address updated successfully from split address service");
                                logger.info("AFTER UPDATE - CustomerAddress: streetNum={}, streetName={}, addressLine1={}", 
                                        customerAddress.getStreetNum(), customerAddress.getStreetName(), customerAddress.getAddressLine1());
                            } else {
                                logger.warn("Extracted address is NULL");
                            }
                        } else {
                            logger.warn("Split address response is INVALID or incomplete");
                        }
                        
                        // CRITICAL: Return the updated customerAddress
                        return Mono.just(customerAddress);
                    })
                    .doOnError(e -> logger.error("!!! Error occurred from cxp spectrum service for address: {}. Error: {}", 
                            customerAddress.getAddressLine1(), e.getMessage(), e))
                    .onErrorResume(error -> {
                        logger.error("Handling error gracefully for address: {}", customerAddress.getAddressLine1());
                        // Still return the original customerAddress even on error
                        return Mono.just(customerAddress);
                    });
            
            addressProcessingTasks.add(task);
        }
        
        logger.info("Total split address tasks created: {}", addressProcessingTasks.size());
        
        // CRITICAL FIX: Use Flux.concat or Mono.zip to collect all updated addresses
        // This ensures all addresses are updated BEFORE proceeding
        return Flux.concat(addressProcessingTasks)
                .collectList()
                .doOnSuccess(updatedAddresses -> {
                    logger.info("All split address service calls completed successfully");
                    logger.info("Updated {} addresses", updatedAddresses.size());
                    
                    // Log each updated address
                    for (int i = 0; i < updatedAddresses.size(); i++) {
                        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = updatedAddresses.get(i);
                        logger.info("Updated Address[{}]: streetNum={}, streetName={}, addressLine1={}", 
                                i, addr.getStreetNum(), addr.getStreetName(), addr.getAddressLine1());
                    }
                    
                    // Replace the original addresses with updated ones
                    fiveGCoverageCheckRequest.setCustomerAddress(updatedAddresses);
                })
                .doOnError(e -> logger.error("Error in split address processing: {}", e.getMessage(), e))
                .then(processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest))
                .map(this::buildSuccessResponse)
                .onErrorResume(this::buildErrorResponse);
    }
    
    logger.warn("Customer addresses are INVALID. Skipping split address service calls.");
    return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest)
            .map(this::buildSuccessResponse)
            .onErrorResume(this::buildErrorResponse);
}


// ============================================================================
// EXPLANATION OF THE FIX:
// ============================================================================
// 
// OLD CODE PROBLEM:
// - Mono<Void> tasks with doOnNext() side effects
// - updateCustomerAddress() was called in doOnNext() (side effect)
// - Mono.when() waited for completion, but side effects might not have executed
// - By the time processValidateCustomerAccountForFiveGCoverage() ran, 
//   the CustomerAddress objects were still not updated
//
// NEW CODE SOLUTION:
// - Mono<CustomerAddress> tasks that RETURN the updated address
// - updateCustomerAddress() is called in flatMap() which is part of the chain
// - Flux.concat() collects all Monos and ensures they all complete
// - collectList() gathers all updated CustomerAddress objects
// - doOnSuccess() replaces the original list with updated addresses
// - NOW when processValidateCustomerAccountForFiveGCoverage() runs,
//   all addresses are guaranteed to be updated!
//
// ============================================================================

// Also add this import if not already present:
import reactor.core.publisher.Flux;
