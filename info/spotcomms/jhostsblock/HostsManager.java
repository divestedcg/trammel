package info.spotcomms.jhostsblock;

import javax.swing.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created using IntelliJ IDEA
 * User: Tad
 * Date: 6/9/15
 * Time; 7:56 PM
 */
public class HostsManager {

    private Utils utils = new Utils();
    private File dirConfigs = utils.getConfigDir();
    private File dirCache = new File(dirConfigs, "cache");
    private File fleHeader = new File(dirConfigs, "hostsheader.conf");
    private ArrayList<String> arrHeader;
    private File fleWhitelist = new File(dirConfigs, "whitelist.conf");
    private ArrayList<String> arrWhitelist;
    private File fleBlacklist = new File(dirConfigs, "blacklist.conf");
    private ArrayList<String> arrBlacklist;
    private File fleBlocklists = new File(dirConfigs, "blocklists.conf");
    private ArrayList<String> arrBlocklists;
    private Set<String> arrDomains = new HashSet<String>();
    private ArrayList<String> arrOut = new ArrayList<String>();
    private boolean cache;
    private boolean optimizeHosts;
    private boolean optimizeIPs;
    private int format;
    private File fleOutput;
    private File fleOutputOld;

    public HostsManager() {

    }

    public HostsManager(boolean cache, boolean optimizeHosts, boolean optimizeIPs, int format, File fleOutput) {
        this.cache = cache;
        this.optimizeHosts = optimizeHosts;
        this.optimizeIPs = optimizeIPs;
        this.format = format;
        this.fleOutput = fleOutput;
        fleOutputOld = new File(fleOutput + ".bak");
        arrHeader = utils.readFileIntoArray(fleHeader);
        arrWhitelist = utils.readFileIntoArray(fleWhitelist);
        arrBlacklist = utils.readFileIntoArray(fleBlacklist);
        arrBlocklists = utils.readFileIntoArray(fleBlocklists);
    }

    public void generateDefaults() {
        try {
            dirConfigs.mkdirs();
            dirCache.mkdirs();
            fleHeader.createNewFile();
            fleWhitelist.createNewFile();
            fleBlacklist.createNewFile();
            fleBlocklists.createNewFile();
            //TODO Possibly set some defaults
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update() {
        try {
            for(String url : arrBlocklists) {
                File out = new File(dirCache, utils.byteArrayToHexString(MessageDigest.getInstance("MD5").digest(url.getBytes("utf-8"))) + utils.identifyFileType(url));
                utils.downloadFile(url, out.toPath(), cache);
                arrDomains.addAll(utils.readHostsFileIntoArray(out));
            }
            arrDomains.addAll(arrBlacklist);
            for(String domain : arrWhitelist) {
                arrDomains.remove(domain);
            }
            ArrayList<String> arrDomainsNew = new ArrayList<String>();
            arrDomainsNew.addAll(arrDomains);
            Collections.sort(arrDomainsNew);
            switch(format) {
                case 0:
                    if(optimizeHosts) {
                        String line = "";
                        for(int x = 0; x < arrDomainsNew.size(); x++) {
                            String domain = arrDomainsNew.get(x);
                            if(x == (arrDomainsNew.size() - 1)) {
                                line += domain;
                                if (optimizeIPs)
                                    arrOut.add("0.0.0.0 " + line);
                                else
                                    arrOut.add("127.0.0.1 " + line);
                            } else if(line.split(" ").length >= 5) {
                                if (optimizeIPs)
                                    arrOut.add("0.0.0.0 " + line);
                                else
                                    arrOut.add("127.0.0.1 " + line);
                                line = domain + " ";
                            } else {
                                line += domain + " ";
                            }
                        }
                    } else {
                        if (optimizeIPs)
                            for (String domain : arrDomainsNew)
                                arrOut.add("0.0.0.0 " + domain);
                        else
                            for (String domain : arrDomainsNew)
                                arrOut.add("127.0.0.1 " + domain);
                    }
                    break;
                case 1://Optimizations aren't allowed for domains only
                    arrOut.addAll(arrDomainsNew);
                    break;
            }
            fleOutput.renameTo(fleOutputOld);
            PrintWriter writer = new PrintWriter(fleOutput, "UTF-8");
            if(format == 0) {
                for (String line : arrHeader) {
                    writer.println(line);
                }
            }
            for (String line : arrOut) {
                writer.println(line);
            }
            writer.close();
            utils.showAlert("Hosts Manager", "Successfully exported domains to file", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            utils.showAlert("Hosts Manager", "Failed to generate file", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void rollback() {
        try {
            fleOutput.delete();
            fleOutputOld.renameTo(fleOutput);
            utils.showAlert("Hosts Manager", "Successfully rolled back file", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            utils.showAlert("Hosts Manager", "Failed to rollback file", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void reset() {
        try {
            fleOutput.delete();
            PrintWriter writer = new PrintWriter(fleOutput, "UTF-8");
            for (String line : arrHeader) {
                writer.println(line);
            }
            writer.close();
            utils.showAlert("Hosts Manager", "Successfully reset file", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            utils.showAlert("Hosts Manager", "Failed to reset file", JOptionPane.ERROR_MESSAGE);
        }
    }

}
