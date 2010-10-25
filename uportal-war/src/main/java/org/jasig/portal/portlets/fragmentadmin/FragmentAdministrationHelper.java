/**
 * 
 */
package org.jasig.portal.portlets.fragmentadmin;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;

import org.jasig.portal.layout.dlm.FragmentDefinition;
import org.jasig.portal.layout.dlm.LegacyConfigurationLoader;
import org.jasig.portal.security.IAuthorizationPrincipal;
import org.jasig.portal.security.IAuthorizationService;
import org.jasig.portal.security.IPerson;
import org.jasig.portal.security.provider.AuthorizationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.webflow.execution.RequestContext;

/**
 * Helper class for the FragmentAdministration web flow.
 * 
 * @author Nicholas Blair, npblair@wisc.edu
 *
 */
@Service("fragmentAdministrationHelper")
public class FragmentAdministrationHelper {

	private static final String UP_USERS = "UP_USERS";
	private static final String IMPERSONATE = "IMPERSONATE";
	private LegacyConfigurationLoader legacyConfigurationLoader;
	
	/**
	 * @param legacyConfigurationLoader the legacyConfigurationLoader to set
	 */
	@Autowired
	public void setLegacyConfigurationLoader(
			LegacyConfigurationLoader legacyConfigurationLoader) {
		this.legacyConfigurationLoader = legacyConfigurationLoader;
	}

	/**
	 * 
	 * @param remoteUser
	 * @return
	 */
	public Map<String, String> getAuthorizedDlmFragments(String remoteUser) {
		List<FragmentDefinition> fragments = this.legacyConfigurationLoader.getFragments();
		IAuthorizationService authorizationService = AuthorizationImpl.singleton();
		IAuthorizationPrincipal principal = authorizationService.newPrincipal(remoteUser, IPerson.class);
		Map<String, String> results = new TreeMap<String, String>();
		for(FragmentDefinition frag: fragments) {
			if(principal.hasPermission(UP_USERS, IMPERSONATE, frag.getOwnerId())) {
				results.put(frag.getOwnerId(), frag.getName());
			}
		}
		return results;
	}
	
	/**
	 * 
	 * @param remoteUser
	 * @param targetFragmentOwner
	 * @return "yes" for success, "no" otherwise
	 */
	public String swapToFragmentOwner(final String remoteUser, final String targetFragmentOwner, RequestContext requestContext) {
		IAuthorizationService authorizationService = AuthorizationImpl.singleton();
		IAuthorizationPrincipal principal = authorizationService.newPrincipal(remoteUser, IPerson.class);
		if(principal.hasPermission(UP_USERS, IMPERSONATE, targetFragmentOwner)) {
			PortletRequest portletRequest = (PortletRequest) requestContext.getExternalContext().getNativeRequest();
			PortletSession session = portletRequest.getPortletSession();
			session.setAttribute(org.jasig.portal.LoginServlet.SWAP_TARGET_UID, targetFragmentOwner, javax.portlet.PortletSession.APPLICATION_SCOPE);
			return "yes";
		}
		return "no";
	}
}
