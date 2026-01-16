// ============================================================================
// PRODUCTION-READY VERSION - Refactored per Architect's Recommendation
// ============================================================================

public Mono<SoeDataWrapper<SmbResponseWrapper<FiveGCoverageCheckResponse>>> validateSplitAddress(
        FiveGCoverageCheckRequest fiveGCoverageCheckRequest) {
    
    logger.info("Starting validateSplitAddress with {} customer addresses", 
            fiveGCoverageCheckRequest.getCustomerAddress() != null ? 
                    fiveGCoverageCheckRequest.getCustomerAddress().size() : 0);
    
    if (!isInValidCustomerAddress(fiveGCoverageCheckRequest.getCustomerAddress())) {
        
        // ARCHITECT'S RECOMMENDATION: Use Flux.fromIterable().flatMap() instead of List<Mono<...>>
        return Flux.fromIterable(fiveGCoverageCheckRequest.getCustomerAddress())
                .flatMap(customerAddress -> {
                    SplitAddressRequest splitAddressRequest = fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(customerAddress);
                    
                    return spectrumAdapterService.splitAddress(splitAddressRequest)
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
                })
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

// ============================================================================
// KEY IMPROVEMENTS PER ARCHITECT'S RECOMMENDATION:
// ============================================================================
//
// BEFORE (Antipattern):
// List<Mono<CustomerAddress>> addressProcessingTasks = new ArrayList<>();
// for (CustomerAddress customerAddress : list) {
//     Mono<CustomerAddress> task = ...;
//     addressProcessingTasks.add(task);
// }
// return Flux.concat(addressProcessingTasks)
//
// AFTER (Recommended):
// return Flux.fromIterable(list)
//     .flatMap(customerAddress -> {
//         return ...Mono processing...;
//     })
//
// BENEFITS:
// 1. More readable and idiomatic reactive code
// 2. Avoids creating intermediate List<Mono<...>> collection
// 3. Better performance - no need to materialize all Monos first
// 4. Cleaner code flow - declarative vs imperative
// 5. Follows reactive best practices
//
// ============================================================================
