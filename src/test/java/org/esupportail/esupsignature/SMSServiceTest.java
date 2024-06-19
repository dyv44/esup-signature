package org.esupportail.esupsignature;

import org.esupportail.esupsignature.service.interfaces.sms.SmsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = EsupSignatureApplication.class)
@TestPropertySource(properties = {"app.scheduling.enable=false"})
public class SMSServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(SMSServiceTest.class);

    @Autowired(required = false)
    private SmsService smsService;

    @Test
    public void testSms() {
        assumeTrue(smsService != null, "SMS not configured");
        try {
            smsService.testSms();
        } catch (Exception e) {
            logger.error("Send mail failed", e);
            fail();
        }
    }

}
