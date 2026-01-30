package onevz.soe.smbenrollment.constants;

/**
 * Constants used across SMB Enrollment service.
 */
public final class SmbConstants {

    private SmbConstants() {
        // Prevent instantiation
    }

    // ==================================================================================
    // EXISTING CONSTANTS
    // ==================================================================================
    public static final String SPECTRUM_ADAPTER = "spectrumAdapter";
    public static final String NAUTILUS_ADAPTER = "nautilusAdapter";
    public static final String SPLIT_ADDRESS_API = "/splitAddress";
    public static final String NAUTILUS_QUALIFICATION_API = "/nautilus/qualification";
    public static final String CLIENT_ID = "ATG-RTL-NETACM";
    public static final String TRAFFIC = "LIVE";
    public static final String NAUTILUS_REQUEST_TYPE = "BULK";
    
    // Status codes
    public static final int SUCCESS_STATUS = 1;
    public static final int FAILURE_STATUS = 0;
    public static final String SUCCESS_RETURN_CODE = "0";
    
    // Common constants
    public static final String EMPTY_STRING = "";

    // ==================================================================================
    // ERROR MESSAGES - FIX #8 (Lines 254,245,325)
    // Standardized error message constants for consistency
    // ==================================================================================
    public static final String ERROR_INVALID_ADDRESS = "Invalid customer address: addressLine1 and zipCode are required";
    public static final String ERROR_SPLIT_ADDRESS_FAILED = "Address validation failed: Invalid split address response";
    public static final String ERROR_SPLIT_ADDRESS_SERVICE = "Address validation failed: ";
    public static final String ERROR_NO_QUALIFICATION_DATA = "No qualification data received from Nautilus service";
    public static final String ERROR_NO_QUALIFICATION_FOR_ADDRESS = "No qualification data found for this address";
    public static final String ERROR_5G_COVERAGE_CHECK_FAILED = "5G coverage check failed: ";
    public static final String ERROR_ALL_ADDRESSES_FAILED_SPLIT = "All addresses failed split address validation";

    // ==================================================================================
    // STATUS MESSAGES
    // ==================================================================================
    public static final String STATUS_MSG_QUALIFIED = "This address qualifies for 5G service.";
    public static final String STATUS_MSG_NOT_QUALIFIED = "This address does not qualify for 5G service.";
    public static final String STATUS_MSG_ADDRESS_VALIDATED = "Address validated successfully";
}
