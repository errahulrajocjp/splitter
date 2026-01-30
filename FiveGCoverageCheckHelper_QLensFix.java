package onevz.soe.smbenrollment.helper;

import onevz.soe.smbenrollment.model.CustomerAddress;
import onevz.soe.smbenrollment.requests.spectrumadapter.AddressLineBased;
import onevz.soe.smbenrollment.requests.spectrumadapter.NautilusAddressInfo;
import onevz.soe.smbenrollment.requests.spectrumadapter.NautilusQualificationRequest;
import onevz.soe.smbenrollment.requests.spectrumadapter.SplitAddressRequest;
import onevz.soe.smbenrollment.constants.SmbConstants;
import onevz.soe.util.CollectionUtilities;
import onevz.soe.util.StringUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FiveGCoverageCheckHelper {

    private static final Logger logger = LoggerFactory.getLogger(FiveGCoverageCheckHelper.class);

    public SplitAddressRequest buildSplitCustomerAddressRequest(CustomerAddress customerAddress) {
        SplitAddressRequest splitRequest = new SplitAddressRequest();
        AddressLineBased addressLineBased = new AddressLineBased();

        addressLineBased.setAddressLine1(customerAddress.getAddressLine1());
        addressLineBased.setAddressLine2(customerAddress.getAddressLine2());
        addressLineBased.setCity(customerAddress.getCity());
        addressLineBased.setState(customerAddress.getState());
        addressLineBased.setZipCode(customerAddress.getZipCode());

        splitRequest.setAddressLineBased(addressLineBased);
        splitRequest.setClientAppName(SmbConstants.CLIENT_ID);
        splitRequest.setStrictValidationRequired(false);
        splitRequest.setTraffic(SmbConstants.TRAFFIC);

        return splitRequest;
    }

    // ╔══════════════════════════════════════════════════════════════════════════════╗
    // ║  LOGICAL ERROR FIX: Flawed Record Identifier Logic Leading to Data Mismatch  ║
    // ║                                                                              ║
    // ║  PROBLEM: Original code used loop index 'i' for recordIdentifier but skipped ║
    // ║  invalid addresses, creating gaps (e.g., "1", "3" instead of "1", "2")       ║
    // ║                                                                              ║
    // ║  SOLUTION: Use dedicated counter (recordIdCounter) that only increments      ║
    // ║  for VALID addresses, ensuring sequential recordIdentifiers with no gaps     ║
    // ╚══════════════════════════════════════════════════════════════════════════════╝
    public NautilusQualificationRequest buildNautilusQualificationRequest(List<CustomerAddress> customerAddresses) {
        
        // Validation - throw exception if list is null or empty
        if (CollectionUtilities.isEmptyOrNull(customerAddresses)) {
            logger.error("Customer addresses list is null or empty");
            throw new IllegalArgumentException("Customer addresses list cannot be null or empty");
        }

        NautilusQualificationRequest request = new NautilusQualificationRequest();
        request.setIncludeCband(true);
        request.setRequestType(SmbConstants.NAUTILUS_REQUEST_TYPE);

        List<NautilusAddressInfo> nautilusAddressList = new ArrayList<>();
        
        // ┌─────────────────────────────────────────────────────────────────────────┐
        // │  FIX: Use dedicated counter for valid addresses only                    │
        // │  OLD CODE: setRecordIdentifier(String.valueOf(i + SmbConstants.RECORD_IDENTIFIER_BASE))  │
        // │  NEW CODE: setRecordIdentifier(String.valueOf(++recordIdCounter))       │
        // └─────────────────────────────────────────────────────────────────────────┘
        int recordIdCounter = 0;  // ← NEW: Dedicated counter for sequential IDs

        for (int i = 0; i < customerAddresses.size(); i++) {
            CustomerAddress address = customerAddresses.get(i);

            // Validation: Check for null address object
            if (address == null) {
                logger.warn("Skipping null address at index {}", i);
                continue;
            }

            // Validation: Check required fields
            if (StringUtilities.isEmptyOrNull(address.getAddressLine1()) ||
                    StringUtilities.isEmptyOrNull(address.getZipCode())) {
                logger.warn("Skipping address at index {} - missing addressLine1 or zipCode", i);
                continue;
            }

            NautilusAddressInfo nautilusAddress = new NautilusAddressInfo();
            
            // ┌─────────────────────────────────────────────────────────────────────┐
            // │  FIX: Increment counter ONLY for valid items, ensuring sequential   │
            // │  recordIdentifiers: "1", "2", "3" (no gaps)                         │
            // └─────────────────────────────────────────────────────────────────────┘
            nautilusAddress.setRecordIdentifier(String.valueOf(++recordIdCounter));  // ← FIXED

            // Set fields with null-safe defaults
            nautilusAddress.setAddressLine1(address.getAddressLine1());
            nautilusAddress.setAddressLine2(StringUtilities.isNotEmptyOrNull(address.getAddressLine2()) 
                    ? address.getAddressLine2() : SmbConstants.EMPTY_STRING);
            nautilusAddress.setCity(StringUtilities.isNotEmptyOrNull(address.getCity()) 
                    ? address.getCity() : SmbConstants.EMPTY_STRING);
            nautilusAddress.setState(StringUtilities.isNotEmptyOrNull(address.getState()) 
                    ? address.getState() : SmbConstants.EMPTY_STRING);
            nautilusAddress.setZip(address.getZipCode());

            nautilusAddressList.add(nautilusAddress);
        }

        if (CollectionUtilities.isEmptyOrNull(nautilusAddressList)) {
            logger.error("No valid addresses found after validation");
            throw new IllegalArgumentException("No valid addresses available for qualification check");
        }

        request.setAddressList(nautilusAddressList);
        logger.debug("Built Nautilus request with {} valid addresses", nautilusAddressList.size());

        return request;
    }
}
