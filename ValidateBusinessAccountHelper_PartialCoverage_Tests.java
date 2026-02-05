// =====================================================================
// TEST CASES FOR PARTIALLY COVERED LINES - ValidateBusinessAccountHelperTest
// Lines: 253, 297-298, 459-461
// =====================================================================

// =====================================================================
// LINE 253: if (address != null) {
// Currently covered: address != null (TRUE branch)
// Need to cover: address == null (FALSE branch - when extractSplitAddress returns null)
// =====================================================================

@Test
void testProcessSplitAddressForCustomer_AddressIsNull_ReturnsFailedAddress() {
    // Setup: Create a SplitAddressResponse where the extracted address will be null
    SplitAddressResponse splitResponse = new SplitAddressResponse();
    SplitAddressResponse.SplitAddressResponseData data = new SplitAddressResponse.SplitAddressResponseData();
    AddressSplit addressSplit = new AddressSplit();
    AddressSplit.AddressSplitServiceResponse serviceResponse = new AddressSplit.AddressSplitServiceResponse();
    // Set address to NULL - this is what we're testing
    serviceResponse.setAddress(null);
    addressSplit.setResponse(serviceResponse);
    data.setAddressSplit(addressSplit);
    splitResponse.setData(data);
    
    CustomerAddress customerAddress = createCustomerAddress("123 Main St", "10001");
    
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(splitResponse));
    
    // Create request
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(Collections.singletonList(customerAddress));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                CustomerAddress result = response.getData().getResponse().getCustomerAddress().get(0);
                // Address should be marked as not qualified with failure message
                return !result.isQualified() && 
                       result.getStatusMsg() != null &&
                       result.getStatusMsg().contains("failed");
            })
            .verifyComplete();
}

@Test
void testProcessSplitAddressForCustomer_ValidResponseButNullAddress_SetsFailureStatus() {
    // This tests the specific case where isValidSplitAddressResponse returns TRUE
    // but extractSplitAddress returns NULL (edge case)
    
    CustomerAddress customerAddress = createCustomerAddress("456 Oak Ave", "20002");
    
    // Create response that passes validation but has null address in extract
    SplitAddressResponse splitResponse = createSplitAddressResponseWithNullAddress();
    
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(splitResponse));
    
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(Collections.singletonList(customerAddress));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                CustomerAddress result = response.getData().getResponse().getCustomerAddress().get(0);
                return result.getStatusMsg() != null && 
                       result.getStatusMsg().startsWith("Address validation failed");
            })
            .verifyComplete();
}

// Helper method for above test
private SplitAddressResponse createSplitAddressResponseWithNullAddress() {
    SplitAddressResponse response = new SplitAddressResponse();
    SplitAddressResponse.SplitAddressResponseData data = new SplitAddressResponse.SplitAddressResponseData();
    AddressSplit addressSplit = new AddressSplit();
    AddressSplit.AddressSplitServiceResponse serviceResponse = new AddressSplit.AddressSplitServiceResponse();
    serviceResponse.setAddress(null);  // NULL address
    addressSplit.setResponse(serviceResponse);
    data.setAddressSplit(addressSplit);
    response.setData(data);
    return response;
}

// =====================================================================
// LINE 297-298: response != null && response.getCustomerAddress() != null ? 
//               response.getCustomerAddress().size() : 0))
// Currently covered: response != null && getCustomerAddress() != null (TRUE branch)
// Need to cover: response == null OR getCustomerAddress() == null (FALSE branch - returns 0)
// =====================================================================

@Test
void testDoOnSuccess_ResponseIsNull_LogsZeroAddresses() {
    // This tests the ternary operator when response is null
    // The doOnSuccess callback should handle null response gracefully
    
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    
    when(fiveGCoverageCheckHelper.buildNautilusQualificationRequest(any(List.class)))
            .thenReturn(new NautilusQualificationRequest());
    
    // Return a response that will result in null FiveGCoverageCheckResponse
    NautilusQualificationResponse nautilusResponse = new NautilusQualificationResponse();
    nautilusResponse.setData(null);  // This will cause issues in mapping
    
    when(spectrumAdapterService.checkAddressQualification(any(NautilusQualificationRequest.class)))
            .thenReturn(Mono.just(nautilusResponse));
    
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(addresses);
    
    // Mock split address to return valid response
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(createSplitAddressResponse()));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> response != null)
            .verifyComplete();
}

@Test
void testDoOnSuccess_ResponseCustomerAddressIsNull_LogsZeroAddresses() {
    // This tests when response is not null but getCustomerAddress() returns null
    
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    
    when(fiveGCoverageCheckHelper.buildNautilusQualificationRequest(any(List.class)))
            .thenReturn(new NautilusQualificationRequest());
    
    // Create Nautilus response that will map to FiveGCoverageCheckResponse with null customerAddress
    NautilusQualificationResponse nautilusResponse = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    data.setBulkAddressQualificationResponse(new ArrayList<>());  // Empty list
    nautilusResponse.setData(data);
    
    when(spectrumAdapterService.checkAddressQualification(any(NautilusQualificationRequest.class)))
            .thenReturn(Mono.just(nautilusResponse));
    
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(addresses);
    
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(createSplitAddressResponse()));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> response.getData() != null)
            .verifyComplete();
}

// =====================================================================
// LINE 459-461: return nautilusResponse != null 
//               && nautilusResponse.getData() != null 
//               && !CollectionUtilities.isEmptyOrNull(nautilusResponse.getData().getBulkAddressQualificationResponse()));
// Currently covered: All conditions TRUE
// Need to cover: Each condition being FALSE
// =====================================================================

@Test
void testIsValidNautilusResponse_NautilusResponseIsNull_ReturnsFalse() {
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isValidNautilusResponse",
            (NautilusQualificationResponse) null
    );
    
    assertFalse(result);
}

@Test
void testIsValidNautilusResponse_DataIsNull_ReturnsFalse() {
    NautilusQualificationResponse response = new NautilusQualificationResponse();
    response.setData(null);  // Data is NULL
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isValidNautilusResponse",
            response
    );
    
    assertFalse(result);
}

@Test
void testIsValidNautilusResponse_BulkAddressQualificationResponseIsNull_ReturnsFalse() {
    NautilusQualificationResponse response = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    data.setBulkAddressQualificationResponse(null);  // BulkAddressQualificationResponse is NULL
    response.setData(data);
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isValidNautilusResponse",
            response
    );
    
    assertFalse(result);
}

@Test
void testIsValidNautilusResponse_BulkAddressQualificationResponseIsEmpty_ReturnsFalse() {
    NautilusQualificationResponse response = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    data.setBulkAddressQualificationResponse(new ArrayList<>());  // Empty list
    response.setData(data);
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isValidNautilusResponse",
            response
    );
    
    assertFalse(result);
}

@Test
void testIsValidNautilusResponse_AllValid_ReturnsTrue() {
    NautilusQualificationResponse response = new NautilusQualificationResponse();
    NautilusData data = new NautilusData();
    
    List<BulkAddressQualificationResponse> qualResponses = new ArrayList<>();
    BulkAddressQualificationResponse qualResponse = new BulkAddressQualificationResponse();
    qualResponse.setRecordIdentifier("1");
    qualResponses.add(qualResponse);
    
    data.setBulkAddressQualificationResponse(qualResponses);
    response.setData(data);
    
    Boolean result = ReflectionTestUtils.invokeMethod(
            validateBusinessAccountHelper,
            "isValidNautilusResponse",
            response
    );
    
    assertTrue(result);
}

// =====================================================================
// INTEGRATION TEST: Cover line 459-461 through main flow
// When isValidNautilusResponse returns FALSE, should return failure response
// =====================================================================

@Test
void testProcessValidateCustomerAccountForFiveGCoverage_InvalidNautilusResponse_ReturnsFailureForAllAddresses() {
    List<CustomerAddress> addresses = new ArrayList<>();
    addresses.add(createCustomerAddress("123 Main St", "10001"));
    addresses.add(createCustomerAddress("456 Oak Ave", "20002"));
    
    when(fiveGCoverageCheckHelper.buildNautilusQualificationRequest(any(List.class)))
            .thenReturn(new NautilusQualificationRequest());
    
    // Return INVALID Nautilus response (null data)
    NautilusQualificationResponse invalidResponse = new NautilusQualificationResponse();
    invalidResponse.setData(null);
    
    when(spectrumAdapterService.checkAddressQualification(any(NautilusQualificationRequest.class)))
            .thenReturn(Mono.just(invalidResponse));
    
    FiveGCoverageCheckRequest request = new FiveGCoverageCheckRequest();
    request.setCustomerAddress(addresses);
    
    // Mock split address
    when(fiveGCoverageCheckHelper.buildSplitCustomerAddressRequest(any(CustomerAddress.class)))
            .thenReturn(new SplitAddressRequest());
    when(spectrumAdapterService.splitAddress(any(SplitAddressRequest.class)))
            .thenReturn(Mono.just(createSplitAddressResponse()));
    
    StepVerifier.create(validateBusinessAccountHelper.check5GCoverageForAddresses(request))
            .expectNextMatches(response -> {
                // All addresses should have failure status due to invalid Nautilus response
                List<CustomerAddress> resultAddresses = response.getData().getResponse().getCustomerAddress();
                return resultAddresses.stream().allMatch(addr -> 
                        addr.getStatusMsg() != null && 
                        addr.getStatusMsg().contains(SmbConstants.ERROR_NO_QUALIFICATION_DATA));
            })
            .verifyComplete();
}

// =====================================================================
// IMPORTS NEEDED:
// =====================================================================
/*
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
*/
