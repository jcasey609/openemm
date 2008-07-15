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

package org.agnitas.dao;

import org.agnitas.beans.Mailloop;
import org.springframework.context.ApplicationContextAware;
import java.util.List;

/**
 *
 * @author Martin Helff
 */
public interface MailloopDao extends ApplicationContextAware {
    /**
     * Deletes mailloop.
     *
     * @return true==success
     *false==errror
     */
    boolean deleteMailloop(int loopID, int companyID);

    /**
     * Getter for property mailloop by mailloop id and company id.
     *
     * @return Value of mailloop.
     */
    Mailloop getMailloop(int mailloopID, int companyID);

    /**
     * Saves mailloop.
     *
     * @return Saved mailloop id.
     */
    int saveMailloop(Mailloop loop);
    
    /**
     * Getter for property mailloops by company id.
     *
     * @return Value of mailloops.
     */
    List getMailloops(int companyID);
    
}