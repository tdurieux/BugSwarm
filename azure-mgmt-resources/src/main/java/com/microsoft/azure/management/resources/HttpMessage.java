/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 *
 * Code generated by Microsoft (R) AutoRest Code Generator.
 */

package com.microsoft.azure.management.resources;


/**
 * The HttpMessage model.
 */
public class HttpMessage {
    /**
     * Gets or sets HTTP message content.
     */
    private Object content;

    /**
     * Get the content value.
     *
     * @return the content value
     */
    public Object content() {
        return this.content;
    }

    /**
     * Set the content value.
     *
     * @param content the content value to set
     * @return the HttpMessage object itself.
     */
    public HttpMessage withContent(Object content) {
        this.content = content;
        return this;
    }

}
