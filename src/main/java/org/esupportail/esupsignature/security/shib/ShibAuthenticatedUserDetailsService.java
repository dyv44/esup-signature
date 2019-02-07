/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.security.shib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.esupportail.esupsignature.security.Group2UserRoleService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class ShibAuthenticatedUserDetailsService
implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> {

	
	protected Map<String, String> mappingGroupesRoles;
	
	protected Group2UserRoleService group2UserRoleService;

	
	public void setMappingGroupesRoles(Map<String, String> mappingGroupesRoles) {
		this.mappingGroupesRoles = mappingGroupesRoles;
	}
	
	public void setGroup2UserRoleService(Group2UserRoleService group2UserRoleService) {
		this.group2UserRoleService = group2UserRoleService;
	}
	
	public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws AuthenticationException {
		List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
		String credentials = (String)token.getCredentials();
		for(String credential : StringUtils.split(credentials, ";")) {
			if(mappingGroupesRoles != null && mappingGroupesRoles.containsKey(credential)){ 
				authorities.add(new SimpleGrantedAuthority(mappingGroupesRoles.get(credential)));
			}
		}
		for(String roleFromLdap : group2UserRoleService.getRoles(token.getName())) {
			authorities.add(new SimpleGrantedAuthority(roleFromLdap));
		}
		return createUserDetails(token, authorities);
	}

	protected UserDetails createUserDetails(Authentication token, Collection<? extends GrantedAuthority> authorities) {
		return new User(token.getName(), "N/A", true, true, true, true, authorities);
	}
}
