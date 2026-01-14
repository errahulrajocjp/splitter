// ============================================================================
// DIAGNOSTIC VERSION - Use this to find exactly where the problem is
// ============================================================================

public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("=== Starting validateSplitAddress ===");
    logger.info("Request received with " + (fiveGCoverageCheckRequest.getCustomerAddress() != null ? fiveGCoverageCheckRequest.getCustomerAddress().size() : 0) + " customer addresses");
    
    // LOG ORIGINAL ADDRESSES
    if (fiveGCoverageCheckRequest.getCustomerAddress() != null) {
        for (int i = 0; i < fiveGCoverageCheckRequest.getCustomerAddress().size(); i++) {
            onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = fiveGCoverageCheckRequest.getCustomerAddress().get(i);
            logger.info("ORIGINAL Address[" + i + "]: streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], addressLine1=[" + addr.getAddressLine1() + "]");
        }
    }
    
    if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
        logger.info("Customer addresses are VALID. Proceeding with split address service calls.");
        
        List<Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress>> addressProcessingTasks = new ArrayList<>();
        
        for (onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress customerAddress : fiveGCoverageCheckRequest.getCustomerAddress()) {
            logger.info("Processing address: " + customerAddress.getAddressLine1() + ", " + customerAddress.getCity() + ", " + customerAddress.getZipcode());
            
            // Store reference to track if it's the same object
            final onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress originalAddressRef = customerAddress;
            logger.info("Original address object hashCode: " + System.identityHashCode(originalAddressRef));
            
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
                                logger.info("BEFORE updateCustomerAddress - customerAddress: streetNum=[" + customerAddress.getStreetNum() + "], streetName=[" + customerAddress.getStreetName() + "]");
                                logger.info("BEFORE updateCustomerAddress - extracted address: streetNum=[" + address.getStreetNum() + "], streetName=[" + address.getStreetName() + "]");
                                logger.info("CustomerAddress object hashCode before update: " + System.identityHashCode(customerAddress));
                                
                                // Update the customer address
                                updateCustomerAddress(customerAddress, address);
                                
                                logger.info("AFTER updateCustomerAddress - customerAddress: streetNum=[" + customerAddress.getStreetNum() + "], streetName=[" + customerAddress.getStreetName() + "]");
                                logger.info("CustomerAddress object hashCode after update: " + System.identityHashCode(customerAddress));
                                logger.info("Are they same object? " + (originalAddressRef == customerAddress));
                                
                                // Verify the update actually worked
                                if (customerAddress.getStreetNum() == null || customerAddress.getStreetName() == null) {
                                    logger.error("!!!!! UPDATE FAILED - streetNum or streetName is still NULL after updateCustomerAddress !!!!!");
                                    logger.error("This means updateCustomerAddress() is NOT working correctly!");
                                } else {
                                    logger.info("✓ UPDATE SUCCESSFUL - streetNum=[" + customerAddress.getStreetNum() + "], streetName=[" + customerAddress.getStreetName() + "]");
                                }
                            } else {
                                logger.warn("Extracted address is NULL");
                            }
                        } else {
                            logger.warn("Split address response is INVALID or incomplete");
                        }
                        
                        // Return the updated customerAddress
                        logger.info("Returning customerAddress with hashCode: " + System.identityHashCode(customerAddress));
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
        
        return Flux.concat(addressProcessingTasks)
                .collectList()
                .doOnSuccess(updatedAddresses -> {
                    logger.info("====== ALL TASKS COMPLETED ======");
                    logger.info("Collected " + updatedAddresses.size() + " updated addresses");
                    
                    // Log each collected address
                    for (int i = 0; i < updatedAddresses.size(); i++) {
                        onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress addr = updatedAddresses.get(i);
                        logger.info("Collected Address[" + i + "] hashCode: " + System.identityHashCode(addr) + 
                                ", streetNum=[" + addr.getStreetNum() + "], streetName=[" + addr.getStreetName() + "], addressLine1=[" + addr.getAddressLine1() + "]");
                    }
                    
                    // CRITICAL: Check if we need to replace or if they're already updated
                    logger.info("About to call setCustomerAddress on the request object");
                    
                    // Get the original list for comparison
                    List<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> originalList = fiveGCoverageCheckRequest.getCustomerAddress();
                    logger.info("Original list hashCode: " + System.identityHashCode(originalList));
                    logger.info("Updated list hashCode: " + System.identityHashCode(updatedAddresses));
                    
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
                })
                .doOnError(e -> logger.error("Error in split address processing: " + e.getMessage(), e))
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
// WHAT TO LOOK FOR IN THE LOGS:
// ============================================================================
//
// 1. Check "BEFORE updateCustomerAddress - extracted address: streetNum=[X]"
//    - If this is NULL, the split address service is not returning the data
//
// 2. Check "AFTER updateCustomerAddress - customerAddress: streetNum=[X]"
//    - If this is NULL, updateCustomerAddress() is broken
//
// 3. Check "Collected Address[X] ... streetNum=[X]"
//    - If this is NULL, the update didn't persist
//
// 4. Check "FINAL Address[X] in request: streetNum=[X]"
//    - If this is NULL, setCustomerAddress() didn't work
//
// 5. Check the hashCodes - they should all be the SAME object
//    - If hashCodes change, objects are being copied/replaced
//
// ============================================================================
