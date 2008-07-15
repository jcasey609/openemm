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

package org.agnitas.taglib;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;
import java.io.*;
import org.agnitas.util.*;
import org.agnitas.beans.Admin;

public class PermissionTag extends BodyBase {
    
    private String pageMode;
    
     /**
     * Setter for property token.
     * 
     * @param mode New value of property token.
     */
    public void setToken(String mode) {
        pageMode=mode;
    }
    
    /**
     * permission control
     */
    public int doStartTag() throws JspTagException {
        HttpSession session=pageContext.getSession();
        
        Admin aAdmin=(Admin)session.getAttribute("emm.admin");
        
        if(aAdmin==null) {
            throw new JspTagException("PermissionDenied$" + pageMode);
        }
        
        if(!aAdmin.permissionAllowed(pageMode))
            throw new JspTagException("PermissionDenied$" + pageMode);
        
        return SKIP_BODY;
    }
}
