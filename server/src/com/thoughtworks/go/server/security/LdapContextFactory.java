/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.security;

import javax.naming.directory.DirContext;

import com.thoughtworks.go.config.LdapConfig;
import com.thoughtworks.go.config.SecurityConfig;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.SpringSecurityContextSource;
import org.springframework.stereotype.Service;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Service
public class LdapContextFactory implements SpringSecurityContextSource {

    private GoConfigService goConfigService;
    private DefaultSpringSecurityContextSource delegate;

    public void initialize() {
        goConfigService.register(new LdapConfigChangedListener(goConfigService.ldapConfig(), this));
    }

    @Autowired
    public LdapContextFactory(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    private DefaultSpringSecurityContextSource delegator() {
        if (delegate == null) {
            initializeDelegator();
        }
        return delegate;
    }

    void initializeDelegator() {
        //LdapAuthenticationProvider has checked that LDAP config directoryExists
        SecurityConfig securityConfig = goConfigService.security();
        LdapConfig ldapConfig = securityConfig.ldapConfig();
        if (ldapConfig.isEnabled()) {
            try {
                delegate = new DefaultSpringSecurityContextSource(ldapConfig.uri());

                //so user can define the variable java.naming.referral=follow in the server.sh
                delegate.setBaseEnvironmentProperties(System.getProperties());
                new LdapContextSourceConfigurator(ldapConfig).configure(delegate);
                delegate.afterPropertiesSet();
            } catch (Exception e) {
                throw bomb("Invalid or empty ldap config, Error creating DefaultSpringSecurityContextSource", e);
            }
        }

    }

    //---------------------------------------------------- delegating methods

    public DirContext getReadWriteContext(String userDn, Object credentials) {
        return delegator().getReadWriteContext(userDn, credentials);
    }

    public DirContext getReadOnlyContext() throws NamingException {
        return delegator().getReadOnlyContext();
    }

    public DirContext getReadWriteContext() throws NamingException {
        return delegator().getReadWriteContext();
    }

    public DistinguishedName getBaseLdapPath() {
        return delegator().getBaseLdapPath();
    }

    public String getBaseLdapPathAsString() {
        return delegator().getBaseLdapPathAsString();
    }
}
