package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;

import ch.cyberduck.core.*;
import ch.cyberduck.ui.cocoa.threading.AbstractBackgroundAction;
import ch.cyberduck.ui.cocoa.util.HyperlinkAttributedStringFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import com.enterprisedt.net.ftp.FTPConnectMode;

/**
 * @version $Id$
 */
public class CDBookmarkController extends CDWindowController {
    private static Logger log = Logger.getLogger(CDBookmarkController.class);

    private NSPopUpButton protocolPopup; // IBOutlet

    public void setProtocolPopup(NSPopUpButton protocolPopup) {
        this.protocolPopup = protocolPopup;
        this.protocolPopup.setEnabled(true);
        this.protocolPopup.removeAllItems();
        this.protocolPopup.addItemsWithTitles(new NSArray(Protocol.getProtocolDescriptions()));
        final Protocol[] protocols = Protocol.getKnownProtocols();
        for(int i = 0; i < protocols.length; i++) {
            final NSMenuItem item = this.protocolPopup.itemWithTitle(protocols[i].getDescription());
            item.setRepresentedObject(protocols[i]);
            item.setImage(CDIconCache.instance().iconForName(protocols[i].icon(), 16));
        }
        this.protocolPopup.setTarget(this);
        this.protocolPopup.setAction(new NSSelector("protocolSelectionChanged", new Class[]{Object.class}));
    }

    public void protocolSelectionChanged(final NSPopUpButton sender) {
        log.debug("protocolSelectionChanged:" + sender);
        final Protocol selected = (Protocol) protocolPopup.selectedItem().representedObject();
        this.host.setPort(selected.getDefaultPort());
        if(this.host.getProtocol().getDefaultHostname().equals(this.host.getHostname())) {
            this.host.setHostname(selected.getDefaultHostname());
        }
        if(!selected.isConfigurable()) {
            this.host.setWebURL(null);
            if(selected == Protocol.IDISK) {
                CDDotMacController controller = new CDDotMacController();
                final String member = controller.getAccountName();
                controller.invalidate();
                if(null != member) {
                    // Account name configured in System Preferences
                    this.host.getCredentials().setUsername(member);
                    this.host.setDefaultPath(Path.DELIMITER + member);
                }
            }
        }
        this.host.setProtocol(selected);
        this.itemChanged();
        this.reachable();
    }

    private NSPopUpButton encodingPopup; // IBOutlet

    public void setEncodingPopup(NSPopUpButton encodingPopup) {
        this.encodingPopup = encodingPopup;
        this.encodingPopup.setEnabled(true);
        this.encodingPopup.removeAllItems();
        this.encodingPopup.addItem(DEFAULT);
        this.encodingPopup.menu().addItem(new NSMenuItem().separatorItem());
        this.encodingPopup.addItemsWithTitles(new NSArray(
                ((CDMainController) NSApplication.sharedApplication().delegate()).availableCharsets()));
        if(null == this.host.getEncoding()) {
            this.encodingPopup.selectItemWithTitle(DEFAULT);
        }
        else {
            this.encodingPopup.selectItemWithTitle(this.host.getEncoding());
        }
        this.encodingPopup.setTarget(this);
        final NSSelector action = new NSSelector("encodingSelectionChanged", new Class[]{Object.class});
        this.encodingPopup.setAction(action);
    }

    public void encodingSelectionChanged(final NSPopUpButton sender) {
        log.debug("encodingSelectionChanged:" + sender);
        if(sender.selectedItem().title().equals(DEFAULT)) {
            this.host.setEncoding(null);
        }
        else {
            this.host.setEncoding(sender.selectedItem().title());
        }
        this.itemChanged();
    }

    private NSTextField nicknameField; // IBOutlet

    public void setNicknameField(NSTextField nicknameField) {
        this.nicknameField = nicknameField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("nicknameInputDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.nicknameField);
    }

    private NSTextField hostField; // IBOutlet

    public void setHostField(NSTextField hostField) {
        this.hostField = hostField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("hostFieldDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.hostField);
    }

    private NSButton alertIcon; // IBOutlet

    public void setAlertIcon(NSButton alertIcon) {
        this.alertIcon = alertIcon;
        this.alertIcon.setHidden(true);
        this.alertIcon.setTarget(this);
        this.alertIcon.setAction(new NSSelector("launchNetworkAssistant", new Class[]{NSButton.class}));
    }

    public void launchNetworkAssistant(final NSButton sender) {
        this.host.diagnose();
    }

    private NSTextField portField; // IBOutlet

    public void setPortField(NSTextField portField) {
        this.portField = portField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("portInputDidEndEditing", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.portField);
    }

    private NSTextField pathField; // IBOutlet

    public void setPathField(NSTextField pathField) {
        this.pathField = pathField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("pathInputDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.pathField);
    }

    private NSTextField urlField; // IBOutlet

    public void setUrlField(NSTextField urlField) {
        this.urlField = urlField;
        this.urlField.setAllowsEditingTextAttributes(true);
        this.urlField.setSelectable(true);
    }

    private NSTextField usernameField; // IBOutlet

    public void setUsernameField(NSTextField usernameField) {
        this.usernameField = usernameField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("usernameInputDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.usernameField);
    }

    private NSTextField webURLField;

    public void setWebURLField(NSTextField webURLField) {
        this.webURLField = webURLField;
        ((NSTextFieldCell) this.webURLField.cell()).setPlaceholderString(
                host.getDefaultWebURL()
        );
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("webURLInputDidChange", new Class[]{NSNotification.class}),
                NSControl.ControlTextDidChangeNotification,
                this.webURLField);
    }

    private NSTextView commentField; // IBOutlet

    public void setCommentField(NSTextView commentField) {
        this.commentField = commentField;
        NSNotificationCenter.defaultCenter().addObserver(this,
                new NSSelector("commentInputDidChange", new Class[]{NSNotification.class}),
                NSText.TextDidChangeNotification,
                this.commentField);
    }

//    private NSPopUpButton timezonePopup; //IBOutlet
//
//    public void setTimezonePopup(NSPopUpButton timezonePopup) {
//        this.timezonePopup = timezonePopup;
//        this.timezonePopup.setTarget(this);
//        this.timezonePopup.setAction(new NSSelector("timezonePopupClicked", new Class[]{NSPopUpButton.class}));
//        this.timezonePopup.removeAllItems();
//        this.timezonePopup.addItem(DEFAULT);
//        this.timezonePopup.menu().addItem(new NSMenuItem().separatorItem());
//        String[] ids = TimeZone.getAvailableIDs();
//        for(int i = 0; i < ids.length; i++) {
//            this.timezonePopup.addItem(TimeZone.getTimeZone(ids[i]).getDisplayName());
//        }
//        if(this.host.getTimezone().equals(TimeZone.getDefault())) {
//            this.timezonePopup.setTitle(DEFAULT);
//        }
//        else {
//            this.timezonePopup.setTitle(this.host.getTimezone().getDisplayName());
//        }
//    }
//
//    public void timezonePopupClicked(NSPopUpButton sender) {
//        String selected = sender.selectedItem().title();
//        if(selected.equals(DEFAULT)) {
//            this.host.setTimezone(null);
//        }
//        else {
//            String[] ids = TimeZone.getAvailableIDs();
//            TimeZone tz;
//            for(int i = 0; i < ids.length; i++) {
//                if((tz = TimeZone.getTimeZone(ids[i])).getDisplayName().equals(selected)) {
//                    this.host.setTimezone(tz);
//                    break;
//                }
//            }
//        }
//        this.itemChanged();
//    }

    private NSPopUpButton connectmodePopup; //IBOutlet

    private static final String CONNECTMODE_ACTIVE = NSBundle.localizedString("Active", "");
    private static final String CONNECTMODE_PASSIVE = NSBundle.localizedString("Passive", "");

    public void setConnectmodePopup(NSPopUpButton connectmodePopup) {
        this.connectmodePopup = connectmodePopup;
        this.connectmodePopup.setTarget(this);
        this.connectmodePopup.setAction(new NSSelector("connectmodePopupClicked", new Class[]{NSPopUpButton.class}));
        this.connectmodePopup.removeAllItems();
        this.connectmodePopup.addItem(DEFAULT);
        this.connectmodePopup.menu().addItem(new NSMenuItem().separatorItem());
        this.connectmodePopup.addItemsWithTitles(new NSArray(new String[]{CONNECTMODE_ACTIVE, CONNECTMODE_PASSIVE}));
    }

    public void connectmodePopupClicked(final NSPopUpButton sender) {
        if(sender.selectedItem().title().equals(DEFAULT)) {
            this.host.setFTPConnectMode(null);
        }
        else if(sender.selectedItem().title().equals(CONNECTMODE_ACTIVE)) {
            this.host.setFTPConnectMode(FTPConnectMode.ACTIVE);
        }
        else if(sender.selectedItem().title().equals(CONNECTMODE_PASSIVE)) {
            this.host.setFTPConnectMode(FTPConnectMode.PASV);
        }
        this.itemChanged();
    }

    private NSPopUpButton transferPopup; //IBOutlet

    private static final String TRANSFER_NEWCONNECTION = NSBundle.localizedString("Open new connection", "");
    private static final String TRANSFER_BROWSERCONNECTION = NSBundle.localizedString("Use browser connection", "");

    public void setTransferPopup(NSPopUpButton transferPopup) {
        this.transferPopup = transferPopup;
        this.transferPopup.setTarget(this);
        this.transferPopup.setAction(new NSSelector("transferPopupClicked", new Class[]{NSPopUpButton.class}));
        this.transferPopup.removeAllItems();
        this.transferPopup.addItem(DEFAULT);
        this.transferPopup.menu().addItem(new NSMenuItem().separatorItem());
        this.transferPopup.addItemsWithTitles(new NSArray(new String[]{TRANSFER_NEWCONNECTION, TRANSFER_BROWSERCONNECTION}));
    }

    public void transferPopupClicked(final NSPopUpButton sender) {
        if(sender.selectedItem().title().equals(DEFAULT)) {
            this.host.setMaxConnections(null);
        }
        else if(sender.selectedItem().title().equals(TRANSFER_BROWSERCONNECTION)) {
            this.host.setMaxConnections(1);
        }
        else if(sender.selectedItem().title().equals(TRANSFER_NEWCONNECTION)) {
            this.host.setMaxConnections(-1);
        }
        this.itemChanged();
    }

    private NSPopUpButton downloadPathPopup; //IBOutlet

    private static final String CHOOSE = NSBundle.localizedString("Choose", "") + "...";

    public void setDownloadPathPopup(NSPopUpButton downloadPathPopup) {
        this.downloadPathPopup = downloadPathPopup;
        this.downloadPathPopup.setTarget(this);
        final NSSelector action = new NSSelector("downloadPathPopupClicked", new Class[]{NSPopUpButton.class});
        this.downloadPathPopup.setAction(action);
        this.downloadPathPopup.removeAllItems();

        // Default download folder
        this.addDownloadPath(action, host.getDownloadFolder());
        this.downloadPathPopup.menu().addItem(new NSMenuItem().separatorItem());
        // Shortcut to the Desktop
        this.addDownloadPath(action, new Local("~/Desktop"));
        // Shortcut to user home
        this.addDownloadPath(action, new Local("~"));
        // Shortcut to user downloads for 10.5
        this.addDownloadPath(action, new Local("~/Downloads"));
        // Choose another folder

        // Choose another folder
        this.downloadPathPopup.menu().addItem(new NSMenuItem().separatorItem());
        this.downloadPathPopup.menu().addItem(CHOOSE, action, "");
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setTarget(this);
    }

    private void addDownloadPath(NSSelector action, Local f) {
        if(f.exists()) {
            this.downloadPathPopup.menu().addItem(NSPathUtilities.displayNameAtPath(
                    f.getAbsolute()), action, "");
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setTarget(this);
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setImage(
                    CDIconCache.instance().iconForPath(f, 16)
            );
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setRepresentedObject(
                    f.getAbsolute());
            if(host.getDownloadFolder().equals(f)) {
                this.downloadPathPopup.selectItemAtIndex(this.downloadPathPopup.numberOfItems() - 1);
            }
        }
    }

    private NSOpenPanel downloadPathPanel;

    public void downloadPathPopupClicked(final NSMenuItem sender) {
        if(sender.title().equals(CHOOSE)) {
            downloadPathPanel = NSOpenPanel.openPanel();
            downloadPathPanel.setCanChooseFiles(false);
            downloadPathPanel.setCanChooseDirectories(true);
            downloadPathPanel.setAllowsMultipleSelection(false);
            downloadPathPanel.setCanCreateDirectories(true);
            downloadPathPanel.beginSheetForDirectory(null, null, null, this.window, this, new NSSelector("downloadPathPanelDidEnd", new Class[]{NSOpenPanel.class, int.class, Object.class}), null);
        }
        else {
            host.setDownloadFolder(sender.representedObject().toString());
            this.itemChanged();
        }
    }

    public void downloadPathPanelDidEnd(NSOpenPanel sheet, int returncode, Object contextInfo) {
        if(returncode == CDSheetCallback.DEFAULT_OPTION) {
            NSArray selected = sheet.filenames();
            String filename;
            if((filename = (String) selected.lastObject()) != null) {
                host.setDownloadFolder(filename);
            }
        }
        else {
            host.setDownloadFolder(null);
        }
        this.downloadPathPopup.itemAtIndex(0).setTitle(NSPathUtilities.displayNameAtPath(
                host.getDownloadFolder().getAbsolute()));
        this.downloadPathPopup.itemAtIndex(0).setRepresentedObject(
                host.getDownloadFolder().getAbsolute());
        this.downloadPathPopup.itemAtIndex(0).setImage(
                CDIconCache.instance().iconForPath(host.getDownloadFolder(), 16));
        this.downloadPathPopup.selectItemAtIndex(0);
        this.downloadPathPanel = null;
        this.itemChanged();
    }

    private NSButton toggleOptionsButton;

    public void setToggleOptionsButton(NSButton toggleOptionsButton) {
        this.toggleOptionsButton = toggleOptionsButton;
    }

    /**
     *
     */
    public static class Factory {
        private static final Map<Host, CDBookmarkController> open
                = new HashMap<Host, CDBookmarkController>();

        public static CDBookmarkController create(final Host host) {
            if(open.containsKey(host)) {
                return open.get(host);
            }
            final CDBookmarkController c = new CDBookmarkController(host) {
                public void windowWillClose(NSNotification notification) {
                    Factory.open.remove(host);
                    super.windowWillClose(notification);
                }
            };
            open.put(host, c);
            return c;
        }
    }

    /**
     * The bookmark
     */
    private Host host;

    /**
     * @param host The bookmark to edit
     */
    private CDBookmarkController(final Host host) {
        this.host = host;
        // Register for bookmark delete event. Will close this window.
        HostCollection.defaultCollection().addListener(new AbstractCollectionListener<Host>() {
            public void collectionItemRemoved(Host item) {
                if(item.equals(host)) {
                    HostCollection.defaultCollection().removeListener(this);
                    final NSWindow window = window();
                    if(null != window) {
                        window.close();
                    }
                }
            }
        });
        this.loadBundle();
    }

    public void windowWillClose(NSNotification notification) {
        Preferences.instance().setProperty("bookmark.toggle.options", this.toggleOptionsButton.state());
        super.windowWillClose(notification);
    }

    protected String getBundleName() {
        return "Bookmark";
    }

    public void awakeFromNib() {
        this.cascade();
        this.init();
        this.setState(this.toggleOptionsButton, Preferences.instance().getBoolean("bookmark.toggle.options"));
    }

    private NSTextField pkLabel;

    public void setPkLabel(NSTextField pkLabel) {
        this.pkLabel = pkLabel;
    }

    private NSButton pkCheckbox;

    public void setPkCheckbox(NSButton pkCheckbox) {
        this.pkCheckbox = pkCheckbox;
        this.pkCheckbox.setTarget(this);
        this.pkCheckbox.setAction(new NSSelector("pkCheckboxSelectionChanged", new Class[]{Object.class}));
    }

    private NSOpenPanel publicKeyPanel;

    public void pkCheckboxSelectionChanged(final NSButton sender) {
        log.debug("pkCheckboxSelectionChanged");
        if(this.pkLabel.stringValue().equals(NSBundle.localizedString("No Private Key selected", ""))) {
            publicKeyPanel = NSOpenPanel.openPanel();
            publicKeyPanel.setCanChooseDirectories(false);
            publicKeyPanel.setCanChooseFiles(true);
            publicKeyPanel.setAllowsMultipleSelection(false);
            publicKeyPanel.beginSheetForDirectory(NSPathUtilities.stringByExpandingTildeInPath("~/.ssh"), null, null, this.window(),
                    this,
                    new NSSelector("pkSelectionPanelDidEnd", new Class[]{NSOpenPanel.class, int.class, Object.class}), null);
        }
        else {
            this.host.getCredentials().setIdentity(null);
            this.itemChanged();
        }
    }

    public void pkSelectionPanelDidEnd(NSOpenPanel sheet, int returncode, Object context) {
        log.debug("pkSelectionPanelDidEnd");
        if(returncode == NSPanel.OKButton) {
            NSArray selected = sheet.filenames();
            java.util.Enumeration enumerator = selected.objectEnumerator();
            while(enumerator.hasMoreElements()) {
                this.host.getCredentials().setIdentity((String) enumerator.nextElement());
            }
        }
        if(returncode == NSPanel.CancelButton) {
            this.host.getCredentials().setIdentity(null);
        }
        publicKeyPanel = null;
        this.itemChanged();
    }

    public void hostFieldDidChange(final NSNotification sender) {
        String input = hostField.stringValue();
        if(Protocol.isURL(input)) {
            this.host.init(Host.parse(input).getAsDictionary());
        }
        else {
            this.host.setHostname(input);
        }
        this.itemChanged();
        this.reachable();
    }

    private void reachable() {
        if(StringUtils.isNotBlank(host.getHostname())) {
            this.background(new AbstractBackgroundAction() {
                boolean reachable = false;

                public void run() {
                    reachable = host.isReachable();
                }

                public void cleanup() {
                    alertIcon.setHidden(reachable);
                }
            });
        }
        else {
            alertIcon.setHidden(true);
        }
    }

    public void portInputDidEndEditing(final NSNotification sender) {
        try {
            this.host.setPort(Integer.parseInt(portField.stringValue()));
        }
        catch(NumberFormatException e) {
            this.host.setPort(-1);
        }
        this.itemChanged();
    }

    public void pathInputDidChange(final NSNotification sender) {
        this.host.setDefaultPath(pathField.stringValue());
        this.itemChanged();
    }

    public void nicknameInputDidChange(final NSNotification sender) {
        this.host.setNickname(nicknameField.stringValue());
        this.itemChanged();
    }

    public void usernameInputDidChange(final NSNotification sender) {
        this.host.getCredentials().setUsername(usernameField.stringValue());
        this.itemChanged();
    }

    public void webURLInputDidChange(final NSNotification sender) {
        this.host.setWebURL(webURLField.stringValue());
        this.itemChanged();
    }

    public void commentInputDidChange(final NSNotification sender) {
        this.host.setComment(commentField.textStorage().stringReference().string());
        this.itemChanged();
    }

    /**
     * Updates the window title and url label with the properties of this bookmark
     * Propagates all fields with the properties of this bookmark
     */
    private void itemChanged() {
        HostCollection.defaultCollection().collectionItemChanged(host);
        this.init();
    }

    private void init() {
        window.setTitle(host.getNickname());
        this.updateField(hostField, host.getHostname());
        hostField.setEnabled(host.getProtocol().isConfigurable());
        this.updateField(nicknameField, host.getNickname());
        final String url;
        if(StringUtils.isNotBlank(host.getDefaultPath())) {
            url = host.toURL() + Path.normalize(host.getDefaultPath());
        }
        else {
            url = host.toURL();
        }
        urlField.setAttributedStringValue(
                HyperlinkAttributedStringFactory.create(
                        new NSMutableAttributedString(new NSAttributedString(url, TRUNCATE_MIDDLE_ATTRIBUTES)), url)
        );
        this.updateField(portField, String.valueOf(host.getPort()));
        portField.setEnabled(host.getProtocol().isConfigurable());
        this.updateField(pathField, host.getDefaultPath());
        this.updateField(usernameField, host.getCredentials().getUsername());
        if(host.getProtocol().equals(Protocol.S3)) {
            ((NSTextFieldCell) usernameField.cell()).setPlaceholderString(
                    NSBundle.localizedString("Access Key ID", "S3", "")
            );
        }
        else {
            ((NSTextFieldCell) usernameField.cell()).setPlaceholderString("");
        }
        protocolPopup.selectItemWithTitle(host.getProtocol().getDescription());
        if(null == host.getMaxConnections()) {
            transferPopup.selectItemWithTitle(DEFAULT);
        }
        else {
            transferPopup.selectItemWithTitle(
                    host.getMaxConnections() == 1 ? TRANSFER_BROWSERCONNECTION : TRANSFER_NEWCONNECTION);
        }
        connectmodePopup.setEnabled(host.getProtocol().equals(Protocol.FTP)
                || host.getProtocol().equals(Protocol.FTP_TLS));
        encodingPopup.setEnabled(host.getProtocol().isConfigurable());
        if(host.getProtocol().equals(Protocol.FTP)
                || host.getProtocol().equals(Protocol.FTP_TLS)) {
            if(null == host.getFTPConnectMode()) {
                connectmodePopup.selectItemWithTitle(DEFAULT);
            }
            else if(host.getFTPConnectMode().equals(FTPConnectMode.PASV)) {
                connectmodePopup.selectItemWithTitle(CONNECTMODE_PASSIVE);
            }
            else if(host.getFTPConnectMode().equals(FTPConnectMode.ACTIVE)) {
                connectmodePopup.selectItemWithTitle(CONNECTMODE_ACTIVE);
            }
        }
        pkCheckbox.setEnabled(host.getProtocol().equals(Protocol.SFTP));
        if(host.getCredentials().isPublicKeyAuthentication()) {
            pkCheckbox.setState(NSCell.OnState);
            this.updateField(pkLabel, host.getCredentials().getIdentity());
        }
        else {
            pkCheckbox.setState(NSCell.OffState);
            pkLabel.setStringValue(NSBundle.localizedString("No Private Key selected", ""));
        }
        webURLField.setEnabled(host.getProtocol().isConfigurable());
        if(!host.getWebURL().equals(host.getDefaultWebURL())) {
            this.updateField(webURLField, host.getWebURL());
        }
        this.updateField(commentField, host.getComment());
    }
}