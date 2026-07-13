package com.whatiread.shared.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.util.StringUtils;

public final class NetworkUtils {

    private NetworkUtils() {
    }

    public static boolean isInternalNetwork(String address) {
        if (!StringUtils.hasText(address)) {
            return false;
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            return inetAddress.isLoopbackAddress()
                    || inetAddress.isSiteLocalAddress()
                    || inetAddress.isLinkLocalAddress();
        } catch (UnknownHostException ignored) {
            return false;
        }
    }
}
