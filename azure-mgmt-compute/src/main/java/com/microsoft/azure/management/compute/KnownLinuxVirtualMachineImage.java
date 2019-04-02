package com.microsoft.azure.management.compute;

/**
 * The popular Azure Linux images.
 */
public enum KnownLinuxVirtualMachineImage {
    /** UbuntuServer 14.04LTS. */
    UBUNTU_SERVER_14_04_LTS("Canonical", "UbuntuServer", "14.04.4-LTS"),
    /** UbuntuServer 16.04LTS. */
    UBUNTU_SERVER_16_04_LTS("Canonical", "UbuntuServer", "16.04.0-LTS"),
    /** Debian 8. */
    DEBIAN_8("credativ", "Debian", "8"),
    /** CentOS 7.2. */
    CENTOS_7_2("OpenLogic", "CentOS", "7.2"),
    /** OpenSUSE-Leap 42.1. */
    OPENSUSE_LEAP_42_1("SUSE", "openSUSE-Leap", "42.1"),
    /** SLES 12-SP1. */
    SLES_12_SP1("SUSE", "SLES", "12-SP1");

    private final String publisher;
    private final String offer;
    private final String sku;

    KnownLinuxVirtualMachineImage(String publisher, String offer, String sku) {
        this.publisher = publisher;
        this.offer = offer;
        this.sku = sku;
    }

    /**
     * @return the name of the image publisher
     */
    public String publisher() {
        return this.publisher;
    }

    /**
     * @return the name of the image offer
     */
    public String offer() {
        return this.offer;
    }

    /**
     * @return the name of the image SKU
     */
    public String sku() {
        return this.sku;
    }

    /**
     * @return the image reference
     */
    public ImageReference imageReference() {
        return new ImageReference()
                .withPublisher(publisher())
                .withOffer(offer())
                .withSku(sku())
                .withVersion("latest");
    }
}
