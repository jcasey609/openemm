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

package org.agnitas.beans.impl;

import java.util.*;
import java.io.*;
import org.agnitas.beans.*;
import org.apache.commons.lang.*;
import org.agnitas.util.*;
import javax.mail.internet.*;
import org.springframework.context.*;

/**
 *
 * @author  mhe
 */
public class MediatypeEmailImpl extends MediatypeImpl implements MediatypeEmail, Serializable {
    
    /** Holds value of property subject. */
    protected String subject="";
    
    /** Holds value of property linefeed. */
    protected int linefeed;
    
    /** Holds value of property mailFormat. */
    protected int mailFormat=2;
    
    /** Holds value of property charset. */
    protected String charset="ISO-8859-1";
    
    /** Holds value of property fromAdr. */
    protected String fromEmail="";

    /** Holds value of property fromAdr. */
    protected String fromFullname="";

    /**
     * Complete Reply-To Address.
     */
    /** Holds value of property replyEmail. */
    protected String replyEmail="";

    /** Holds value of property replyFullname. */
    protected String replyFullname="";
    
    /** Creates a new instance of MediaTypeEmail */
    public MediatypeEmailImpl() {
        template="[agnDYN name=\"Text\"/]";
        htmlTemplate="[agnDYN name=\"HTML-Version\"/]";
    }
     
    /** Getter for property subject.
     * @return Value of property subject.
     *
     */
    public String getSubject() {
        return subject;
    }
    
    /** Setter for property subject.
     * @param subject New value of property subject.
     *
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    /** Getter for property fromAdr.
     * @return Value of property fromAdr.
     *
     */
    public String getFromEmail() {
        return fromEmail;
    }
    
    /** Setter for property fromAdr.
     * @param fromAdr New value of property fromAdr.
     *
     */
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
    
    /** Getter for property fromAdr.
     * @return Value of property fromAdr.
     *
     */
    public String getFromFullname() {
        return this.fromFullname;
    }
    
    /** Setter for property fromAdr.
     * @param fromAdr New value of property fromAdr.
     *
     */
    public void setFromFullname(String fromFullname) {
        this.fromFullname = fromFullname;
    }
    
    public String getFromAdr() throws Exception {
        InternetAddress tmpFrom=new InternetAddress(
                                           this.fromEmail, this.fromFullname,
                                           "utf-8"); 
        return AgnUtils.propertySaveString(tmpFrom.toString());
    }

    /** Getter for property linefeed.
     * @return Value of property linefeed.
     *
     */
    public int getLinefeed() {
        return this.linefeed;
    }
    
    /** Setter for property linefeed.
     * @param linefeed New value of property linefeed.
     *
     */
    public void setLinefeed(int linefeed) {
        this.linefeed = linefeed;
    }
    
    /** Getter for property mailFormat.
     * @return Value of property mailFormat.
     *
     */
    public int getMailFormat() {
        return mailFormat;
    }
    
    /** Setter for property mailFormat.
     * @param mailFormat New value of property mailFormat.
     *
     */
    public void setMailFormat(int mailFormat) {
        this.mailFormat = mailFormat;
    }
    
    /** Getter for property charset.
     * @return Value of property charset.
     *
     */
    public String getCharset() {
        return this.charset;
    }
    
    /** Setter for property charset.
     * @param charset New value of property charset.
     *
     */
    public void setCharset(String charset) {
        this.charset = charset;
    }
    
    public String getParam() throws Exception {
        StringBuffer result=new StringBuffer();
        InternetAddress tmpFrom=new InternetAddress(
                                           this.fromEmail, this.fromFullname,
                                           "utf-8"); 
        if(replyEmail== null) {
            replyEmail=fromEmail;
        }
        if(replyFullname== null) {
            replyFullname=fromFullname;
        }
        InternetAddress tmpReply=new InternetAddress(
                                           this.replyEmail, this.replyFullname,
                                           "utf-8"); 
        result.append("from=\"");
        result.append(tmpFrom.toString());
        result.append("\", ");
        
        result.append("subject=\"");
        result.append(AgnUtils.propertySaveString(this.subject));
        result.append("\", ");
        
        result.append("charset=\"");
        result.append(AgnUtils.propertySaveString(this.charset));
        result.append("\", ");
        
        result.append("linefeed=\"");
        result.append(AgnUtils.propertySaveString(Integer.toString(this.linefeed)));
        result.append("\", ");
        
        result.append("mailformat=\"");
        result.append(AgnUtils.propertySaveString(Integer.toString(this.mailFormat)));
        result.append("\", ");
        
        result.append("reply=\"");
        result.append(tmpReply.toString());
        result.append("\", ");
        
        result.append("onepixlog=\"");
        result.append(AgnUtils.propertySaveString(this.onepixel));
        result.append("\", ");

        super.setParam(result.toString());
        return result.toString();
    }

    public void setParam(String param) throws Exception {
        int tmp=0;
        String from=AgnUtils.findParam("from", param);

        if(from.length() > 0) {
            InternetAddress adr=new InternetAddress(from);

            this.fromEmail=adr.getAddress();
            this.fromFullname=adr.getPersonal();
        } else {
            this.fromEmail="";
            this.fromFullname="";
        }
        
        from=AgnUtils.findParam("reply", param);
        if(from==null) {
            from=AgnUtils.findParam("from", param);
        }
        
        if(from.length() > 0) {
            InternetAddress adr=new InternetAddress(from);

            this.replyEmail=adr.getAddress();
            this.replyFullname=adr.getPersonal();
        } else {
            this.replyEmail="";
            this.replyFullname="";
        }
        
        this.charset=AgnUtils.findParam("charset", param);
        if(this.charset==null) {
            this.charset="ISO-8859-1";
        }
        this.subject=AgnUtils.findParam("subject", param);
        try {
            tmp=Integer.parseInt(AgnUtils.findParam("mailformat", param));
        } catch (Exception e) {
            tmp=2; // default: Offline-HTML
        }
        this.mailFormat=tmp;
        try {
            tmp=Integer.parseInt(AgnUtils.findParam("linefeed", param));
        } catch (Exception e) {
            tmp=72; // default: after 72 characters
        }
        this.linefeed=tmp;
        
        this.onepixel=AgnUtils.findParam("onepixlog", param);
        if(this.onepixel==null) {
            this.onepixel=MediatypeEmailImpl.ONEPIXEL_NONE;
        }
    }
    
    /**
     * Getter for property replyAdr.
     * @return Value of property replyAdr.
     */
    public String getReplyAdr() throws Exception {
        InternetAddress tmpReply=new InternetAddress(
                                           this.replyEmail, this.replyFullname,
                                           "utf-8"); 
        return AgnUtils.propertySaveString(tmpReply.toString());
    }
    
    /**
     * Getter for property replyEmail.
     * @return Value of property replyEmail.
     */
    public String getReplyEmail() {
        return replyEmail;
    }
    
    /**
     * Setter for property replyAdr.
     * @param replyAdr New value of property replyAdr.
     */
    public void setReplyEmail(String replyEmail) {
        this.replyEmail = replyEmail;
    }

    /**
     * Getter for property replyFullname.
     * @return Value of property replyFullname.
     */
    public String getReplyFullname() {
        return replyFullname;
    }
    
    /**
     * Setter for property replyFullname.
     * @param replyFullname New value of property replyFullname.
     */
    public void setReplyFullname(String replyFullname) {
        this.replyFullname = replyFullname;
    }

    /**
     * Holds value of property onepixel.
     */
    protected String onepixel = MediatypeEmailImpl.ONEPIXEL_NONE;

    /**
     * Getter for property onepixel.
     * @return Value of property onepixel.
     */
    public String getOnepixel() {

        return this.onepixel;
    }

    /**
     * Setter for property onepixel.
     * @param onepixel New value of property onepixel.
     */
    public void setOnepixel(String onepixel) {
        this.onepixel = onepixel;
    }

    /**
     * Holds value of property mailingID.
     */
    protected int mailingID;

    /**
     * Getter for property mailingID.
     * @return Value of property mailingID.
     */
    public int getMailingID() {

        return this.mailingID;
    }

    /**
     * Setter for property mailingID.
     * @param mailingID New value of property mailingID.
     */
    public void setMailingID(int mailingID) {

        this.mailingID = mailingID;
    }

    /**
     * Holds value of property onepixel.
     */
    protected String htmlTemplate;

    /**
     * Getter for property onepixel.
     * @return Value of property onepixel.
     */
    public String getHtmlTemplate() {
        return htmlTemplate;
    }

    /**
     * Setter for property onepixel.
     * @param onepixel New value of property onepixel.
     */
    public void setHtmlTemplate(String htmlTemplate) {
        this.htmlTemplate = htmlTemplate;
    }

    public void syncTemplate(Mailing mailing, ApplicationContext con) { 
        MailingComponent component;

        component=mailing.getTextTemplate();
        if(component!=null) {
            component.setEmmBlock(template);
            component.setBinaryBlock(template.getBytes());
        }

        component=mailing.getHtmlTemplate();
        if(component!=null) {
            component.setEmmBlock(htmlTemplate);
            component.setBinaryBlock(htmlTemplate.getBytes());
        }
    }
}
