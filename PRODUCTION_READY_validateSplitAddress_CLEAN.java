// ============================================================================
// PRODUCTION-READY VERSION - Clean code without verbose logging
// ============================================================================

public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("Starting validateSplitAddress with {} customer addresses", 
            fiveGCoverageCheckRequest.getCustomerAddress() != null ? 
                    fiveGCoverageCheckRequest.getCustomerAddress().size() : 0);
    
    if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
        
        List<Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress>> addressProcessingTasks = new ArrayList<>();
        
        for (onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress customerAddress : fiveGCoverageCheckRequest.getCustomerAddress()) {
            
            SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);
            
            Mono<onevz.soe.smbenrollment.requests.spectrumadapter.CustomerAddress> task = spectrumAdapterService.splitAddress(splitAddressRequest)
                    .flatMap(addressResponse -> {
                        
                        if (isValidSplitAddressResponse(addressResponse)) {
                            onevz.soe.smbenrollment.responses.spectrumadapter.Address address = extractSplitAddress(addressResponse);
                            
                            if (address != null) {
                                updateCustomerAddress(customerAddress, address);
                                logger.info("Customer address updated successfully from split address service");
                            }
                        }
                        
                        return Mono.just(customerAddress);
                    })
                    .doOnError(e -> logger.error("Error occurred from split address service for address: {}. Error: {}", 
                            customerAddress.getAddressLine1(), e.getMessage(), e))
                    .onErrorResume(error -> {
                        logger.error("Handling error gracefully for address: {}", customerAddress.getAddressLine1());
                        return Mono.just(customerAddress);
                    });
            
            addressProcessingTasks.add(task);
        }
        
        return Flux.concat(addressProcessingTasks)
                .collectList()
                .flatMap(updatedAddresses -> {
                    logger.info("All split address service calls completed successfully");
                    
                    fiveGCoverageCheckRequest.setCustomerAddress(updatedAddresses);
                    
                    return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest);
                })
                .map(this::buildSuccessResponse)
                .onErrorResume(this::buildErrorResponse);
    }
    
    logger.warn("Customer addresses are invalid. Skipping split address service calls");
    return processValidateCustomerAccountForFiveGCoverage(fiveGCoverageCheckRequest)
            .map(this::buildSuccessResponse)
            .onErrorResume(this::buildErrorResponse);
}
