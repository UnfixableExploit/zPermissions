/*
 * Copyright 2012 ZerothAngel <zerothangel@tyrannyofheaven.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tyrannyofheaven.bukkit.zPermissions.util;

import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.colorize;
import static org.tyrannyofheaven.bukkit.util.ToHMessageUtils.sendMessage;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.tyrannyofheaven.bukkit.util.ToHMessageUtils;
import org.tyrannyofheaven.bukkit.util.ToHStringUtils;
import org.tyrannyofheaven.bukkit.util.command.ParseException;
import org.tyrannyofheaven.bukkit.zPermissions.model.EntityMetadata;
import org.tyrannyofheaven.bukkit.zPermissions.model.Entry;
import org.tyrannyofheaven.bukkit.zPermissions.model.Membership;
import org.tyrannyofheaven.bukkit.zPermissions.model.PermissionEntity;

/**
 * Collection of static utils, constants, etc.
 * 
 * @author zerothangel
 */
public class Utils {

    public final static Comparator<PermissionEntity> PERMISSION_ENTITY_ALPHA_COMPARATOR = new Comparator<PermissionEntity>() {
        @Override
        public int compare(PermissionEntity a, PermissionEntity b) {
            return a.getDisplayName().compareTo(b.getDisplayName());
        }
    };

    private final static Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)\\s*(h(?:ours?)?|d(?:ays?)?|m(?:onths?)?|y(?:ears?)?)?$", Pattern.CASE_INSENSITIVE);

    public final static Comparator<Entry> ENTRY_COMPARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry a, Entry b) {
            if (a.getRegion() != null && b.getRegion() == null)
                return 1;
            else if (a.getRegion() == null && b.getRegion() != null)
                return -1;
            else if (a.getRegion() != null && b.getRegion() != null) {
                int regions = a.getRegion().getName().compareTo(b.getRegion().getName());
                if (regions != 0) return regions;
            }

            if (a.getWorld() != null && b.getWorld() == null)
                return 1;
            else if (a.getWorld() == null && b.getWorld() != null)
                return -1;
            else if (a.getWorld() != null && b.getWorld() != null) {
                int worlds = a.getWorld().getName().compareTo(b.getWorld().getName());
                if (worlds != 0) return worlds;
            }

            return a.getPermission().compareTo(b.getPermission());
        }
    };

    private final static Comparator<PermissionInfo> PERMISSION_INFO_COMPARATOR = new Comparator<PermissionInfo>() {
        @Override
        public int compare(PermissionInfo a, PermissionInfo b) {
            return a.getPermission().compareTo(b.getPermission());
        }
    };

    private static final Comparator<EntityMetadata> METADATA_COMPARATOR = new Comparator<EntityMetadata>() {
        @Override
        public int compare(EntityMetadata a, EntityMetadata b) {
            return a.getName().compareToIgnoreCase(b.getName());
        }
    };

    public static List<Entry> sortPermissions(Collection<Entry> entries) {
        List<Entry> result = new ArrayList<Entry>(entries);
        Collections.sort(result, ENTRY_COMPARATOR);
        return result;
    }

    public static List<EntityMetadata> sortMetadata(Collection<EntityMetadata> metadata) {
        List<EntityMetadata> result = new ArrayList<EntityMetadata>(metadata);
        Collections.sort(result, METADATA_COMPARATOR);
        return result;
    }

    public static void displayPermissions(Plugin plugin, CommandSender sender, List<String> header, Map<String, Boolean> permissions, String filter) {
        List<PermissionInfo> permList = new ArrayList<PermissionInfo>(permissions.size());
        for (Map.Entry<String, Boolean> me : permissions.entrySet()) {
            permList.add(new PermissionInfo(me.getKey(), me.getValue(), null));
        }
        displayPermissions(plugin, sender, header, permList, filter, false);
    }

    public static void displayPermissions(Plugin plugin, CommandSender sender, List<String> header, List<PermissionInfo> permissions, String filter, boolean verbose) {
        if (header == null)
            header = Collections.emptyList();

        // Sort for display
        permissions = new ArrayList<PermissionInfo>(permissions); // make copy
        Collections.sort(permissions, PERMISSION_INFO_COMPARATOR);

        // Convert to lines and filter
        List<String> lines = new ArrayList<String>(header.size() + permissions.size());
        lines.addAll(header);
        if (filter != null) {
            filter = filter.toLowerCase().trim();
            if (filter.isEmpty())
                filter = null;
        }
        for (PermissionInfo pi : permissions) {
            String key = pi.getPermission();
            if (filter != null && !key.contains(filter)) continue;
            String source;
            if (verbose) {
                source = pi.getSource() != null ? (ChatColor.RED + " [" + pi.getSource() + "]") : "";
            }
            else {
                boolean notMine = pi.getSource() != null &&
                        !plugin.getName().equals(pi.getSource());
                source = notMine? (ChatColor.RED + " *") : "";
            }
            lines.add(String.format(colorize("{DARK_GREEN}- {GOLD}%s{DARK_GREEN}: {GREEN}%s%s"), key, pi.getValue(), source));
        }

        if (lines.isEmpty()) {
            sendMessage(sender, colorize("{RED}No %spermissions found."), filter == null ? "" : "matching ");
        }
        else {
            ToHMessageUtils.displayLines(plugin, sender, lines);
        }
    }

    public static String displayGroups(String defaultGroup, List<Membership> memberships) {
        boolean gotGroup = false;

        Date now = new Date();
        StringBuilder sb = new StringBuilder();
        for (Iterator<Membership> i = memberships.iterator(); i.hasNext();) {
            Membership membership = i.next();
            if (membership.getExpiration() == null || membership.getExpiration().after(now)) {
                sb.append(ChatColor.DARK_GREEN);
                gotGroup = true;
            }
            else
                sb.append(ChatColor.GRAY);

            sb.append(membership.getGroup().getDisplayName());

            if (membership.getExpiration() != null) {
                sb.append('[');
                sb.append(Utils.dateToString(membership.getExpiration()));
                sb.append(']');
            }

            if (i.hasNext()) {
                sb.append(ChatColor.YELLOW);
                sb.append(", ");
            }
        }

        // Add default group if we got nothing
        if (!gotGroup) {
            if (sb.length() > 0) {
                sb.append(ChatColor.YELLOW);
                sb.append(", ");
            }
            sb.append(ChatColor.DARK_GREEN);
            sb.append(defaultGroup);
        }

        return sb.toString();
    }

    public static List<String> toMembers(Collection<Membership> memberships) {
        List<String> result = new ArrayList<String>(memberships.size());
        for (Membership membership : memberships) {
            result.add(membership.getMember());
        }
        return result;
    }

    public static List<String> toGroupNames(Collection<Membership> memberships) {
        List<String> result = new ArrayList<String>(memberships.size());
        for (Membership membership : memberships) {
            result.add(membership.getGroup().getDisplayName());
        }
        return result;
    }

    public static List<Membership> filterExpired(Collection<Membership> memberships) {
        List<Membership> result = new ArrayList<Membership>(memberships.size());
        Date now = new Date();
        for (Membership membership : memberships) {
            if (membership.getExpiration() == null || membership.getExpiration().after(now))
                result.add(membership);
        }
        return result;
    }

    public static Date parseDurationTimestamp(String duration, String[] args) {
        if (!ToHStringUtils.hasText(duration))
            return null;
        
        if (duration != null) {
            // Append args, if present
            if (args.length > 0)
                duration = duration + " " + ToHStringUtils.delimitedString(" ", (Object[])args);
        }

        duration = duration.trim();

        Matcher match = DURATION_PATTERN.matcher(duration);
        if (match.matches()) {
            int unitsInt = Calendar.DAY_OF_MONTH;

            int durationInt = Integer.valueOf(match.group(1));
            if (durationInt < 1)
                throw new ParseException("Invalid value: duration/timestamp"); // NB Should match option name

            String units = match.group(2);

            if (units != null) {
                units = units.toLowerCase();

                if ("hours".equals(units) || "hour".equals(units) || "h".equals(units))
                    unitsInt = Calendar.HOUR;
                else if ("days".equals(units) || "day".equals(units) || "d".equals(units))
                    unitsInt = Calendar.DAY_OF_MONTH;
                else if ("months".equals(units) || "month".equals(units) || "m".equals(units))
                    unitsInt = Calendar.MONTH;
                else if ("years".equals(units) || "year".equals(units) || "y".equals(units))
                    unitsInt = Calendar.YEAR;
                else
                    throw new ParseException("units must be hours, days, months, years");
            }

            Calendar cal = Calendar.getInstance();
            cal.add(unitsInt, durationInt);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTime();
        }
        else {
            // Try ISO 8601 date
            duration = duration.toUpperCase(); // Make sure that 'T' is capitalized
            try {
                Calendar cal = DatatypeConverter.parseDateTime(duration);
                cal.set(Calendar.MILLISECOND, 0);
                return cal.getTime();
            }
            catch (IllegalArgumentException e2) {
                // One last try. Append :00
                // WHY U SO STRICT DatatypeConverter?!
                try {
                    Calendar cal = DatatypeConverter.parseDateTime(duration + ":00");
                    cal.set(Calendar.MILLISECOND, 0);
                    return cal.getTime();
                }
                catch (IllegalArgumentException e3) {
                    throw new ParseException("Invalid value: duration/timestamp"); // NB Should match option name
                }
            }
        }
    }

    // Suitable for user viewing (e.g. not dumps)
    public static String dateToString(Date date) {
        if (date == null)
            throw new IllegalArgumentException("date cannot be null");
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        
        String result = DatatypeConverter.printDateTime(cal);
        
        if (result.length() < 16)
            return result;
        else
            return result.substring(0, 16);
    }

    /**
     * Give a little warning if the player isn't online.
     * 
     * @param sender the CommandSender to send warning to
     * @param playerName the player name
     */
    public static void checkPlayer(CommandSender sender, String playerName) {
        if (Bukkit.getPlayerExact(playerName) == null) {
            sendMessage(sender, colorize("{GRAY}(Player not online, make sure the name is correct)"));
        }
    }

    public static class PermissionInfo {
        
        private final String permission;
        
        private final boolean value;
        
        private final String source;
        
        public PermissionInfo(String permission, boolean value, String source) {
            if (!ToHStringUtils.hasText(permission))
                throw new IllegalArgumentException("permission must have a value");
            this.permission = permission.toLowerCase();
            this.value = value;
            this.source = source;
        }

        public String getPermission() {
            return permission;
        }

        public boolean getValue() {
            return value;
        }

        public String getSource() {
            return source;
        }

    }

}