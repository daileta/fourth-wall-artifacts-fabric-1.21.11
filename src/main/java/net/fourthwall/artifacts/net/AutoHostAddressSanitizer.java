package net.fourthwall.artifacts.net;

public final class AutoHostAddressSanitizer {
    private AutoHostAddressSanitizer() {
    }

    public static String sanitize(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        String sanitized = address.trim();

        int bungeeSeparator = sanitized.indexOf('\0');
        if (bungeeSeparator >= 0) {
            sanitized = sanitized.substring(0, bungeeSeparator);
        }

        int proxySeparator = sanitized.indexOf("///");
        if (proxySeparator >= 0) {
            sanitized = sanitized.substring(0, proxySeparator);
        }

        int slashIndex = sanitized.indexOf('/');
        if (slashIndex >= 0) {
            sanitized = sanitized.substring(0, slashIndex);
        }

        sanitized = stripPort(sanitized.trim());
        return sanitized.isBlank() ? address.trim() : sanitized;
    }

    private static String stripPort(String address) {
        if (address.startsWith("[")) {
            int bracketEnd = address.indexOf(']');
            if (bracketEnd > 0) {
                return address.substring(0, bracketEnd + 1);
            }
            return address;
        }

        int firstColon = address.indexOf(':');
        int lastColon = address.lastIndexOf(':');
        if (firstColon > -1 && firstColon == lastColon) {
            String maybePort = address.substring(lastColon + 1);
            if (isNumeric(maybePort)) {
                return address.substring(0, lastColon);
            }
        }

        return address;
    }

    private static boolean isNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }

        return true;
    }
}
