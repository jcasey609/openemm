/*********************************************************************************
 * The contents of this file are subject to the OpenEMM Public License Version 1.1
 * ("License"); You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.agnitas.org/openemm.
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied.  See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is OpenEMM.
 * The Initial Developer of the Original Code is AGNITAS AG. Portions created by
 * AGNITAS AG are Copyright (C) 2006 AGNITAS AG. All Rights Reserved.
 *
 * All copies of the Covered Code must include on each user interface screen,
 * visible to all users at all times
 *    (a) the OpenEMM logo in the upper left corner and
 *    (b) the OpenEMM copyright notice at the very bottom center
 * See full license, exhibit B for requirements.
 ********************************************************************************/

package org.agnitas.web;

import javax.servlet.http.*;
import org.apache.struts.action.*;
import org.apache.struts.util.*;
import org.agnitas.target.*;
import org.agnitas.target.impl.*;
import java.util.*;

public class EmmActionForm extends StrutsFormBase {
    
    private String shortname;
    private String description;
    private int actionID;
    private int action;
    private ArrayList actions;
    
    public EmmActionForm() {
    }
    
    /**
     * Validate the properties that have been set from this HTTP request,
     * and return an <code>ActionErrors</code> object that encapsulates any
     * validation errors that have been found.  If no errors are found, return
     * <code>null</code> or an <code>ActionErrors</code> object with no
     * recorded error messages.
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     * @return errors
     */
    public ActionErrors validate(ActionMapping mapping,
            HttpServletRequest request) {
        
        ActionErrors errors = new ActionErrors();
        
        if(request.getParameter("add.x")!=null) {
            this.setAction(EmmActionAction.ACTION_ADD_MODULE);
        }
        
        if(this.getAction()==EmmActionAction.ACTION_NEW) {
            this.actionID=0;
            this.shortname=null;
            this.actions=null;
            this.description=null;
            this.deleteModule=0;
            this.type=0;
            this.action=EmmActionAction.ACTION_VIEW;
        }
        
        if(request.getParameter("save.x")!=null) {
            if(this.shortname!=null && this.shortname.length()<1) {
                errors.add("shortname", new ActionMessage("error.nameToShort"));
            }
        }
        
        if(request.getParameter("deleteModule")!=null) {
            if(this.actions!=null) {
                this.actions.remove(this.deleteModule);
            }
        }
        
        return errors;
    }
    
    /**
     * Getter for property shortname.
     *
     * @return Value of property shortname.
     */
    public String getShortname() {
        return this.shortname;
    }
    
    /**
     * Setter for property shortname.
     *
     * @param shortname New value of property shortname.
     */
    public void setShortname(String shortname) {
        this.shortname = shortname;
    }
    
    /**
     * Getter for property description.
     *
     * @return Value of property description.
     */
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Setter for property description.
     *
     * @param description New value of property description.
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Getter for property actionID.
     *
     * @return Value of property actionID.
     */
    public int getActionID() {
        
        return this.actionID;
    }
    
    /**
     * Setter for property actionID.
     *
     * @param actionID New value of property actionID.
     */
    public void setActionID(int actionID) {
        
        this.actionID = actionID;
    }
    
    /**
     * Getter for property action.
     *
     * @return Value of property action.
     */
    public int getAction() {
        return this.action;
    }
    
    /**
     * Setter for property action.
     *
     * @param action New value of property action.
     */
    public void setAction(int action) {
        this.action = action;
    }
    
    /**
     * Getter for property actions.
     *
     * @return Value of property actions.
     */
    public ArrayList getActions() {
        
        return this.actions;
    }
    
    /**
     * Setter for property actions.
     *
     * @param actions New value of property actions.
     */
    public void setActions(ArrayList actions) {
        
        this.actions = actions;
    }
    
    /**
     * Holds value of property type.
     */
    private int type;
    
    /**
     * Getter for property type.
     *
     * @return Value of property type.
     */
    public int getType() {
        
        return this.type;
    }
    
    /**
     * Setter for property type.
     *
     * @param type New value of property type.
     */
    public void setType(int type) {
        
        this.type = type;
    }
    
    /**
     * Holds value of property deleteModule.
     */
    private int deleteModule;
    
    /**
     * Getter for property deleteModule.
     * 
     * @return Value of property deleteModule.
     */
    public int getDeleteModule() {
        
        return this.deleteModule;
    }
    
    /**
     * Setter for property deleteModule.
     *
     * @param deleteModule New value of property deleteModule.
     */
    public void setDeleteModule(int deleteModule) {
        
        this.deleteModule = deleteModule;
    }
    
    /**
     * Holds value of property newModule.
     */
    private String newModule;
    
    /**
     * Getter for property newModule.
     *
     * @return Value of property newModule.
     */
    public String getNewModule() {
        
        return this.newModule;
    }
    
    /**
     * Setter for property newModule.
     *
     * @param newModule New value of property newModule.
     */
    public void setNewModule(String newModule) {
        
        this.newModule = newModule;
    }
    
}
