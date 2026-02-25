package com.kavun.backend.service.security;

import com.kavun.config.properties.ClamAVProperties;
import com.kavun.constant.ClamAVConstants;
import com.kavun.exception.VirusDetectedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * Service for virus scanning using ClamAV.
 * Scans uploaded files for viruses and malware before storing them.
 *
 * @author Yunus Emre Alpu
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClamAVService {

    private final ClamAVProperties clamAVProperties;

    /**
     * Scan a file for viruses using ClamAV.
     *
     * @param file The file to scan
     * @return ScanResult containing scan details
     * @throws VirusDetectedException if virus is detected and action is REJECT
     * @throws IOException            if there's an error reading the file
     */
    public ScanResult scanFile(MultipartFile file) throws IOException {
        if (!clamAVProperties.isEnabled()) {
            LOG.debug("ClamAV scanning is disabled. Skipping scan for file: {}", file.getOriginalFilename());
            return null;
        }

        LOG.info("Scanning file for viruses: fileName={}, size={}", file.getOriginalFilename(), file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            ClamavClient client = new ClamavClient(clamAVProperties.getHost(), clamAVProperties.getPort());

            ScanResult result = client.scan(inputStream);

            if (result instanceof ScanResult.OK) {
                LOG.info("File is clean: {}", file.getOriginalFilename());
                return result;
            } else if (result instanceof ScanResult.VirusFound virusFound) {
                Map<String, Collection<String>> viruses = virusFound.getFoundViruses();
                String virusSignature = viruses.keySet().toString();
                LOG.warn("Virus detected in file: fileName={}, virus={}", file.getOriginalFilename(), virusSignature);
                throw new VirusDetectedException(file.getOriginalFilename(), virusSignature);
            } else {
                LOG.error("Unexpected scan result type: {}", result.getClass().getName());
                throw new RuntimeException("Unexpected scan result from ClamAV");
            }

        } catch (VirusDetectedException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Error scanning file with ClamAV: fileName={}, error={}",
                    file.getOriginalFilename(), e.getMessage(), e);

            // If ClamAV is not available, decide whether to fail or allow
            if (clamAVProperties.isEnabled()) {
                throw new RuntimeException(ClamAVConstants.VIRUS_SCANNER_NOT_AVAILABLE, e);
            }

            return null;
        }
    }

    /**
     * Check if a file contains viruses.
     *
     * @param file The file to check
     * @return true if virus is detected, false otherwise
     */
    public boolean isVirusDetected(MultipartFile file) {
        try {
            ScanResult result = scanFile(file);
            return result instanceof ScanResult.VirusFound;
        } catch (VirusDetectedException e) {
            return true;
        } catch (Exception e) {
            LOG.error("Error checking virus status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if ClamAV service is available and responding.
     *
     * @return true if ClamAV is available, false otherwise
     */
    public boolean isAvailable() {
        if (!clamAVProperties.isEnabled()) {
            return false;
        }

        try {
            ClamavClient client = new ClamavClient(clamAVProperties.getHost(), clamAVProperties.getPort());
            client.ping();
            LOG.debug("ClamAV service is available");
            return true;
        } catch (Exception e) {
            LOG.warn("ClamAV service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get ClamAV version information.
     *
     * @return Version string or null if not available
     */
    public String getVersion() {
        if (!clamAVProperties.isEnabled()) {
            return null;
        }

        try {
            ClamavClient client = new ClamavClient(clamAVProperties.getHost(), clamAVProperties.getPort());
            return client.version();
        } catch (Exception e) {
            LOG.warn("Failed to get ClamAV version: {}", e.getMessage());
            return null;
        }
    }
}
