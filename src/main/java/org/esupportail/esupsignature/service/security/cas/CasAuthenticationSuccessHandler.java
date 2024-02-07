package org.esupportail.esupsignature.service.security.cas;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.UserType;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CasAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private static final Logger logger = LoggerFactory.getLogger(CasAuthenticationSuccessHandler.class);

	@Resource
	private UserService userService;

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	@Transactional
	public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException {
		logger.info("authentication success for " + authentication.getName());
		String name = httpServletRequest.getHeader("sn");
		String firstname = httpServletRequest.getHeader("givenName");
		String email = httpServletRequest.getHeader("mail");
		if(email == null) {
			throw new EsupSignatureRuntimeException("user must have an email");
		}
        User user = userService.createUserWithAuthentication(null, name, firstname, email, authentication, UserType.ldap);
		if(!user.getManagersRoles().isEmpty()) {
			CasAuthenticationToken auth = (CasAuthenticationToken) authentication;
			List<GrantedAuthority> updatedAuthorities = new ArrayList<>(auth.getAuthorities());
			updatedAuthorities.add(new SimpleGrantedAuthority("ROLE_MANAGER"));
			Authentication newAuth = new CasAuthenticationToken("EsupSignatureCAS", auth.getPrincipal(), auth.getCredentials(), updatedAuthorities, auth.getUserDetails(), auth.getAssertion());
			SecurityContextHolder.getContext().setAuthentication(newAuth);
		}
		httpServletRequest.getSession().setAttribute("securityServiceName", "CasSecurityServiceImpl");
		DefaultSavedRequest defaultSavedRequest = (DefaultSavedRequest) httpServletRequest.getSession().getAttribute("SPRING_SECURITY_SAVED_REQUEST");
		if(defaultSavedRequest != null) {
			String queryString = defaultSavedRequest.getQueryString();
			if (queryString != null && queryString.split("=")[0].equals("redirect")) {
				if(!queryString.split("=", 2)[1].equals("null")) {
					String url = queryString.split("=", 2)[1];
					this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, url);
					return;
				}
			}
		}
		this.redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/");
	}

}