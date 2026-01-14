// ============================================================================
// FINAL FIX - Replace your validateSplitAddress method with this
// ============================================================================

public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("=== Starting validateSplitAddress ===");
    logger.info("Request received with " + (fiveGCoverageCheckRequest.getCustomerAddress() != null ? fiveGCoverageCheckRequest.getCustomerAddress().size() : 0) + " customer addresses");
    
    if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
        logger.info("Customer addresses are VALID. Proceeding with split address service calls.");
        
        List<Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress>> addressProcessingTasks = new ArrayList<>();
        
        for (onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress customerAddress : fiveGCoverageCheckRequest.getCustomerAddress()) {
            logger.info("Processing address: " + customerAddress.getAddressLine1() + ", " + customerAddress.getCity() + ", " + customerAddress.getZipcode());
            
            SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);
            logger.info("Built split address request for: " + customerAddress.getAddressLine1());
            
            Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> task = spectrumAdapterService.splitAddress(splitAddressRequest)
                    .doOnSubscribe(subscription -> 
                            logger.info(">>> Calling split address service for: " + customerAddress.getAddressLine1()))
                    .flatMap(addressResponse -> {
                        logger.info("<<< Received response from split address service for: " + customerAddress.getAddressLine1());
                        
                        if (isValidSplitAddressResponse(addressResponse)) {
                            logger.info("Split address response is VALID");
                            onevz.soe.smbenrollment.responses.spectrumadapter.Address address = extractSplitAddress(addressResponse);
                            
                            if (address != null) {
                                logger.info("BEFORE updateCustomerAddress - extracted address: streetNum=[" + address.getStreetNum() + "], streetName=[" + address.getStreetName() + "]");
                                
                                updateCustomerAddress(customerAddress, address);
                                
                                logger.info("AFTER updateCustomerAddress - customerAddress: streetNum=[" + customerAddress.getStreetNum() + "], streetName=[" + customerAddress.getStreetName() + "]");
                                
                                if (customerAddress.getStreetNum() != null && customerAddress.getStreetName() != null) {
                                    logger.info("✓ UPDATE SUCCESSFUL - streetNum=[" + customerAddress.getStreetNum() + "], streetName=[" + customerAddress.getStreetName() + "]");
                                } else {
                                    logger.error("!!!!! UPDATE FAILED - streetNum or streetName is still NULL after updateCustomerAddress !!!!!");
                                }
                            } else {
                                logger.warn("Extracted address is NULL");
                            }
                        } else {
                            logger.warn("Split address response is INVALID or incomplete");
                        }
                        
                        return Mono.just(customerAddress);
                    })
                    .doOnError(e -> logger.error("!!! Error occurred from cxp spectrum service for address: " + customerAddress.getAddressLine1() + ". Error: " + e.getMessage(), e))
                    .onErrorResume(error -> {
                        logger.error("Handling error gracefully for address: " + customerAddress.getAddressLine1());
                        return Mono.just(customerAddress);
                    });
            
            addressProcessingTasks.add(task);
        }
        
        logger.info("Total split address tasks created: " + addressProcessingTasks.size());
        
        // CRITICAL FIX: Use flatMap instead of doOnSuccess to ensure the update happens in the chain
        return Flux.concat(addressProcessingTasks)
                .collectList()
                .flatMap(updatedAddresses -> {
                    logger.info("====== ALL TASKS COMPLETED ======");
                    logger.info("Collected " + updatedAddresses.size() + " updated addresses");
                    
                    // Log each collected address
                    for (int i = 0; i < updatedAddresses.size(); i++) {
                        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = updatedAddresses.get(i);
                        logger.info("Collected Address[" + i + "] hashCode: " + System.identityHashCode(addr) + 
                                ", streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], addressLine1=[" + addr.getAddressLine1() + "]");
                    }
                    
                    // Replace the original addresses with updated ones
                    fiveGCoverageCheckRequest.setCustomerAddress(updatedAddresses);
                    
                    // Verify the replacement worked
                    List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> afterSetList = fiveGCoverageCheckRequest.getCustomerAddress();
                    logger.info("After setCustomerAddress, list hashCode: " + System.identityHashCode(afterSetList));
                    
                    if (afterSetList != null) {
                        for (int i = 0; i < afterSetList.size(); i++) {
                            onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = afterSetList.get(i);
                            logger.info("FINAL Address[" + i + "] in request: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], addressLine1=[" + addr.getAddressLine1() + "]");
                        }
                    }
                    
                    // CRITICAL: Now call the next method INSIDE flatMap to ensure it happens AFTER setCustomerAddress
                    return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest);
                })
                .map(this::buildSuccessResponse)
                .onErrorResume(this::buildErrorResponse);
    }
    
    logger.warn("Customer addresses are INVALID. Skipping split address service calls.");
    return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest)
            .map(this::buildSuccessResponse)
            .onErrorResume(this::buildErrorResponse);
}

// ============================================================================
// KEY CHANGE EXPLANATION:
// ============================================================================
//
// OLD CODE:
// .collectList()
// .doOnSuccess(updatedAddresses -> {
//     fiveGCoverageCheckRequest.setCustomerAddress(updatedAddresses);
// })
// .then(processValidateCustomerAccountForFiveGCoverage(...))
//
// PROBLEM: .then() discards the result and proceeds immediately
// The doOnSuccess might not have executed yet when processValidateCustomer... runs
//
// NEW CODE:
// .collectList()
// .flatMap(updatedAddresses -> {
//     fiveGCoverageCheckRequest.setCustomerAddress(updatedAddresses);
//     return processValidateCustomerAccountForFiveGCoverage(...);
// })
//
// SOLUTION: flatMap ensures processValidateCustomer... is called AFTER
// setCustomerAddress() completes, and the addresses are guaranteed to be updated
//
// ============================================================================
