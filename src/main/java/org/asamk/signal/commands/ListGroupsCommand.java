package org.asamk.signal.commands;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

import org.asamk.signal.JsonWriter;
import org.asamk.signal.OutputType;
import org.asamk.signal.manager.Manager;
import org.asamk.signal.manager.groups.GroupInviteLinkUrl;
import org.asamk.signal.manager.storage.groups.GroupInfo;
import org.asamk.signal.manager.storage.groups.GroupInfoV2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListGroupsCommand implements LocalCommand {

    private final static Logger logger = LoggerFactory.getLogger(ListGroupsCommand.class);

    private static Set<String> resolveMembers(Manager m, Set<SignalServiceAddress> addresses) {
        return addresses.stream()
                .map(m::resolveSignalServiceAddress)
                .map(SignalServiceAddress::getLegacyIdentifier)
                .collect(Collectors.toSet());
    }

    private static void printGroupPlainText(Manager m, GroupInfo group, boolean detailed) {
        if (detailed) {
            final GroupInviteLinkUrl groupInviteLink = group.getGroupInviteLink();

            System.out.println(String.format(
                    "Id: %s Name: %s  Active: %s Blocked: %b Members: %s Pending members: %s Requesting members: %s Link: %s",
                    group.getGroupId().toBase64(),
                    group.getTitle(),
                    group.isMember(m.getSelfAddress()),
                    group.isBlocked(),
                    resolveMembers(m, group.getMembers()),
                    resolveMembers(m, group.getPendingMembers()),
                    resolveMembers(m, group.getRequestingMembers()),
                    groupInviteLink == null ? '-' : groupInviteLink.getUrl()));
        } else {
            System.out.println(String.format("Id: %s Name: %s  Active: %s Blocked: %b",
                    group.getGroupId().toBase64(),
                    group.getTitle(),
                    group.isMember(m.getSelfAddress()),
                    group.isBlocked()));
        }
    }

    @Override
    public void attachToSubparser(final Subparser subparser) {
        subparser.addArgument("-d", "--detailed")
                .action(Arguments.storeTrue())
                .help("List the members and group invite links of each group. If output=json, then this is always set");

        subparser.help("List group information including names, ids, active status, blocked status and members");
    }

    @Override
    public Set<OutputType> getSupportedOutputTypes() {
        return Set.of(OutputType.PLAIN_TEXT, OutputType.JSON);
    }

    static public String saveToJsonString(final Manager m, GroupInfo group) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final JsonWriter jsonWriter = new JsonWriter(bos);
        final GroupInviteLinkUrl groupInviteLink = group.getGroupInviteLink();

        JsonGroup g = new JsonGroup(group.getGroupId().toBase64(),
                group.getTitle(),
                group.isMember(m.getSelfAddress()),
                group.isBlocked(),
                resolveMembers(m, group.getMembers()),
                resolveMembers(m, group.getPendingMembers()),
                resolveMembers(m, group.getRequestingMembers()),
                (group instanceof GroupInfoV2 ? ((GroupInfoV2)group).getAccessControlAddFromInviteLink() : ""),
                groupInviteLink == null ? null : groupInviteLink.getUrl());
        jsonWriter.write(g);
        return new String(bos.toByteArray(), "UTF-8");
    }

    static public String saveToJsonString(final Manager m) throws IOException {
        List<GroupInfo> groups = m.getGroups();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final JsonWriter jsonWriter = new JsonWriter(bos);

        List<JsonGroup> jsonGroups = new ArrayList<>();
        for (GroupInfo group : groups) {
            final GroupInviteLinkUrl groupInviteLink = group.getGroupInviteLink();

            jsonGroups.add(new JsonGroup(group.getGroupId().toBase64(),
                    group.getTitle(),
                    group.isMember(m.getSelfAddress()),
                    group.isBlocked(),
                    resolveMembers(m, group.getMembers()),
                    resolveMembers(m, group.getPendingMembers()),
                    resolveMembers(m, group.getRequestingMembers()),
                    (group instanceof GroupInfoV2 ? ((GroupInfoV2)group).getAccessControlAddFromInviteLink() : ""),
                    groupInviteLink == null ? null : groupInviteLink.getUrl()));
        }

        jsonWriter.write(jsonGroups);
        return new String(bos.toByteArray(), "UTF-8");
    }

    @Override
    public int handleCommand(final Namespace ns, final Manager m) {
        if (ns.get("output") == OutputType.JSON) {
            try {
                String s = saveToJsonString(m);
                System.out.println(s);
            } catch (IOException e) {
                logger.error("Failed to write json object: {}", e.getMessage());
                return 3;
            }
            return 0;
        } else {
            boolean detailed = ns.getBoolean("detailed");
            for (GroupInfo group : m.getGroups()) {
                printGroupPlainText(m, group, detailed);
            }
        }

        return 0;
    }

    private static final class JsonGroup {

        public String id;
        public String name;
        public boolean isMember;
        public boolean isBlocked;

        public Set<String> members;
        public Set<String> pendingMembers;
        public Set<String> requestingMembers;
        public String groupInviteAccessControl;
        public String groupInviteLink;

        public JsonGroup(
                String id,
                String name,
                boolean isMember,
                boolean isBlocked,
                Set<String> members,
                Set<String> pendingMembers,
                Set<String> requestingMembers,
                String groupInviteAccessControl,
                String groupInviteLink
        ) {
            this.id = id;
            this.name = name;
            this.isMember = isMember;
            this.isBlocked = isBlocked;

            this.members = members;
            this.pendingMembers = pendingMembers;
            this.requestingMembers = requestingMembers;
            this.groupInviteAccessControl = groupInviteAccessControl;
            this.groupInviteLink = groupInviteLink;
        }
    }
}
