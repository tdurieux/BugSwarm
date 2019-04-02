/**
 * This code was generated by
 * \ / _    _  _|   _  _
 *  | (_)\/(_)(_|\/| |(/_  v1.0.0
 *       /       /
 */

package com.twilio.rest.chat.v2;

import com.twilio.base.Updater;
import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import com.twilio.exception.RestException;
import com.twilio.http.HttpMethod;
import com.twilio.http.Request;
import com.twilio.http.Response;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.Domains;

public class CredentialUpdater extends Updater<Credential> {
    private final String pathSid;
    private String friendlyName;
    private String certificate;
    private String privateKey;
    private Boolean sandbox;
    private String apiKey;
    private String secret;

    /**
     * Construct a new CredentialUpdater.
     * 
     * @param pathSid The sid
     */
    public CredentialUpdater(final String pathSid) {
        this.pathSid = pathSid;
    }

    /**
     * The friendly_name.
     * 
     * @param friendlyName The friendly_name
     * @return this
     */
    public CredentialUpdater setFriendlyName(final String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
    }

    /**
     * The certificate.
     * 
     * @param certificate The certificate
     * @return this
     */
    public CredentialUpdater setCertificate(final String certificate) {
        this.certificate = certificate;
        return this;
    }

    /**
     * The private_key.
     * 
     * @param privateKey The private_key
     * @return this
     */
    public CredentialUpdater setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    /**
     * The sandbox.
     * 
     * @param sandbox The sandbox
     * @return this
     */
    public CredentialUpdater setSandbox(final Boolean sandbox) {
        this.sandbox = sandbox;
        return this;
    }

    /**
     * The api_key.
     * 
     * @param apiKey The api_key
     * @return this
     */
    public CredentialUpdater setApiKey(final String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * The secret.
     * 
     * @param secret The secret
     * @return this
     */
    public CredentialUpdater setSecret(final String secret) {
        this.secret = secret;
        return this;
    }

    /**
     * Make the request to the Twilio API to perform the update.
     * 
     * @param client TwilioRestClient with which to make the request
     * @return Updated Credential
     */
    @Override
    @SuppressWarnings("checkstyle:linelength")
    public Credential update(final TwilioRestClient client) {
        Request request = new Request(
            HttpMethod.POST,
            Domains.CHAT.toString(),
            "/v2/Credentials/" + this.pathSid + "",
            client.getRegion()
        );

        addPostParams(request);
        Response response = client.request(request);

        if (response == null) {
            throw new ApiConnectionException("Credential update failed: Unable to connect to server");
        } else if (!TwilioRestClient.SUCCESS.apply(response.getStatusCode())) {
            RestException restException = RestException.fromJson(response.getStream(), client.getObjectMapper());
            if (restException == null) {
                throw new ApiException("Server Error, no content");
            }

            throw new ApiException(
                restException.getMessage(),
                restException.getCode(),
                restException.getMoreInfo(),
                restException.getStatus(),
                null
            );
        }

        return Credential.fromJson(response.getStream(), client.getObjectMapper());
    }

    /**
     * Add the requested post parameters to the Request.
     * 
     * @param request Request to add post params to
     */
    private void addPostParams(final Request request) {
        if (friendlyName != null) {
            request.addPostParam("FriendlyName", friendlyName);
        }

        if (certificate != null) {
            request.addPostParam("Certificate", certificate);
        }

        if (privateKey != null) {
            request.addPostParam("PrivateKey", privateKey);
        }

        if (sandbox != null) {
            request.addPostParam("Sandbox", sandbox.toString());
        }

        if (apiKey != null) {
            request.addPostParam("ApiKey", apiKey);
        }

        if (secret != null) {
            request.addPostParam("Secret", secret);
        }
    }
}