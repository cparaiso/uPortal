/**
 * Copyright � 2001 The JA-SIG Collaborative.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Redistributions of any form whatsoever must retain the following
 *    acknowledgment:
 *    "This product includes software developed by the JA-SIG Collaborative
 *    (http://www.jasig.org/)."
 *
 * THIS SOFTWARE IS PROVIDED BY THE JA-SIG COLLABORATIVE "AS IS" AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE JA-SIG COLLABORATIVE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * formatted with JxBeauty (c) johann.langhofer@nextra.at
 */


package  org.jasig.portal.channels.UserPreferences;

import  org.jasig.portal.*;
import  org.w3c.dom.Document;
import  org.xml.sax.DocumentHandler;
import  java.io.File;


/** <p>Manages User Layout, user preferences and profiles </p>
 * @author Peter Kharchenko, peterk@interactivebusiness.com
 * @author Ken Weiner, kweiner@interactivebusiness.com
 * @version $Revision$
 */
public class CUserPreferences implements IPrivilegedChannel {
  UserLayoutManager ulm;
  ChannelRuntimeData runtimeData = null;
  ChannelStaticData staticData = null;
  StylesheetSet set = null;
  private static final String fs = File.separator;
  private static final String portalBaseDir = GenericPortalBean.getPortalBaseDir();
  String stylesheetDir = portalBaseDir + fs + "webpages" + fs + "stylesheets" + fs + "org" + fs + "jasig" + fs + "portal" + fs + "channels" + fs + "CUserPreferences";
  private UserPreferences up = null;
  private Document userLayoutXML = null;
  private int mode;
  public static final int MANAGE_PREFERENCES = 1;
  public static final int MANAGE_PROFILES = 2;
  IPrivilegedChannel internalState = null;
  IPrivilegedChannel managePreferences = null;
  IPrivilegedChannel manageProfiles = null;
  protected IUserPreferencesStore updb;
  private PortalControlStructures pcs;
    private boolean initialized=false;
    UserProfile editedProfile=null;

  /**
   * put your documentation comment here
   */
  public CUserPreferences () {
    this.runtimeData = new ChannelRuntimeData();
    this.set = new StylesheetSet(stylesheetDir + fs + "CUserPreferences.ssl");
    this.set.setMediaProps(portalBaseDir + fs + "properties" + fs + "media.properties");

    manageProfiles = new ManageProfilesState(this);
    //managePreferences = new GPreferencesState(this);
    //internalState = managePreferences;

  }

  /**
   * put your documentation comment here
   * @return
   */
  protected UserLayoutManager getUserLayoutManager () {
    return  ulm;
  }

  /**
   * put your documentation comment here
   * @return
   */
  protected UserPreferences getCurrentUserPreferences () {
    return  up;
  }

  /**
   * put your documentation comment here
   * @return
   */
  protected ChannelRuntimeData getRuntimeData () {
    return  runtimeData;
  }

  /**
   * put your documentation comment here
   * @return
   */
  protected StylesheetSet getStylesheetSet () {
    return  set;
  }

  /**
   * put your documentation comment here
   * @param pcs
   * @exception PortalException
   */
  public void setPortalControlStructures (PortalControlStructures pcs) throws PortalException {
    if (ulm == null)
      ulm = pcs.getUserLayoutManager();
    if (up == null)
      up = ulm.getUserPreferencesCopy();
    // instantiate the browse state here
    this.pcs = pcs;

    if (!initialized) {
	instantiateManagePreferencesState(up.getProfile());
	// Initial state should be manage preferences
	internalState = managePreferences;
        internalState.setStaticData(staticData);
	editedProfile=up.getProfile();
	initialized=true;
    }
    if(internalState!=null) {
	internalState.setPortalControlStructures(pcs);
    }
  }


    /**
     * Instantiates appropriate managePreferences object.
     *
     * @param profile profile for which preferences are to be edited
     */
    private void instantiateManagePreferencesState(UserProfile profile) {
	try {
	    ThemeStylesheetDescription tsd = RdbmServices.getCoreStylesheetDescriptionImpl().getThemeStylesheetDescription(profile.getThemeStylesheetId());
	    if(tsd!=null) {
		String cupmClass = tsd.getCustomUserPreferencesManagerClass();
		managePreferences = (IPrivilegedChannel)Class.forName(cupmClass).newInstance();
		((BaseState)managePreferences).setContext(this);
	    } else {
		Logger.log(Logger.ERROR,"CUserPreferences::instantiateManagePreferencesState() : unable to retrieve theme stylesheet description. stylesheetId="+profile.getThemeStylesheetId());
		managePreferences = new GPreferencesState(this);
	    }
	} catch (Exception e) {
	    Logger.log(Logger.ERROR, e);
	    managePreferences = new GPreferencesState(this);
	}
    }

  /** Returns channel runtime properties
   * @return handle to runtime properties
   */
  public ChannelRuntimeProperties getRuntimeProperties () {
    // Channel will always render, so the default values are ok
    return  new ChannelRuntimeProperties();
  }

  /** Processes layout-level events coming from the portal
   * @param ev a portal layout event
   */
  public void receiveEvent (PortalEvent ev) {
    // no events for this channel
    internalState.receiveEvent(ev);
  }

  /** Receive static channel data from the portal
   * @param sd static channel data
   */
  public void setStaticData (ChannelStaticData sd) throws PortalException {
    this.staticData = sd;
  }

  /** CUserPreferences listens for an HttpRequestParameter "userPreferencesAction"
   * and based on its value changes state between profile management and layout/stylesheet
   * preferences.
   * @param rd handle to channel runtime data
   */
  public void setRuntimeData (ChannelRuntimeData rd) throws PortalException {
    this.runtimeData = rd;
    String action = runtimeData.getParameter("userPreferencesAction");
    if (action != null) {
      Integer profileId = null;
      try {
        profileId = new Integer(runtimeData.getParameter("profileId"));
      } catch (NumberFormatException nfe) {}
      ;
      boolean systemProfile = false;
      if (profileId != null) {
        String profileType = runtimeData.getParameter("proifileType");
        if (profileType != null && profileType.equals("system"))
          systemProfile = true;
      }
      if (action.equals("manageProfiles")) {
        if (profileId != null) {
        //if(!profile.equals(currentProfileName)) {
        // need a new manage preferences state
        //			}
        }
        else {
          // reset to the manage profiles state
	    //          manageProfiles.setRuntimeData(rd);
          this.internalState = manageProfiles;
        }
      }
      else if (action.equals("managePreferences")) {
	  if (profileId != null) {
	      // find the profile mapping
	      updb = RdbmServices.getUserPreferencesStoreImpl();
	      if (systemProfile) {
		  UserProfile newProfile = updb.getSystemProfileById(profileId.intValue());
		  if(newProfile!=null && (!(editedProfile.isSystemProfile() && editedProfile.getProfileId()==newProfile.getProfileId()))) {
		      // new profile has been selected
		      editedProfile=newProfile;
		      instantiateManagePreferencesState(editedProfile);
		  }
	      } else {
		  UserProfile newProfile = updb.getUserProfileById(ulm.getPerson().getID(), profileId.intValue());
		  if(newProfile!=null && (editedProfile.isSystemProfile() || (editedProfile.getProfileId()!=newProfile.getProfileId()))) {
		      // new profile has been selected
		      editedProfile=newProfile;
		      instantiateManagePreferencesState(editedProfile);
		  }
	      }
	  }

	  if(editedProfile==null) {
	      editedProfile = up.getProfile();
	  }
	  //        managePreferences.setRuntimeData(rd);
	  this.internalState = managePreferences;
      }
    }
    if (internalState != null)
      internalState.setRuntimeData(rd);
  }

  /**
   * Output channel content to the portal
   * @param out a sax document handler
   */
  public void renderXML (DocumentHandler out) throws PortalException {
    internalState.renderXML(out);
  }

  /**
   * put your documentation comment here
   * @exception PortalException
   */
  private void prepareSaveChanges () throws PortalException {
    // write code to persist the userLayoutXML to the session
    // and the database (remember, as the user interacts with this
    // channel, changes are only made to a copy of the userLayoutXML
    // until this method is called)
    ulm.setNewUserLayoutAndUserPreferences(userLayoutXML, up);
  }

  protected UserPreferences getUserPreferencesFromStore(UserProfile profile) throws Exception {
      IUserPreferencesStore upStore = new RDBMUserPreferencesStore();
      up = upStore.getUserPreferences(getUserLayoutManager().getPerson().getID(), profile);
      up.synchronizeWithUserLayoutXML(GenericPortalBean.getUserLayoutStore().getUserLayout(getUserLayoutManager().getPerson().getID(), getCurrentUserPreferences().getProfile().getProfileId()));
      return up;
  }
}



