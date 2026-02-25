package com.kavun.exception;

import lombok.Getter;

/**
 * Exception thrown when a virus or malware is detected in an uploaded file.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Getter
public class VirusDetectedException extends RuntimeException {

    private final String fileName;
    private final String virusSignature;

    public VirusDetectedException(String fileName, String virusSignature) {
        super(String.format("Virus detected in file '%s': %s", fileName, virusSignature));
        this.fileName = fileName;
        this.virusSignature = virusSignature;
    }

    public VirusDetectedException(String fileName) {
        super(String.format("Virus detected in file '%s'", fileName));
        this.fileName = fileName;
        this.virusSignature = "Unknown";
    }
}
