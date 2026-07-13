package com.whatiread.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NetworkUtilsTest {

    @Test
    void recognizesLoopbackAsInternal() {
        assertThat(NetworkUtils.isInternalNetwork("127.0.0.1")).isTrue();
        assertThat(NetworkUtils.isInternalNetwork("::1")).isTrue();
    }

    @Test
    void recognizesPrivateRangesAsInternal() {
        assertThat(NetworkUtils.isInternalNetwork("10.0.0.5")).isTrue();
        assertThat(NetworkUtils.isInternalNetwork("172.17.0.2")).isTrue();
        assertThat(NetworkUtils.isInternalNetwork("192.168.1.10")).isTrue();
    }

    @Test
    void rejectsPublicAddresses() {
        assertThat(NetworkUtils.isInternalNetwork("8.8.8.8")).isFalse();
        assertThat(NetworkUtils.isInternalNetwork("1.1.1.1")).isFalse();
    }

    @Test
    void rejectsBlankAndUnresolvableAddresses() {
        assertThat(NetworkUtils.isInternalNetwork("")).isFalse();
        assertThat(NetworkUtils.isInternalNetwork("   ")).isFalse();
        assertThat(NetworkUtils.isInternalNetwork("not-a-valid-host")).isFalse();
    }

    @Test
    void recognizesLinkLocalAddresses() {
        assertThat(NetworkUtils.isInternalNetwork("169.254.10.1")).isTrue();
    }
}
