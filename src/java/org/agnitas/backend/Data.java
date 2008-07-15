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
package org.agnitas.backend;

import	java.sql.Date;
import	java.sql.Time;
import	java.sql.Timestamp;
import	java.sql.ResultSet;
import	java.sql.ResultSetMetaData;
import	java.sql.Connection;
import	java.sql.SQLException;
import	java.util.Vector;
import	java.io.File;
import	java.util.HashSet;
import	java.util.Hashtable;
import	java.util.Enumeration;
import	java.util.Locale;
import	java.util.TimeZone;
import	java.text.SimpleDateFormat;
import	org.agnitas.util.Config;
import	org.agnitas.util.ConfigException;
import	org.agnitas.util.Log;
import	org.agnitas.target.TargetRepresentation;
import org.agnitas.beans.BindingEntry;

/** Class holding most of central configuration and global database
 * information
 */
class Data extends Config {
    /** the file to read the configuration from */
    final static String	INI_FILE = "Mailgun.ini";
    /** the property pointing to a filename passed on startup */
    final static String	INI_PROP = "org.agnitas.backend.ini_filename";
    /** all available variables in the config file */
    final static String[]	INI_VARS = {
        "LOGLEVEL",
        "MAILDIR",
        "RESERVED1",
        "RESERVED2",
        "RESERVED3",
        "RESERVED4",
        "RESERVED5",
        "DEFAULT_ENCODING",
        "DEFAULT_CHARSET",
        "DB_LOGIN",
        "DB_PASSWORD",
        "SQL_CONNECT",
        // file mode specific
        "BLOCKSIZE",
        "METADIR",
        "XMLBACK",
        "XMLVALIDATE",
        // general output specific
        "SAMPLE_EMAILS",
        "DOMAIN",
        "BOUNDARY",
        "EOL",
        "MAILER",
        "MAIL_LOG_NUMBER",
        "START_MESSAGE",
        "END_MESSAGE"
    };
    /** Available output file formats */
    final static String[]	OUT_MODES = {
        "meta"
    };
    /** Output file format constant: META */
    final static int	OUT_META = 0;
    /** default output file format */
    final static int	DEFAULT_OUT_MODE = OUT_META;
    /** Available modes for output format meta */
    final static String[]	META_MODES = {
        "xml",
        "xml/gz"
    };
    /** Mode for output format meta XML */
    final static int	OUT_META_XML = 0;
    /** Mode for output format meta XML gzip'd */
    final static int	OUT_META_XMLGZ = 1;
    /** default mode for output format meta */
    final static int	DEFAULT_META_MODE = OUT_META_XML;

    /** default value for domain entry */
    final static String	DEF_DOMAIN = "openemm.org";
    /** default value for boundary entry */
    final static String	DEF_BOUNDARY = "AGNITAS";
    /** default value for EOL coding */
    final static String	DEF_EOL = "\r\n";
    /** default value for X-Mailer: header */
    final static String	DEF_MAILER = "OpenEMM/Agnitas AG V5.0";
    /** minimal size of a block */
    final static int	MIN_BLOCK_SIZE = 500;
    /** maximum size of a block */
    final static int	MAX_BLOCK_SIZE = 10000;

    /** Constant for onepixellog: no automatic insertion */
    final static int	OPL_NONE = 0;
    /** Constant for onepixellog: insertion on top */
    final static int	OPL_TOP = 1;
    /** Constant for onepixellog: insertion at bottom */
    final static int	OPL_BOTTOM = 2;

    /** Loglevel */
    private int		logLevel = Log.ERROR;
    /** directory to write admin-/testmails to */
    private String		mailDir = null;
    /** default encoding for all blocks */
    private String		defaultEncoding = null;
    /** default character set for all blocks */
    private String		defaultCharset = null;
    /** database login */
    private String		dbLogin = null;
    /** database password */
    private String		dbPassword = null;
    /** database connect expression */
    private String		sqlConnect = null;
    /** used block size */
    private int		blockSize = 0;
    /** directory to store meta files for further processing */
    private String		metaDir = null;
    /** name of program to execute meta files */
    private String		xmlBack = "xmlback";
    /** validate each generated block */
    private boolean		xmlValidate = false;
    /** Send samples of worldmailing to dedicated address(es) */
    private String		sampleEmails = null;
    /** write a DB record after creating that number of receiver */
    private int		mailLogNumber = 0;
    /** flag wether we need to update the mailing status */
    private boolean		setMailingStatus = false;
    /** flag if we should write the end message */
    private boolean		writeEndMessage = true;
    /** freetext start messages */
    private String		startMessage = "generating in progress";
    /** freetext end message */
    private String		endMessage = "generating successfully ended";
    
    /** the used output format */
    public int		outMode = DEFAULT_OUT_MODE;
    /** the used meta output format */
    public int		metaMode = DEFAULT_META_MODE;
    /** in case of campaing mailing, send mail only to this customer */
    public long		campaignCustomerID = 0;
    /** in case of a transaction, use this transaction ID */
    public long		campaignTransactionID = 0;
    /** for campaign mailings, use this user status in the binding table */
    public long		campaignUserStatus = BindingEntry.USER_STATUS_ACTIVE;
    /** a counter to enforce uniqueness on compaign mails */
    public long		pass = 0;
    /** alternative campaign mailing selection */
    public TargetRepresentation
                campaignSubselect = null;
    /** custom generated tags */
    public Vector		customTags = null;
    /** custom generated tags with values */
    public Hashtable	customMap = null;
    /** overwtite existing database fields */
    public Hashtable	overwriteMap = null;
    /** overwtite existing database fields for more receivers */
    public Hashtable	overwriteMapMulti = null;
    /** virtual database fields */
    public Hashtable	virtualMap = null;
    /** virtual database fields for more receivers */
    public Hashtable	virtualMapMulti = null;
    /** instance to write logs to */
    private Log		log = null;
    /** the ID to write as marker in the logfile */
    private String		lid = null;
    /** the connection to the database */
    public DBase		dbase = null;
    /** a list of all available tables */
    private HashSet		tables = null;
    /** status_id from maildrop_status_tbl */
    public long		maildrop_status_id = -1;
    /** assigned company to this mailing */
    public long		company_id = -1;
    /** mailinglist assigned to this mailing */
    public long		mailinglist_id = -1;
    /** mailign_id of this mailing */
    public long		mailing_id = -1;
    /** status_field from maildrop_status_tbl */
    public String		status_field = null;
    /** when to send the mailing, date */
    public Date		senddate = null;
    /** when to send the mailing, time */
    public Time		sendtime = null;
    /** when to send the mailing, date+time */
    public Timestamp	sendtimestamp = null;
    /** current send date, calculated from sendtimstamp and stepping */
    public java.util.Date	currentSendDate = null;
    /** the currentSendDate in epoch */
    public long		sendSeconds = 0;
    /** steps in seconds between two entities */
    public long		step = 0;
    /** number of blocks per entity */
    public int		blocksPerStep = 1;
    /** the subselection for the receiver of this mailing */
    public String		subselect = null;
    /** the name of this mailing */
    public String		mailing_name = null;
    /** the subject for this mailing */
    public String		subject = null;
    /** the sender address for this mailing */
    public EMail		from_email = null;
    /** the optional reply-to address for this mailing */
    public EMail		reply_to = null;
    /** the encoding for this mailing */
    public String		encoding = null;
    /** the charachter set for this mailing */
    public String		charset = null;
    /** domain used to build message-ids */
    public String		domain = DEF_DOMAIN;
    /** boundary part to build multipart messages */
    public String		boundary = DEF_BOUNDARY;
    /** EOL coding for spoolfiles */
    public String		eol = DEF_EOL;
    /** content of the X-Mailer: header */
    public String		mailer = DEF_MAILER;
    /** the base for the profile URL */
    public String		profileURL = null;
    /** the base for the unsubscribe URL */
    public String		unsubscribeURL = null;
    /** the base for the auto URL */
    public String		autoURL = null;
    /** the base for the onepixellog URL */
    public String		onePixelURL = null;
    /** the largest mailtype to generate */
    public int		masterMailtype = 2;
    /** default line length in text part */
    public int		lineLength = 72;
    /** where to automatically place the onepixellog */
    public int		onepixlog = OPL_NONE;
    /** Password for signatures */
    public String		password = null;
    /** the base domain to build the base URLs */
    public String		rdirDomain = null;
    /** Collection of media information */
    public Media		media = null;
    /** Bitfield of available media types in mailing */
    public long		availableMedias = 0;
    /** number of all subscriber of a mailing */
    public long		totalSubscribers = -1;
    /** all URLs from rdir_url_tbl */
    public Vector		URLlist = null;
    /** number of entries in URLlist */
    public int		urlcount = 0;
    /** all title tags */
    public Hashtable	titles = null;
    /** usage of title tags 0 unused, 1 title, 2 titlefull in use */
    public int		titleUsage = 0;
    /** layout of the customer table */
    public Vector		layout = null;
    /** number of entries in layout */
    public int		lcount = 0;
    /** number of entries in layout used */
    public int		lusecount = 0;
    /** name of the company (for logfile display only) */
    public String		company_name = null;
    /** name of mailtracking table */
    public String		mailtracking_table = null;
    /** optional information to append to end message */
    public String		extraEndMessage = null;
    /** for housekeeping of created files */
    private Vector		toRemove = null;

    /** check if database is available
     */
    private void checkDatabase () throws ConfigException {
        if (dbase == null)
            throw new ConfigException ("Database not available");
    }
    
    /**
     * setup database connection and retreive a list of all available
     * tables
     * @param conn an optional existing database connection
     */
    private void setupDatabase (Connection conn) throws ConfigException {
        try {
            dbase = new DBase (this, conn);
            tables = new HashSet ();
            
            ResultSet	rset;
            
            rset = dbase.execQuery ("SHOW TABLES");
            while (rset.next ())
                tables.add (rset.getString (1).toLowerCase ());
            rset.close ();
        } catch (Exception e) {
            throw new ConfigException ("Database setup failed: " + e);
        }
    }
    
    /** close a database and free all assigned data
     */
    private void closeDatabase () throws ConfigException {
        if (dbase != null) {
            try {
                dbase.done ();
                dbase = null;
                tables = null;
            } catch (Exception e) {
                throw new ConfigException ("Database close failed: " + e);
            }
        }
    }

    /**
     * find an entry from the media record for this mailing
     * @param m instance of media record
     * @param id the ID to look for
     * @param dflt a default value if no entry is found
     * @return the found entry or the default
     */
    private String findMediadata (Media m, String id, String dflt) {
        Vector	v;
        String	rc;
        
        v = m.findParameterValues (id);
        rc = null;
        if ((v != null) && (v.size () > 0))
            rc = (String) v.elementAt (0);
        return rc == null ? dflt : rc;
    }
    
    /**
     * find a numeric entry from the media record for this mailing
     * @param m instance of media record
     * @param id the ID to look for
     * @param dflt a default value if no entry is found
     * @return the found entry or the default
     */
    private int ifindMediadata (Media m, String id, int dflt) {
        String	tmp = findMediadata (m, id, null);
        int	rc;
        
        if (tmp != null)
            try {
                rc = Integer.parseInt (tmp);
            } catch (Exception e) {
                rc = dflt;
            }
        else
            rc = dflt;
        return rc;
    }
    
    /**
     * find a boolean entry from the media record for this mailing
     * @param m instance of media record
     * @param id the ID to look for
     * @param dflt a default value if no entry is found
     * @return the found entry or the default
     */
    private boolean bfindMediadata (Media m, String id, boolean dflt) {
        String	tmp = findMediadata (m, id, null);
        boolean	rc = dflt;
        
        if (tmp != null)
            if (tmp.length () == 0)
                rc = true;
            else {
                	String tok = tmp.substring (0, 1).toLowerCase ();
                
                if (tok.equals ("t") || tok.equals ("y") ||
                    tok.equals ("+") || tok.equals ("1"))
                    rc = true;
            } else
                rc = false;
        return rc;
    }

    /**
     * query all basic information about this mailing
     * @param status_id the reference to the mailing
     */
    private void queryMailingInformations (String status_id) throws ConfigException {
        ResultSet	rset;
        int		bs;
        int		genstat;

        checkDatabase ();
        try {
            maildrop_status_id = Long.parseLong (status_id);
            
            // get the first information block from maildrop_status_tbl
            rset = dbase.simpleQuery ("SELECT company_id, mailing_id, status_field, senddate, step, blocksize, genstatus " +
                          "FROM maildrop_status_tbl " +
                          "WHERE status_id = " + maildrop_status_id);
            company_id = rset.getLong (1);
            mailing_id = rset.getLong (2);
            status_field = dbase.getValidString (rset, 3);
            sendtimestamp = rset.getTimestamp (4);
            step = rset.getLong (5);
            bs = rset.getInt (6);
            genstat = rset.getInt (7);
            rset.close ();
            if (bs > 0)
                setBlockSize (bs);
            if (genstat != 1)
                throw new ConfigException ("Generation state is not 1, but " + genstat);
            if (isAdminMailing () || isTestMailing () || isWorldMailing () || isRuleMailing ()) {
                int	rowcount = 0;
                
                try {
                    rowcount = dbase.execUpdate ("UPDATE maildrop_status_tbl SET genchange = " + dbase.sysdate + ", genstatus = 2 " +
                                     "WHERE status_id = " + maildrop_status_id + " AND genstatus = 1");
                } catch (Exception e) {
                    throw new ConfigException ("Unable to update generation state to 2: " + e.toString ());
                }
                if (rowcount != 1)
                    throw new ConfigException ("Update of maildrop_status_tbl affects " + rowcount + " rows, not exactly one");
            }
            //
            // get the second block from mailing_tbl
            // get subject, from_email-adress, urls
            String	f_email = null;
            String	r_to = null;
            String	targetExpr;
            rset = dbase.simpleQuery ("SELECT mailinglist_id, shortname, target_expression " +
                          "FROM mailing_tbl WHERE mailing_id = " + mailing_id);
            mailinglist_id = rset.getLong (1);
            mailing_name = rset.getString (2);
            targetExpr = dbase.getValidString (rset, 3);
            rset.close ();


            if (targetExpr != null) {
                StringBuffer	buf = new StringBuffer ();
                int		tlen = targetExpr.length ();
                
                for (int n = 0; n < tlen; ++n) {
                    char	ch = targetExpr.charAt (n);
                    
                    if ((ch == '(') || (ch == ')')) {
                        buf.append (ch);
                    } else if ((ch == '&') || (ch == '|')) {
                        if (ch == '&')
                            buf.append (" AND");
                        else
                            buf.append (" OR");
                        while (((n + 1) < tlen) && (targetExpr.charAt (n + 1) == ch))
                            ++n;
                    } else if (ch == '!') {
                        buf.append (" NOT");
                    } else if ("0123456789".indexOf (ch) != -1) {
                        int	newn = n;
                        int	tid = 0;
                        int	pos;
                        String	temp;
                        
                        while ((n < tlen) && ((pos = "0123456789".indexOf (ch)) != -1)) {
                            newn = n;
                            tid *= 10;
                            tid += pos;
                            ++n;
                            if (n < tlen)
                                ch = targetExpr.charAt (n);
                            else
                                ch = '\0';
                        }
                        n = newn;
                        rset = dbase.simpleQuery ("SELECT target_sql " +
                                      "FROM dyn_target_tbl " +
                                      "WHERE (company_id = " + company_id + " OR company_id = 0) AND target_id = " + tid);
                        temp = dbase.getValidString (rset, 1, 3);
                        rset.close ();
                        if (temp != null)
                            buf.append (" (" + temp + ")");
                    }
                }
                if (buf.length () >= 3)
                    subselect = buf.toString ();
            }
            rset = dbase.execQuery ("SELECT mediatype, param FROM mailing_mt_tbl " +
                        "WHERE mailing_id = " + mailing_id);
            if (rset.next ()) {
                int	mediatype = rset.getInt (1);
                String	param = rset.getString (2);

                media = new Media (mediatype, 0, Media.STAT_ACTIVE, param);
            }
            rset.close ();
            if (media == null) {
                media = new Media (Media.TYPE_EMAIL, 0, Media.STAT_ACTIVE, null);
                    
                media.addParameter ("from", f_email);
                media.addParameter ("reply", r_to);
                media.addParameter ("subject", subject);
                media.addParameter ("charset", charset);
                media.addParameter ("mailformat", Integer.toString (masterMailtype));
                media.addParameter ("encoding", encoding);
                media.addParameter ("linefeed", Integer.toString (lineLength));
                media.addParameter ("onepixlog", "none");
            }

            Media	tmp;
            tmp = media;
            if (tmp.type == Media.TYPE_EMAIL) {
                    availableMedias = 1;
                    if (tmp.findParameterValues ("charset") == null)
                        tmp.setParameter ("charset", defaultCharset);
                    if (tmp.findParameterValues ("encoding") == null)
                        tmp.setParameter ("encoding", defaultEncoding);
                    from_email = new EMail (findMediadata (tmp, "from", f_email));
                    reply_to = new EMail (findMediadata (tmp, "reply", r_to));
                    subject = findMediadata (tmp, "subject", subject);
                    charset = findMediadata (tmp, "charset", charset);
                    masterMailtype = ifindMediadata (tmp, "mailformat", masterMailtype);
                    encoding = findMediadata (tmp, "encoding", encoding);
                    lineLength = ifindMediadata (tmp, "linefeed", lineLength);
                        
                    String	opl = findMediadata (tmp, "onepixlog", "none");
                    
                    if (opl.equals ("top"))
                        onepixlog = OPL_TOP;
                    else if (opl.equals ("bottom"))
                        onepixlog = OPL_BOTTOM;
                    else
                        onepixlog = OPL_NONE;
            }
            if ((encoding == null) || (encoding.length () == 0))
                encoding = defaultEncoding;
            if ((charset == null) || (charset.length () == 0))
                charset = defaultCharset;
            //
            // now count the total number of subscribers
            if (! isCampaignMailing ()) {
                if (subselect == null)
                    rset = dbase.simpleQuery ("SELECT count(distinct(customer_id)) FROM customer_" + company_id + "_binding_tbl " +
                                  "WHERE " +
                                  "mailinglist_id = " + mailinglist_id + " AND " +
                                  "user_status = " + BindingEntry.USER_STATUS_ACTIVE);
                else
                    rset = dbase.simpleQuery ("SELECT count(distinct(cust.customer_id)) FROM "+
                                  "customer_" + company_id + "_tbl cust, " +
                                  "customer_" + company_id + "_binding_tbl bind " +
                                  "WHERE " +
                                  "bind.customer_id = cust.customer_id AND " +
                                  "bind.mailinglist_id = " + mailinglist_id + " AND " +
                                  "bind.user_status = " + BindingEntry.USER_STATUS_ACTIVE + " AND " +
                                  "(" + subselect + ")");
                totalSubscribers = rset.getLong (1);
                rset.close ();
            } else
                totalSubscribers = 1;
            
            if ((subselect != null) && (! isWorldMailing ()) && (! isCampaignMailing ()) && (! isRuleMailing ())) {
                subselect = null;
            }
            
            //
            // get all possible URLs that should be replaced
            rset = dbase.execQuery ("SELECT url_id, full_url, measure_type FROM rdir_url_tbl " +
                        "WHERE company_id = " + company_id + " AND mailing_id = " + mailing_id);
            URLlist = new Vector ();
            while (rset.next ()) {
                long	id = rset.getLong (1);
                String	dest = rset.getString (2);
                long	usage = rset.getLong (3);
                
                if (usage != 0)
                    URLlist.addElement (new URL (id, dest, usage));
            }
            rset.close ();
            urlcount = URLlist.size ();
            
            //
            // get all possible title tags for this company
            rset = dbase.execQuery ("SELECT title_id, title, gender FROM title_gender_tbl " +
                        "WHERE title_id IN (SELECT title_id FROM title_tbl WHERE company_id = " + company_id + " OR company_id = 0 OR company_id IS null)");
            titles = new Hashtable ();
            while (rset.next ()) {
                Long	id = new Long (rset.getLong (1));
                String	title = rset.getString (2);
                int	gender = rset.getInt (3);
                Title	cur = null;

                if ((cur = (Title) titles.get (id)) == null) {
                    cur = new Title (id);
                    titles.put (id, cur);
                }
                cur.setTitle (gender, title);
            }
            rset.close ();
            //
            // and now try to determinate the layout of the
            // customer table
            rset = dbase.execQuery ("SELECT * FROM customer_" + company_id + "_tbl WHERE 1 = 0");
            
            ResultSetMetaData	meta = rset.getMetaData ();
            int			ccnt = meta.getColumnCount ();
            
            layout = new Vector ();
            for (int n = 0; n < ccnt; ++n) {
                String	cname = meta.getColumnName (n + 1);
                int	ctype = meta.getColumnType (n + 1);

                if (Column.typeStr (ctype) != null)
                    layout.addElement (new Column (cname, ctype));
            }
            rset.close ();
            lcount = layout.size ();
            lusecount = lcount;

            //
            // retrieve some information data
            mailtracking_table = "mailtrack_tbl";
            rset = dbase.simpleQuery ("SELECT shortname, xor_key, rdir_domain FROM company_tbl WHERE company_id = " + company_id);
            company_name = rset.getString (1);
            password = rset.getString (2);
            rdirDomain = rset.getString (3);
            rset.close ();
            if (rdirDomain != null) {
                if (profileURL == null)
                    profileURL = rdirDomain + "/p?";
                if (unsubscribeURL == null)
                    unsubscribeURL = rdirDomain + "/uq?";
                if (autoURL == null)
                    autoURL = rdirDomain + "/r?";
                if (onePixelURL == null)
                    onePixelURL = rdirDomain + "/g?";
            }
        } catch (SQLException e) {
            logging (Log.ERROR, "init", "SQLError in quering initial data: " + e);
            throw new ConfigException ("Data error/initial query: " + e);
        } catch (Exception e) {
            logging (Log.ERROR, "init", "Error in quering initial data: " + e);
            throw new ConfigException ("Database error/initial query: " + e);
        }
    }
    
    /**
     * Set the blocksize for generation doing some sanity checks
     * @param newBlockSize the new block size to use
     */
    private void setBlockSize (int newBlockSize) {
        blocksPerStep = 1;
        if (newBlockSize < MIN_BLOCK_SIZE)
            blockSize = MIN_BLOCK_SIZE;
        else if (newBlockSize > MAX_BLOCK_SIZE) {
            blocksPerStep = (newBlockSize + MAX_BLOCK_SIZE - 1) / MAX_BLOCK_SIZE;
            blockSize = (newBlockSize + 1) / blocksPerStep;
        } else
            blockSize = newBlockSize;
    }


    /**
     * Remove an existing old entry for mailing status and
     * write a new record
     */
    private void setupMailingStatus () throws ConfigException {
        setMailingStatus = false;
        checkDatabase ();
        try {
            // delete old mailing id entry first
            dbase.execUpdate ("DELETE FROM mailing_status_tbl " +
                      "WHERE mailing_id=" + mailing_id);
            // insert new status text for mailing id
            dbase.execUpdate ("INSERT INTO mailing_status_tbl (mailing_id, status_text) " +
                      "VALUES ( " + mailing_id + " , '" + startMessage + "' )");
            setMailingStatus = true;
        } catch (Exception e) {
            logging (Log.ERROR, "init", "Error in setup mailing status: " + e);
            throw new ConfigException ("Unable to setup mailing status: " + e);
        }
    }
    
    /**
     * Change the mailing status to a new status
     * @param msg the new status
     */
    private void changeMailingStatus (String msg) throws ConfigException {
        if (setMailingStatus) {
            checkDatabase ();
            try {
                dbase.execUpdate ("UPDATE mailing_status_tbl SET status_text = '" + msg + "' " +
                            "WHERE mailing_id=" + mailing_id);
            } catch (Exception e) {
                logging (Log.ERROR, "data", "Error in changing mailing status: " + e);
                throw new ConfigException ("Unable to change mailing status: " + e);
            }
        }
    }
    
    /**
     * Validate all set variables and make a sanity check
     * on the database to avoid double triggering of a
     * mailing
     */
    private void checkMailingData () throws ConfigException {
        int	cnt;
        String	msg;
        
        cnt = 0;
        msg = "";
        if (isWorldMailing ())
            try {
                ResultSet	rset;
                long		nid;
            
                checkDatabase ();
                rset = dbase.simpleQuery ("SELECT status_id FROM maildrop_status_tbl WHERE status_field = 'W' AND mailing_id = " + mailing_id + " ORDER BY status_id");
                nid = rset.getLong (1);
                if (nid != maildrop_status_id) {
                    ++cnt;
                    msg += "\tlowest maildrop_status_id is not mine (" + maildrop_status_id + ") but " + nid + "\n";
                }
            } catch (Exception e) {
                ++cnt;
                msg += "\tunable to requery my status_id: " + e.toString () + "\n";
            }
        if (maildrop_status_id <= 0) {
            ++cnt;
            msg += "\tmaildrop_status_id is less than 1 (" + maildrop_status_id + ")\n";
        }
        if (company_id <= 0) {
            ++cnt;
            msg += "\tcompany_id is less than 1 (" + company_id + ")\n";
        }
        if (mailinglist_id <= 0) {
            ++cnt;
            msg += "\tmailinglist_id is less than 1 (" + mailinglist_id + ")\n";
        }
        if (mailing_id <= 0) {
            ++cnt;
            msg += "\tmailing_id is less than 1 (" + mailing_id + ")\n";
        }
        if ((! isAdminMailing ()) &&
            (! isTestMailing ()) &&
            (! isCampaignMailing ()) &&
            (! isRuleMailing ()) &&
            (! isWorldMailing ())) {
            ++cnt;
            msg += "\tstatus_field must be one of A, T, E, R or W (" + status_field + ")\n";
        }

        //
        // on admin/test mailing, generate the most
        // user readable format according to outMode
        if ((! isCampaignMailing ()) &&
            (! isRuleMailing ()) &&
            (! isWorldMailing ()))
            switch (outMode) {
            case OUT_META:
                metaMode = OUT_META_XML;
                break;
            default:
                outMode = OUT_META;
                metaMode = OUT_META_XML;
                break;
            }

        long	now = System.currentTimeMillis () / 1000;
        if (sendtimestamp != null)
            sendSeconds = sendtimestamp.getTime () / 1000;
        else if ((senddate != null) && (sendtime != null))
            sendSeconds = (senddate.getTime () + sendtime.getTime ()) / 1000;
        else
            sendSeconds = now;
        if (sendSeconds < now)
            currentSendDate = new java.util.Date (now * 1000);
        else
            currentSendDate = new java.util.Date (sendSeconds * 1000);
        if (step < 0) {
            ++cnt;
            msg += "\tstep is less than 0 (" + step + ")\n";
        }
        if ((encoding == null) || (encoding.length () == 0)) {
            ++cnt;
            msg += "\tmissing or empty encoding\n";
        }
        if ((charset == null) || (charset.length () == 0)) {
            ++cnt;
            msg += "\tmissing or empty charset\n";
        }
        if ((profileURL == null) || (profileURL.length () == 0)) {
            ++cnt;
            msg += "\tmissing or empty profile_url\n";
        }
        if ((unsubscribeURL == null) || (unsubscribeURL.length () == 0)) {
            ++cnt;
            msg += "\tmissing or empty unsubscribe_url\n";
        }
        if ((autoURL == null) || (autoURL.length () == 0)) {
            ++cnt;
            msg += "\tmissing or empty auto_url\n";
        }
        if ((onePixelURL == null) || (onePixelURL.length () == 0)) {
//			++cnt;
            onePixelURL = "file://localhost/";
            msg += "\tmissing or empty onepixe_url\n";
        }
        if ((masterMailtype < 0) || (masterMailtype > 2)) {
            ++cnt;
            msg += "\tmaster_mailtype is out of range (0 .. 2)\n";
        }
        if (lineLength < 0) {
            ++cnt;
            msg += "\tlinelength is less than zero\n";
        }
        if (totalSubscribers <= 0) {
//			++cnt;
            msg += "\ttotal number of subscribers is less than 1 (" + totalSubscribers + ")\n";
        }
        if (cnt > 0) {
            logging (Log.ERROR, "init", "Error configuration report:\n" + msg);
            throw new ConfigException (msg);
        }
        if (msg.length () > 0)
            logging (Log.WARNING, "init", "Configuration report:\n" + msg);
    }
    
    /** Setup logging interface
     * @param program to create the logging path
     * @param setprinter if we should also log to stdout
     */
    private void setupLogging (String program, boolean setprinter) {
        log = new Log (program, logLevel);
        if (setprinter)
            log.setPrinter (System.out);
    }

    /**
     * Constructor for the class
     * @param program the name of the program (for logging setup)
     * @param status_id the status_id to read the mailing information from
     * @param option output option
     * @param conn optional opened database connection
     */
    public Data (String program, String status_id,
             String option, Connection conn) throws ConfigException {
        super (INI_PROP, INI_FILE);
        validation (INI_VARS);
        setupLogging (program, conn == null);
        
        int	n;

        logging (Log.DEBUG, "init", "Data read from " + filename + " for " + status_id + ":" + (option == null ? "default" : option));
        outMode = DEFAULT_OUT_MODE;
        if ((n = option.indexOf (':')) != -1) {
            String	omode;
            
            omode = option.substring (0, n);
            option = option.substring (n + 1);
            outMode = -1;
            for (n = 0; n < OUT_MODES.length; ++n)
                if (omode.equals (OUT_MODES[n])) {
                    outMode = n;
                    break;
                }
            if (outMode == -1) {
                logging (Log.ERROR, "init", "Unknown output mode " + omode);
                throw new ConfigException ("Unknown output mode " + omode + " specified");
            }
            logging (Log.DEBUG, "init", "Using output mode " + outModeDescription ());
        }
        metaMode = -1;
        switch (outMode) {
        case OUT_META:
            for (n = 0; n < META_MODES.length; ++n)
                if (option.equals (META_MODES[n])) {
                    metaMode = n;
                    break;
                }
            if (metaMode == -1) {
                logging (Log.ERROR, "init", "Unknown meta output type " + option);
                throw new ConfigException ("Unknwon metaoutput option type " + option + " specified");
            }
            logging (Log.DEBUG, "init", "Using meta output mode " + metaModeDescription ());
            break;
        default:
            logging (Log.ERROR, "init", "Unknown output mode " + outMode);
            throw new ConfigException ("Unkown output Mode " + outMode + " found");
        }
        setupDatabase (conn);
        logging (Log.DEBUG, "init", "Initial database connection established");
        try {
            queryMailingInformations (status_id);
            setupMailingStatus ();
        } catch (Exception e) {
            throw new ConfigException ("Database failure: " + e);
        }
        logging (Log.DEBUG, "init", "Initial data read from database");
        checkMailingData ();
        lid = "(" + company_id + "/" +
                mailinglist_id + "/" +
                mailing_id + "/" +
                maildrop_status_id + ")";
        if (islog (Log.DEBUG)) {
            logging (Log.DEBUG, "init", "Initial data valid");
            logging (Log.DEBUG, "init", "All set variables:");
            logging (Log.DEBUG, "init", "\tlogLevel = " + log.levelDescription () + " (" + log.level () + ")");
            logging (Log.DEBUG, "init", "\tmailDir = " + mailDir);
            logging (Log.DEBUG, "init", "\tdefaultEncoding = " + defaultEncoding);
            logging (Log.DEBUG, "init", "\tdefaultCharset = " + defaultCharset);
            logging (Log.DEBUG, "init", "\tdbLogin = " + dbLogin);
            logging (Log.DEBUG, "init", "\tdbPassword = ******");
            logging (Log.DEBUG, "init", "\tsqlConnect = " + sqlConnect);
            logging (Log.DEBUG, "init", "\tblockSize = " + blockSize);
            logging (Log.DEBUG, "init", "\tmetaDir = " + metaDir);
            logging (Log.DEBUG, "init", "\txmlBack = " + xmlBack);
            logging (Log.DEBUG, "init", "\txmlValidate = " + xmlValidate);
            logging (Log.DEBUG, "init", "\tsampleEmails = " + sampleEmails);
            logging (Log.DEBUG, "init", "\tmailLogNumber = " + mailLogNumber);
            logging (Log.DEBUG, "init", "\tstartMessage = " + startMessage);
            logging (Log.DEBUG, "init", "\tendMessage = " + endMessage);
            logging (Log.DEBUG, "init", "\tdbase = " + dbase);
            logging (Log.DEBUG, "init", "\tmaildrop_status_id = " + maildrop_status_id);
            logging (Log.DEBUG, "init", "\tcompany_id = " + company_id);
            if (company_name != null)
                logging (Log.DEBUG, "init", "\tcompany_name = " + company_name);
            if (mailtracking_table != null)
                logging (Log.DEBUG, "init", "\tmailtracking_table = " + mailtracking_table);
            logging (Log.DEBUG, "init", "\tmailinglist_id = " + mailinglist_id);
            logging (Log.DEBUG, "init", "\tmailing_id = " + mailing_id);
            logging (Log.DEBUG, "init", "\tstatus_field = " + status_field);
            logging (Log.DEBUG, "init", "\tsenddate = " + senddate);
            logging (Log.DEBUG, "init", "\tsendtime = " + sendtime);
            logging (Log.DEBUG, "init", "\tsendtimestamp = " + sendtimestamp);
            logging (Log.DEBUG, "init", "\tsendSeconds = " + sendSeconds);
            logging (Log.DEBUG, "init", "\tstep = " + step);
            logging (Log.DEBUG, "init", "\tblocksPerStep = " + blocksPerStep);
            logging (Log.DEBUG, "init", "\tsubselect = " + (subselect == null ? "*not set*" : subselect));
            logging (Log.DEBUG, "init", "\tmailing_name = " + (mailing_name == null ? "*not set*" : mailing_name));
            logging (Log.DEBUG, "init", "\tsubject = " + (subject == null ? "*not set*" : subject));
            logging (Log.DEBUG, "init", "\tfrom_email = " + (from_email == null ? "*not set*" : from_email.toString ()));
            logging (Log.DEBUG, "init", "\treply_to = " + (reply_to == null ? "*not set*" : reply_to.toString ()));
            logging (Log.DEBUG, "init", "\tencoding = " + encoding);
            logging (Log.DEBUG, "init", "\tcharset = " + charset);
            logging (Log.DEBUG, "init", "\tdomain = " + domain);
            logging (Log.DEBUG, "init", "\tboundary = " + boundary);
            if (eol.equals ("\r\n"))
                logging (Log.DEBUG, "init", "\teol = CRLF");
            else if (eol.equals ("\n"))
                logging (Log.DEBUG, "init", "\teol = LF");
            else
                logging (Log.DEBUG, "init", "\teol = unknown (" + eol.length () + ")");
            logging (Log.DEBUG, "init", "\tmailer = " + mailer);
            logging (Log.DEBUG, "init", "\tprofileURL = " + profileURL);
            logging (Log.DEBUG, "init", "\tunsubscribeURL = " + unsubscribeURL);
            logging (Log.DEBUG, "init", "\tautoURL = " + autoURL);
            logging (Log.DEBUG, "init", "\tonePixelURL = " + onePixelURL);
            logging (Log.DEBUG, "init", "\tmasterMailtype = " + masterMailtype);
            logging (Log.DEBUG, "init", "\tlineLength = " + lineLength);
            logging (Log.DEBUG, "init", "\tonepixlog = " + onepixlog);
            logging (Log.DEBUG, "init", "\tpassword = " + password);
            logging (Log.DEBUG, "init", "\trdirDomain = " + rdirDomain);
            logging (Log.DEBUG, "init", "\ttotalSubscribers = " + totalSubscribers);
        }
    }

    /**
     * Constructor for non mailing based instances
     * @param program the program name for logging
     */
    public Data (String program) throws ConfigException {
        super (INI_PROP, INI_FILE);
        validation (INI_VARS);
        setupLogging (program, true);
        logging (Log.DEBUG, "init", "Starting up");
        setupDatabase (null);
    }
    
    /**
     * Suspend call between setup and main execution
     * @param conn optional database connection
     */
    public void suspend (Connection conn) throws Exception {
        if ((conn != null) && (dbase != null))
            dbase.done ();
    }
    
    /**
     * Resume before main execution
     * @param conn optional database connection
     */
    public void resume (Connection conn) throws Exception {
        if ((conn != null) && (dbase != null))
            dbase.setConnection (conn);
    }
    
    /**
     * Cleanup all open resources and write mailing status before
     */
    public void done () throws ConfigException {
        int	cnt;
        String	msg;
        
        cnt = 0;
        msg = "";
        if (setMailingStatus && writeEndMessage) {
            logging (Log.DEBUG, "deinit", "Writing final report");
            try {
                String	emsg = endMessage;
                
                if ((extraEndMessage != null) && (extraEndMessage.length () > 0))
                    emsg += " " + extraEndMessage;
                changeMailingStatus (emsg);
            } catch (Exception e) {
                ++cnt;
                msg += "\tFailed in final report: " + e + "\n";
            }
        }
        if (dbase != null) {
            logging (Log.DEBUG, "deinit", "Shuting down database connection");
            try {
                closeDatabase ();
            } catch (ConfigException e) {
                ++cnt;
                msg += "\t" + e + "\n";
            }
        }
        if (toRemove != null) {
            int	fcnt = toRemove.size ();
            
            if (fcnt > 0) {
                logging (Log.DEBUG, "deinit", "Remove " + fcnt + " file" + Log.exts (fcnt) + " if existing");
                while (fcnt-- > 0) {
                    String	fname = (String) toRemove.remove (0);
                    File	file = new File (fname);
                
                    if (file.exists ())
                        if (! file.delete ())
                            msg += "\trm " + fname + "\n";
                    file = null;
                }
            }
            toRemove = null;
        }
        if (cnt > 0)
            throw new ConfigException ("Unable to cleanup:\n" + msg);
        logging (Log.DEBUG, "deinit", "Cleanup done: " + msg);
    }
    
    /**
     * Sanity check for mismatch company_id and perhaps deleted
     * mailing
     */
    public void sanityCheck () throws ConfigException {
        ResultSet	rset;

        try {
            long	cid, del;
            
            rset = dbase.simpleQuery ("SELECT company_id, deleted FROM mailing_tbl WHERE mailing_id = " + mailing_id);
            cid = rset.getLong (1);
            del = rset.getLong (2);
            rset.close ();
            if (cid != company_id)
                throw new ConfigException ("Original companyID " + company_id + " for mailing " + mailing_id + " does not match current company_id " + cid);
            if (del != 0) {
                dbase.execUpdate ("UPDATE maildrop_status_tbl SET genchange = " + dbase.sysdate + ", genstatus = 4 " +
                          "WHERE status_id = " + maildrop_status_id);
                throw new ConfigException ("Mailing " + mailing_id + "marked as deleted");
            }
        } catch (Exception e) {
            logging (Log.ERROR, "sanity", "Error in quering mailing_tbl: " + e);
            throw new ConfigException ("Unable to find entry in mailing_tbl for " + mailing_id + ": " + e);
        }
    }
    
    /**
     * Change generation state for the current mailing
     */
    public void updateGenerationState () {
        if (isAdminMailing () || isTestMailing () || isWorldMailing () || isRuleMailing ()) {
            try {
                int	rowcount;
                int	newstatus;

                if (isRuleMailing ())
                    newstatus = 1;
                else
                    newstatus = 3;
                rowcount = dbase.execUpdate ("UPDATE maildrop_status_tbl SET genchange = " + dbase.sysdate + ", genstatus = " + newstatus + " " +
                                 "WHERE status_id = " + maildrop_status_id + " AND genstatus = 2");
                if (rowcount != 1)
                    logging (Log.ERROR, "genstate", "Updated " + rowcount + " rows, not excatly one");
            } catch (Exception e) {
                logging (Log.ERROR, "genstate", "Unable to update generation state: " + e.toString ());
            }
        }
    }
    
    /**
     * Convert a given object to an integer
     * @param o the input object
     * @param what for logging purpose
     * @return the converted value
     */
    private int obj2int (Object o, String what) throws ConfigException {
        int	rc;
        
        if (o.getClass () == new Integer (0).getClass ())
            rc = ((Integer) o).intValue ();
        else if (o.getClass () == new Long (0L).getClass ())
            rc = ((Long) o).intValue ();
        else if (o.getClass () == new String ().getClass ())
            rc = Integer.parseInt ((String) o);
        else
            throw new ConfigException ("Unknown data type for " + what);
        return rc;
    }
    
    /**
     * Convert a given object to a long
     * @param o the input object
     * @param what for logging purpose
     * @return the converted value
     */
    private long obj2long (Object o, String what) throws ConfigException {
        long	rc;

        if (o.getClass () == new Integer (0).getClass ())
            rc = ((Integer) o).longValue ();
        else if (o.getClass () == new Long (0L).getClass ())
            rc = ((Long) o).longValue ();
        else if (o.getClass () == new String ().getClass ())
            rc = Long.parseLong ((String) o);
        else
            throw new ConfigException ("Unknown data type for " + what);
        return rc;
    }
    
    /**
     * Convert a given object to a date
     * @param o the input object
     * @param what for logging purpose
     * @return the converted value
     */
    private java.util.Date obj2date (Object o, String what) throws ConfigException {
        java.util.Date	rc;
            
        if (o.getClass () == new java.util.Date ().getClass ())
            rc = (java.util.Date) o;
        else
            throw new ConfigException ("Unknown data type for " + what);
        return rc;
    }

    /**
     * Parse options passed during runtime 
     * @param opts the options to use
     * @param state if 1, the before initialization pass, 2 on execution pass
     */
    public void options (Hashtable opts, int state) throws ConfigException {
        Object	tmp;

        if (opts == null) {
            return;
        }
        if (state == 1) {
            tmp = opts.get ("custom-tags");
            if (tmp != null) {
                if (customTags == null)
                    customTags = new Vector ();
                for (Enumeration e = ((Hashtable) tmp).keys (); e.hasMoreElements (); ) {
                    String	s = (String) e.nextElement ();
                    
                    if (s != null)
                        customTags.add (s);
                }
            }
        } else if (state == 2) {
            tmp = opts.get ("customer-id");
            if (tmp != null)
                campaignCustomerID = obj2long (tmp, "customer-id");
            tmp = opts.get ("transaction-id");
            if (tmp != null)
                campaignTransactionID = obj2long (tmp, "transaction-id");
            tmp = opts.get ("user-status");
            if (tmp != null)
                campaignUserStatus = obj2int (tmp, "user-status");
            tmp = opts.get ("send-date");
            if (tmp != null) {
                currentSendDate = obj2date (tmp, "send-date");
                sendSeconds = currentSendDate.getTime () / 1000;

                long	now = System.currentTimeMillis () / 1000;
                if (sendSeconds < now)
                    sendSeconds = now;
            }

            tmp = opts.get ("step");
            if (tmp != null)
                step = obj2long (tmp, "step");
            tmp = opts.get ("block-size");
            if (tmp != null)
                setBlockSize (obj2int (tmp, "block-size"));

            campaignSubselect = (TargetRepresentation) opts.get ("select");
            
            customMap = (Hashtable) opts.get ("custom-tags");
            overwriteMap = (Hashtable) opts.get ("overwrite");
            virtualMap = (Hashtable) opts.get ("virtual");
            overwriteMapMulti = (Hashtable) opts.get ("overwrite-multi");
            virtualMapMulti = (Hashtable) opts.get ("virtual-multi");
        }
    }
    
    /**
     * Find entry in map for overwrite/virtual records
     * @param cid the customer id
     * @param multi optional available multi hash table
     * @param simple optional simple hash table
     * @param colname the name of the column
     * @return the found string or null
     */
    private String findInMap (Long cid, Hashtable multi, Hashtable simple, String colname) {
        Hashtable	map;
        
        if ((multi != null) && multi.containsKey (cid))
            map = (Hashtable) multi.get (cid);
        else
            map = simple;
        if ((map != null) && map.containsKey (colname))
            return (String) map.get (colname);
        return null;
    }

    /**
     * Find an overwrite column
     * @param cid the customer id
     * @param colname the name of the column
     * @return the found string or null
     */
    public String overwriteData (Long cid, String colname) {
        return findInMap (cid, overwriteMapMulti, overwriteMap, colname);
    }
    
    /**
     * Find a virtual column
     * @param cid the customer id
     * @param colname the name of the column
     * @return the found string or null
     */
    public String virtualData (Long cid, String colname) {
        return findInMap (cid, virtualMapMulti, virtualMap, colname);
    }

    /**
     * If we have another subselection during runtime
     * return it from here
     * @return the extra subselect or null
     */
    public String getCampaignSubselect () {
        String	rc = null;
        String	sql;
        
        if (campaignSubselect != null) {
            sql = campaignSubselect.generateSQL ();
            if ((sql != null) && (sql.length () > 0))
                rc = sql;
        }
        return rc;
    }

    /**
     * Write a mailing status to the database, open the database
     * temp., if not yet opened
     * @param msg the new mailing status
     */
    public void report (String msg) throws ConfigException {
        logging (Log.DEBUG, "data", "Write report: " + msg);
        try {
            boolean	tempOpened;
            
            if (dbase == null) {
                setupDatabase (null);
                setMailingStatus = true;
                tempOpened = true;
            } else
                tempOpened = false;
            if (dbase != null) {
                changeMailingStatus (msg);
                writeEndMessage = false;
                if (tempOpened == true)
                    closeDatabase ();
            }
        } catch (Exception e) {
            throw new ConfigException ("Unable to report to MailingStatus: " + e);
        }
    }
    
    /**
     * Write an error as mailing status
     * @param msg the error message
     */
    public void error (String msg) throws ConfigException {
        report ("Error: " + msg);
    }

    /**
     * Check for existance of a database table
     * @param table the table name
     * @return true, if the table exists
     */
    public boolean tableExists (String table) {
        boolean	rc = false;
        
        if (tables != null)
            rc = tables.contains (table.toLowerCase ());
        return rc;
    }

    /**
     * Mark a filename to be removed during cleanup phase
     * @param fname the filename
     */
    public void markToRemove (String fname) {
        if (toRemove == null)
            toRemove = new Vector ();
        if (! toRemove.contains (fname))
            toRemove.addElement (fname);
    }
    
    /**
     * Mark a file to be removed during cleanup
     * @param file a File instance for the file to be removed
     */
    public void markToRemove (File file) {
        markToRemove (file.getAbsolutePath ());
    }

    /**
     * Unmark a filename to be removed, if we already removed
     * it by hand
     * @param fname the filename
     */
    public void unmarkToRemove (String fname) {
        if ((toRemove != null) && toRemove.contains (fname))
            toRemove.remove (fname);
    }
    
    /**
     * Unmark a file to be removed
     * @param file a File instance
     */
    public void unmarkToRemove (File file) {
        unmarkToRemove (file.getAbsolutePath ());
    }
    
    /**
     * Overwrite the super class method, not used herein
     * @param vindex index into the configuration variable array
     * @param variable name of the variable
     * @param parm subidentification for the variable
     * @return none, always raise exception
     */
    protected boolean selected (int vindex, String variable, String parm) throws ConfigException {
        throw new ConfigException ("Subidentifications in variables not supported");
    }

    /**
     * Validate configuration variables and stores values
     * @param vindex index into the configuration variable array
     * @param variable name of the variable
     * @param value the value for this variable
     */
    protected void validate (int vindex, String variable, String value) throws ConfigException {
        switch (vindex) {
        case 0:
            try {
                logLevel = Log.matchLevel (value);
            } catch (NumberFormatException e) {
                throw new ConfigException ("Loglevel must be a known string or a numerical value");
            }
            break;
        case 1:
            mailDir = value;
            break;
        case 2:
            break;
        case 3:
            break;
        case 4:
            break;
        case 5:
            break;
        case 6:
            break;
        case 7:
            defaultEncoding = value;
            break;
        case 8:
            defaultCharset = value;
            break;
        case 9:
            dbLogin = value;
            break;
        case 10:
            dbPassword = value;
            break;
        case 11:
            sqlConnect = value;
            break;
        case 12:
            if ((blockSize = mkInt (value)) < 1)
                throw new ConfigException ("Value must be at least 1");
            break;
        case 13:
            metaDir = value;
            break;
        case 14:
            xmlBack = value;
            break;
        case 15:
            xmlValidate = mkBool (value);
            break;
        case 16:
            sampleEmails = value;
            if ((sampleEmails.length () == 0) || (sampleEmails.equals ("-")))
                sampleEmails = null;
            break;
        case 17:
            domain = value;
            break;
        case 18:
            boundary = value;
            break;
        case 19:
            if (value.equalsIgnoreCase ("CRLF"))
                eol = "\r\n";
            else if (value.equalsIgnoreCase ("LF"))
                eol = "\n";
            else
                throw new ConfigException ("EOL must be either CRLF or LF");
            break;
        case 20:
            mailer = value;
            break;
        case 21:
            if ((mailLogNumber = mkInt (value)) < 0)
                throw new ConfigException ("Value must be greater or equal 0");
            break;
        case 22:
            startMessage = value;
            break;
        case 23:
            endMessage = value;
            break;
        default:
            throw new ConfigException ("Invalid variable index " + vindex);
        }
    }
    
    /**
     * Report error, if a required variable is missing
     * @param vindex index into the configuration variable array
     * @param variable name of the variable
     */
    protected void missing (int vindex, String variable) throws ConfigException {
        // loglevel, xmlback, sample email, domain, mailer,
        // start and end message has valid default values
        switch (vindex) {
                // ignore:
        case 0:		// loglevel
        case 2:		// reserved1
        case 3:		// reserved2
        case 4:		// reserved3
        case 5:		// reserved4
        case 6:		// reserved5
        case 16:	// sample email
        case 17:	// domain
        case 18:	// boundary
        case 19:	// eol
        case 20:	// mailer
        case 22:	// start message
        case 23:	// end message
            break;
        default:
            throw new ConfigException ();
        }
    }

    /**
     * Report error, if we hit an unknown variable. We just log this
     * case and keep going
     * @param variable the name of the unknown variable
     */
    protected void unknown (String variable) throws ConfigException {
        logging (Log.WARNING, "data", "Unknown configuration parameter " + variable);
    }

    /**
     * Check if we have to write logging for a given loglevel
     * @param loglvl the loglevel to check against
     * @return true if we should log
     */
    public boolean islog (int loglvl) {
        return log.islog (loglvl);
    }

    /**
     * Write entry to logfile
     * @param loglvl the level to report
     * @param mid the ID of the message
     * @param msg the message itself
     */
    public void logging (int loglvl, String mid, String msg) {
        if (lid != null)
            if (mid != null)
                mid = mid + "/" + lid;
            else
                mid = lid;
        if (log != null)
            log.out (loglvl, mid, msg);
        else
            System.err.println ((mid == null ? "" : mid + " ") + msg);
    }


    /**
     * Create a path to write test-/admin mails to
     * @return the path to use
     */
    public String mailDir () {
        return mailDir;
    }

    /** returns the database login
     * @return login string
     */
    public String dbLogin () {
        return dbLogin;
    }
    
    /** returns the database password
     * @return password string
     */
    public String dbPassword () {
        return dbPassword;
    }

    /** returns the connection string for the database
     * @return connection string
     */
    public String sqlConnect () {
        return sqlConnect;
    }

    /** returns the block size to be used
     * @return block size
     */
    public int blockSize () {
        return blockSize;
    }

    /** returns the directory to write meta files to
     * @return path to meta
     */
    public String metaDir () {
        return metaDir;
    }
    
    /** returns the path to xmlback program
     * @return path to xmlback
     */
    public String xmlBack () {
        return xmlBack;
    }

    /** returns wether we should validate generated XML files
     * @return true if validation should take place
     */
    public boolean xmlValidate () {
        return xmlValidate;
    }

    /** returns the optional used sample receivers
     * @return receiver list
     */
    public String sampleEmails () {
        return sampleEmails;
    }

    /** returns the number of generate mails to write log entries for
     * @return number of mails
     */
    public int mailLogNumber () {
        return mailLogNumber;
    }

    /** returns the X-Mailer: header content
     * @return mailer name
     */
    public String makeMailer () {
        if ((mailer != null) && (company_name != null))
            return StringOps.replace (mailer, "[agnMANDANT]", company_name);
        return mailer;
    }

    /** returns textual representation of output mode
     * @return output mode as string
     */
    public String outModeDescription () {
        return ((outMode >= 0) && (outMode < OUT_MODES.length)) ? OUT_MODES[outMode] : null;
    }
    
    /** returns textual representation of meta file format
     * @return file format as string
     */
    public String metaModeDescription () {
        return ((metaMode >= 0) && (metaMode < META_MODES.length)) ? META_MODES[metaMode] : null;
    }
    
    /** if this is a admin mail
     * @return true, if admin mail
     */
    public boolean isAdminMailing () {
        return status_field.equals ("A");
    }
    
    /** if this is a test mail
     * @return true, if test mail
     */
    public boolean isTestMailing () {
        return status_field.equals ("T");
    }
    
    /** if this is a campaign mail
     * @return true, if campaign mail
     */
    public boolean isCampaignMailing () {
        return status_field.equals ("E");
    }

    public boolean
    isRuleMailing ()
    {
        return status_field.equals ("R");
    }

    /** if this is a world mail
     * @return true, if world mail
     */
    public boolean isWorldMailing () {
        return status_field.equals ("W");
    }
    
    /**
     * Add a message to the mailing status end message
     * @param msg the message to add
     */
    public void addToEndMessage (String msg) {
        if ((msg != null) && (msg.length () > 0))
            if (extraEndMessage != null)
                extraEndMessage += " " + msg;
            else
                extraEndMessage = msg;
    }

    /**
     * Set standard columns, if they are not already found in database
     * @param use already used column names
     */
    public void setUsedFieldsInLayout (HashSet use) {
        int	sanity = 0;
        HashSet	predef = new HashSet ();

        predef.add ("customerid");
        predef.add ("email");
        if (titleUsage > 0) {
            predef.add ("gender");
            predef.add ("lastname");
            if (titleUsage > 1) {
                predef.add ("firstname");
            }
        }
        for (int n = 0; n < lcount; ++n) {
            Column	c = (Column) layout.elementAt (n);
            String	name = c.name.toLowerCase ();
            
            if (use.contains (name) || predef.contains (name)) {
                if (! c.inuse) {
                    c.inuse = true;
                    ++lusecount;
                }
                ++sanity;
            } else {
                if (c.inuse) {
                    c.inuse = false;
                    --lusecount;
                }
            }
        }
        if (sanity != lusecount)
            logging (Log.ERROR, "layout", "Sanity check failed in setUsedFieldsInLayout");
    }

    /** return the name of the column at a given position
     * @param col the position in the column layout
     * @return the column name
     */
    public String columnName (int col) {
        return ((Column) layout.elementAt (col)).name;
    }
    
    /** return the type of the column at a given position
     * @param col the position in the column layout
     * @return the column type
     */
    public int columnType (int col) {
        return ((Column) layout.elementAt (col)).type;
    }
    
    /** return the type as string of the column at a given position
     * @param col the position in the column layout
     * @return the column type as string
     */
    public String columnTypeStr (int col) {
        return ((Column) layout.elementAt (col)).typeStr ();
    }

    /** Set a column from a result set
     * @param col the position in the column layout
     * @param rset the result set
     * @param index position in the result set
     */
    public void columnSet (int col, ResultSet rset, int index) {
        ((Column) layout.elementAt (col)).set (rset, index);
    }
    
    /** Get a value from a column
     * @param col the position in the column layout
     * @return the contents of that column
     */
    public String columnGetStr (int col) {
        return ((Column) layout.elementAt (col)).get ();
    }
    
    /** Check wether a columns value is NULL
     * @param col the position in the column layout
     * @return true of column value is NULL
     */
    public boolean columnIsNull (int col) {
        return ((Column) layout.elementAt (col)).isNull ();
    }

    /** Check wether a column is in use
     * @param col the position in the column layout
     * @return true if column is in use
     */
    public boolean columnUse (int col) {
        return ((Column) layout.elementAt (col)).inUse ();
    }
    
    /** get the default output option
     * @return the option string
     */
    public static String defaultOption () {
        String	opt;
        
        opt = OUT_MODES[DEFAULT_OUT_MODE] + ":";
        switch (DEFAULT_OUT_MODE) {
        case OUT_META:
            opt += META_MODES[DEFAULT_META_MODE];
            break;
        default:
            opt += "???";
            break;
        }
        return opt;
    }
    
    /** create a RFC compatible Date: line
     * @param ts the input time
     * @return the RFC representation
     */
    public String RFCDate (java.util.Date ts) {
        SimpleDateFormat	fmt = new SimpleDateFormat ("EEE, d MMM yyyy HH:mm:ss z",
                                    new Locale ("en", "DE"));
        fmt.setTimeZone (TimeZone.getTimeZone ("GMT"));
        if (ts == null)
            ts = new java.util.Date ();
        return fmt.format (ts);
    }
}