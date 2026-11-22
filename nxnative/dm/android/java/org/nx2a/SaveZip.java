package org.nx2a;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Where a game's save is in an exported .zip (SaveBridge): its entries' names only, so it is tested without Android (tests/test_save_folder.py). */
final class SaveZip {
    private SaveZip() {}

    /** What scan found: prefix, the save's folder in the zip ("<title ID>/", or "<folder>/<title ID>/" when the zip was made of a folder holding it), null none;
     *  files, the files under it; others, the title IDs of other games' save folders; unsafe, a name with a ".." part (the zip is refused) */
    static final class Layout { String prefix; int files; final TreeSet<String> others = new TreeSet<>(); String unsafe; }

    /** An entry's name with / for \ and no leading / */
    static String norm(String n) { n = n.replace('\\', '/'); while (n.startsWith("/")) n = n.substring(1); return n; }

    /** names: the entries' names (norm), a folder's ending in /. The save's folder is matched without regard to case (a PC's zip tool may change it) */
    static Layout scan(List<String> names, String title) {
        Layout l = new Layout();
        for (String n : names) {
            String[] p = n.split("/");
            for (String s : p) if (s.equals("..")) l.unsafe = n;
            if (p[0].equals("__MACOSX")) continue;   // (macOS's zip tool adds it)
            for (int k = 0; k < Math.min(2, p.length - (n.endsWith("/") ? 0 : 1)); k++) {
                if (p[k].equalsIgnoreCase(title)) { if (l.prefix == null) l.prefix = join(p, k + 1); break; }
                String base = p[k].endsWith(".device") ? p[k].substring(0, p[k].length() - 7) : p[k];
                if (base.matches("[0-9A-Fa-f]{16}")) l.others.add(base.toUpperCase(Locale.US));
            }
        }
        l.others.remove(title.toUpperCase(Locale.US));
        if (l.prefix != null) for (String n : names) if (!n.endsWith("/") && n.regionMatches(true, 0, l.prefix, 0, l.prefix.length())) l.files++;
        return l;
    }

    /** name's path under prefix (the save's folder, or its .device folder), null when it is not under it or would leave it */
    static String inside(String name, String prefix) {
        if (!name.regionMatches(true, 0, prefix, 0, prefix.length())) return null;
        String rest = name.substring(prefix.length());
        if (rest.isEmpty()) return null;
        for (String s : rest.split("/")) if (s.equals("..")) return null;
        return rest;
    }

    /** The device save's folder beside the save's: "<title ID>.device/" */
    static String devicePrefix(String prefix) { return prefix.substring(0, prefix.length() - 1) + ".device/"; }

    /** Whether a file's first n bytes (up to 4) start a zip: a local file header, or an empty zip's end record */
    static boolean isZip(byte[] head, int n) { return n >= 4 && head[0] == 'P' && head[1] == 'K' && (head[2] == 3 && head[3] == 4 || head[2] == 5 && head[3] == 6); }

    /** A loose save file's name as the game reads it: no path, and without the " (1)" a picker or browser adds to a second copy; null when no name is left */
    static String looseName(String shown) {
        String n = norm(shown); n = n.substring(n.lastIndexOf('/') + 1).trim().replaceFirst(" \\(\\d+\\)(\\.[^.]*)?$", "$1");
        return n.isEmpty() || n.equals(".") || n.equals("..") ? null : n;
    }

    /** The names in the game's save, sorted and joined, when it has files and none is named as (the file is likely another game's); null otherwise */
    static String otherNames(String as, String[] have) {
        if (have == null || have.length == 0) return null;
        for (String h : have) if (h.equalsIgnoreCase(as)) return null;
        String[] s = have.clone(); Arrays.sort(s); return String.join(", ", s);
    }

    /** The newest backup in saves/ that an import or a folder load made (<stamp>-before-import, <stamp>-before-folder), null none */
    static String lastBackup(String[] names) {
        String best = null;
        if (names != null) for (String n : names) if (n.matches("\\d{8}-\\d{6}.*-before-(import|folder)(-\\d+)?") && (best == null || n.compareTo(best) > 0)) best = n;
        return best;
    }

    /** A saves/ folder's stamp as a date ("20261122-104038-before-import" → "2026-11-22 10:40"), the name itself when it has none */
    static String stampLabel(String name) {
        if (!name.matches("\\d{8}-\\d{6}.*")) return name;
        return name.substring(0, 4) + "-" + name.substring(4, 6) + "-" + name.substring(6, 8) + " " + name.substring(9, 11) + ":" + name.substring(11, 13);
    }

    private static String join(String[] p, int k) { StringBuilder b = new StringBuilder(); for (int i = 0; i < k; i++) b.append(p[i]).append('/'); return b.toString(); }
}
