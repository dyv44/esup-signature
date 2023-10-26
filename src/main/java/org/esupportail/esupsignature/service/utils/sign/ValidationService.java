package org.esupportail.esupsignature.service.utils.sign;

import eu.europa.esig.dss.enumerations.TokenExtractionStrategy;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.SignaturePolicyProvider;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.executor.ValidationLevel;
import eu.europa.esig.dss.validation.reports.Reports;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dss.DssUtils;
import org.esupportail.esupsignature.dss.model.DssMultipartFile;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    private CertificateVerifier certificateVerifier;

    @Autowired(required = false)
    public void setCertificateVerifier(CertificateVerifier certificateVerifier) {
        this.certificateVerifier = certificateVerifier;
    }

    @Resource
    protected FileService fileService;

    @Resource
    protected SignaturePolicyProvider signaturePolicyProvider;

    @Resource
    private org.springframework.core.io.Resource defaultPolicy;

    public Reports validate(InputStream docInputStream, InputStream signInputStream) {
        try {
            List<DSSDocument> detachedContents = new ArrayList<>();
            SignedDocumentValidator documentValidator;
            if(signInputStream != null && signInputStream.available() > 0) {
                detachedContents.add(DssUtils.toDSSDocument(new DssMultipartFile("doc", "doc", null, docInputStream)));
                documentValidator = SignedDocumentValidator.fromDocument(Objects.requireNonNull(DssUtils.toDSSDocument(new DssMultipartFile("sign", "sign", null, signInputStream))));
                documentValidator.setDetachedContents(detachedContents);
            } else {
                DSSDocument dssDocument = DssUtils.toDSSDocument(new DssMultipartFile("doc", "doc", null, docInputStream));
                if(dssDocument != null) {
                    documentValidator = SignedDocumentValidator.fromDocument(dssDocument);
                } else {
                    return null;
                }
            }
            logger.debug("validate with : " + documentValidator.getClass());
            if(certificateVerifier != null) {
                documentValidator.setCertificateVerifier(certificateVerifier);
            }
            documentValidator.setSignaturePolicyProvider(new SignaturePolicyProvider());
            documentValidator.setTokenExtractionStrategy(TokenExtractionStrategy.NONE);
            documentValidator.setLocale(Locale.FRENCH);
            documentValidator.setValidationLevel(ValidationLevel.LONG_TERM_DATA);
            documentValidator.setSignaturePolicyProvider(signaturePolicyProvider);
            documentValidator.setIncludeSemantics(true);

            Reports reports = null;
            try {
                InputStream is = defaultPolicy.getInputStream();
                reports = documentValidator.validateDocument(is);
            } catch (Exception e) {
                logger.error("Unable to parse policy : " + e.getMessage(), e);
            }
            return reports;
        } catch (DSSException | UnsupportedOperationException | IOException e) {
            logger.warn("Unable to read document : " + e.getMessage());
        }
        return null;
    }

}
