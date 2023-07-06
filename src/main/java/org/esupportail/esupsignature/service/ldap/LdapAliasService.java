package org.esupportail.esupsignature.service.ldap;

import org.esupportail.esupsignature.config.ldap.LdapProperties;
import org.esupportail.esupsignature.service.ldap.entry.AliasLdap;
import org.esupportail.esupsignature.service.ldap.mapper.AliasLdapAttributesMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.List;

@Service
@ConditionalOnProperty({"spring.ldap.base"})
@EnableConfigurationProperties(LdapProperties.class)
public class LdapAliasService {

    private static final Logger logger = LoggerFactory.getLogger(LdapAliasService.class);

    @Resource
    private LdapProperties ldapProperties;

    @Resource
    private LdapTemplate ldapTemplate;

    public List<AliasLdap> searchByMail(String mail) {
        logger.debug("search AliasLdap by mail");
        String formattedFilter = MessageFormat.format("(mail=*{0})", (Object[]) new String[] { mail });
        StringBuilder objectClasses = new StringBuilder();
        for(String objectClass : ldapProperties.getAliasObjectClasses()) {
            objectClasses.append("(objectClass=").append(objectClass).append(")");
        }
        formattedFilter = "(&(|" + objectClasses + ")" + formattedFilter + ")";
        logger.debug("search AliasLdap by eppn : " + formattedFilter);
        LdapQuery ldapQuery = LdapQueryBuilder.query().countLimit(10).filter(formattedFilter);
        return ldapTemplate.search(ldapQuery, new AliasLdapAttributesMapper());
    }

}