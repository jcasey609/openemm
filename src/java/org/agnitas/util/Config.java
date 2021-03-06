/*********************************************************************************
 * The contents of this file are subject to the Common Public Attribution
 * License Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.openemm.org/cpal1.html. The License is based on the Mozilla
 * Public License Version 1.1 but Sections 14 and 15 have been added to cover
 * use of software over a computer network and provide for limited attribution
 * for the Original Developer. In addition, Exhibit A has been modified to be
 * consistent with Exhibit B.
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * 
 * The Original Code is OpenEMM.
 * The Original Developer is the Initial Developer.
 * The Initial Developer of the Original Code is AGNITAS AG. All portions of
 * the code written by AGNITAS AG are Copyright (c) 2007 AGNITAS AG. All Rights
 * Reserved.
 * 
 * Contributor(s): AGNITAS AG. 
 ********************************************************************************/
package org.agnitas.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Enumeration;

/**
 * general class to read configuration files
 */
public class Config {
    /**
     * the name of the configuration file
     */
    private String      filename;
    
    /**
     * Contains all values for configuration
     */
    private Properties  config;

    /**
     * Constructor for the class
     *
     * @param property the property to look up the filename
     * @param defaultFilename the default, if no filename is found
     */
    public Config () {
        filename = null;
        config = new Properties ();
    }
    
    public String getSource () {
        return filename == null ? "rsc" : filename;
    }
    
    /**
     * Check for existance of file
     *
     * @param fname filename to check
     * @return true if file exists, false otherwise
     */
    private boolean fileExists (String fname) {
        boolean exists = false;

        try {
            File    f = new File (fname);

            exists = f.exists ();
        } catch (Exception e) {
            ;
        }
        return exists;
    }

    /**
     * Check if a path is a directory
     *
     * @param path the path name
     * @return true if it is a directory, false otherwise
     */
    private boolean isDirectory (String path) {
        boolean isdir = false;

        try {
            File    f = new File (path);

            isdir = (f.exists () && f.isDirectory ());
        } catch (Exception e) {
            ;
        }
        return isdir;
    }

    /**
     * Scans start directory for a file
     * @param base start directory
     * @param sep path separater
     * @param fname filename
     * @return full path, if file is found
     */
    private String scanForConfig (String base, String sep, String fname) {
        String  rc = null;

        try {
            File        f = new File (base.equals ("") ? sep : base);
            String[]    flist = f.list ();

            if (flist != null) {
                for (int n = 0; (rc == null) && (n < flist.length); ++n)
                    if (fname.equals (flist[n])) {
                        try {
                            String  test = base + sep + flist[n];
                            File    temp = new File (test);
                            if (temp.exists () && temp.isFile ()) {
                                rc = test;
                            }
                        } catch (Exception e) {
                            ;
                        }
                    }
                if (rc == null) {
                    for (int n = 0; (rc == null) && (n < flist.length); ++n) {
                        String  down = base + sep + flist[n];

                        if (isDirectory (down)) {
                            rc = scanForConfig (down, sep, fname);
                        }
                    }
                }
            }
        } catch (Exception e) {
            ;
        }
        return rc;
    }

    /**
     * Loads a configuration file
     * 
     * @param nFilename the basename of the file
     *   or
     * @param rsc the resource bundle
     * @param prefix the prefix for the keys
     * @returns true on success, false otherwise
     */
    public boolean loadConfig (String nFilename) {
        if (! fileExists (nFilename)) {
            String      classPath = System.getProperty ("java.class.path");
            String      filesep = System.getProperty ("file.separator");

            if (filesep == null) {
                filesep = java.io.File.separator;
                if (filesep == null) {
                    filesep = "/";
                }
            }
            int     n = nFilename.lastIndexOf (filesep);
            
            if (n != -1) {
                nFilename = nFilename.substring (n + 1);
            }

            filename = null;
            if (classPath != null) {
                String      pathsep = System.getProperty ("path.separator");

                if (pathsep == null) {
                    pathsep = java.io.File.pathSeparator;
                    if (pathsep == null) {
                        pathsep = ":";
                    }
                }

                String[]    parts = classPath.split (pathsep);
                
                for (n = 0; n < parts.length; ++n) {
                    if (isDirectory (parts[n])) {
                        String  fname = parts[n] + "/" + nFilename;

                        if (fileExists (fname)) {
                            filename = fname;
                            break;
                        }
                    }
                }
            }
            if (filename == null) {
                String  home = System.getProperty ("user.home");

                if (home != null) {
                    filename = scanForConfig (home, filesep, nFilename);
                }
            }
/*
            if (filename == null) {
                filename = scanForConfig ("", filesep, nFilename);
            }
 */
        } else {
            filename = nFilename;
        }
        boolean rc = false;
        
        if (filename != null) {
            try {
                config.load (new FileInputStream (filename));
                rc = true;
            } catch (IOException e) {
                ;
            }
        }
        return rc;
    }
    public boolean loadConfig (ResourceBundle rsc, String prefix) {
        boolean rc;
        
        rc = false;
        try {
            int plen = prefix == null ? 0 : prefix.length ();
            
            for (Enumeration e = rsc.getKeys (); e.hasMoreElements (); ) {
                String  key = (String) e.nextElement ();
                
                try {
                    String  value = rsc.getString (key);
                    
                    if ((prefix == null) || key.startsWith (prefix + ".")) {
                        if (value.startsWith ("::")) {
                            value = rsc.getString (value.substring (2));
                        }
                        if (prefix != null) {
                            key = key.substring (plen + 1);
                        }
                        config.setProperty (key.toUpperCase (), value);
                        rc = true;
                    }
                } catch (Exception ex) {
                    ;
                }
            }
        } catch (Exception ex) {
            ;
        }
        return rc;
    }
    
    /**
     * Search for a value
     * 
     * @param key the key to search for
     * (@param dftl default value)
     * @return data if, null otherwise
     */
    public String cget (String key) {
        return config.getProperty (key);
    }
    public String cget (String key, String dflt) {
        String  temp = cget (key);
        
        return temp == null ? dflt : temp;
    }
    public int cget (String key, int dflt) {
        String  temp = cget (key);
        
        return temp == null ? dflt : Integer.parseInt (temp);
    }
    public long cget (String key, long dflt) {
        String  temp = cget (key);
        
        return temp == null ? dflt : Long.parseLong (temp);
    }
    private boolean convertToBool (String str) {
        boolean val = false;

        if (str != null) {
            try {
                char    ch = str.charAt (0);

                switch (ch) {
                case 't':
                case 'T':
                case 'y':
                case 'Y':
                case '1':
                case '+':
                    val = true;
                    break;
                }
            } catch (IndexOutOfBoundsException e) {
                ;
            }
        }
        return val;
    }
    public boolean cget (String key, boolean dflt) {
        String  temp = cget (key);
        
        return temp == null ? dflt : convertToBool (temp);
    }
}
