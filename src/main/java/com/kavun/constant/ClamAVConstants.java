package com.kavun.constant;

/**
 * Constants for ClamAV virus scanner.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
public final class ClamAVConstants {

    // Messages
    public static final String VIRUS_DETECTED = "Detected a virus in the file!";
    public static final String VIRUS_SCAN_FAILED = "Virus scan failed!";
    public static final String VIRUS_SCANNER_NOT_AVAILABLE = "Virus scanner service is not available!";
    public static final String FILE_CLEAN = "File is clean, no virus detected.";
    public static final String FILE_QUARANTINED = "File quarantined.";
    public static final String SCANNING_FILE = "Scanning file for viruses...";

    private ClamAVConstants() {
        throw new AssertionError(ErrorConstants.NOT_INSTANTIABLE);
    }
}
