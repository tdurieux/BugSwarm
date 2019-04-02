/**
 * This code was generated by
 * \ / _    _  _|   _  _
 *  | (_)\/(_)(_|\/| |(/_  v1.0.0
 *       /       /
 */

package com.twilio.rest.chat.v1.service.channel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.twilio.base.Resource;
import com.twilio.converter.DateConverter;
import com.twilio.converter.Promoter;
import com.twilio.exception.ApiConnectionException;
import com.twilio.exception.ApiException;
import com.twilio.exception.RestException;
import com.twilio.http.HttpMethod;
import com.twilio.http.Request;
import com.twilio.http.Response;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.Domains;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message extends Resource {
    private static final long serialVersionUID = 68490171213295L;

    public enum OrderType {
        ASC("asc"),
        DESC("desc");

        private final String value;

        private OrderType(final String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Generate a OrderType from a string.
         * @param value string value
         * @return generated OrderType
         */
        @JsonCreator
        public static OrderType forValue(final String value) {
            return Promoter.enumFromString(value, OrderType.values());
        }
    }

    /**
     * Create a MessageFetcher to execute fetch.
     * 
     * @param pathServiceSid The service_sid
     * @param pathChannelSid The channel_sid
     * @param pathSid The sid
     * @return MessageFetcher capable of executing the fetch
     */
    public static MessageFetcher fetcher(final String pathServiceSid, 
                                         final String pathChannelSid, 
                                         final String pathSid) {
        return new MessageFetcher(pathServiceSid, pathChannelSid, pathSid);
    }

    /**
     * Create a MessageCreator to execute create.
     * 
     * @param pathServiceSid The service_sid
     * @param pathChannelSid The channel_sid
     * @param body The body
     * @return MessageCreator capable of executing the create
     */
    public static MessageCreator creator(final String pathServiceSid, 
                                         final String pathChannelSid, 
                                         final String body) {
        return new MessageCreator(pathServiceSid, pathChannelSid, body);
    }

    /**
     * Create a MessageReader to execute read.
     * 
     * @param pathServiceSid The service_sid
     * @param pathChannelSid The channel_sid
     * @return MessageReader capable of executing the read
     */
    public static MessageReader reader(final String pathServiceSid, 
                                       final String pathChannelSid) {
        return new MessageReader(pathServiceSid, pathChannelSid);
    }

    /**
     * Create a MessageDeleter to execute delete.
     * 
     * @param pathServiceSid The service_sid
     * @param pathChannelSid The channel_sid
     * @param pathSid The sid
     * @return MessageDeleter capable of executing the delete
     */
    public static MessageDeleter deleter(final String pathServiceSid, 
                                         final String pathChannelSid, 
                                         final String pathSid) {
        return new MessageDeleter(pathServiceSid, pathChannelSid, pathSid);
    }

    /**
     * Create a MessageUpdater to execute update.
     * 
     * @param pathServiceSid The service_sid
     * @param pathChannelSid The channel_sid
     * @param pathSid The sid
     * @return MessageUpdater capable of executing the update
     */
    public static MessageUpdater updater(final String pathServiceSid, 
                                         final String pathChannelSid, 
                                         final String pathSid) {
        return new MessageUpdater(pathServiceSid, pathChannelSid, pathSid);
    }

    /**
     * Converts a JSON String into a Message object using the provided ObjectMapper.
     * 
     * @param json Raw JSON String
     * @param objectMapper Jackson ObjectMapper
     * @return Message object represented by the provided JSON
     */
    public static Message fromJson(final String json, final ObjectMapper objectMapper) {
        // Convert all checked exceptions to Runtime
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (final JsonMappingException | JsonParseException e) {
            throw new ApiException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new ApiConnectionException(e.getMessage(), e);
        }
    }

    /**
     * Converts a JSON InputStream into a Message object using the provided
     * ObjectMapper.
     * 
     * @param json Raw JSON InputStream
     * @param objectMapper Jackson ObjectMapper
     * @return Message object represented by the provided JSON
     */
    public static Message fromJson(final InputStream json, final ObjectMapper objectMapper) {
        // Convert all checked exceptions to Runtime
        try {
            return objectMapper.readValue(json, Message.class);
        } catch (final JsonMappingException | JsonParseException e) {
            throw new ApiException(e.getMessage(), e);
        } catch (final IOException e) {
            throw new ApiConnectionException(e.getMessage(), e);
        }
    }

    private final String sid;
    private final String accountSid;
    private final String attributes;
    private final String serviceSid;
    private final String to;
    private final String channelSid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Boolean wasEdited;
    private final String from;
    private final String body;
    private final Integer index;
    private final URI url;

    @JsonCreator
    private Message(@JsonProperty("sid")
                    final String sid, 
                    @JsonProperty("account_sid")
                    final String accountSid, 
                    @JsonProperty("attributes")
                    final String attributes, 
                    @JsonProperty("service_sid")
                    final String serviceSid, 
                    @JsonProperty("to")
                    final String to, 
                    @JsonProperty("channel_sid")
                    final String channelSid, 
                    @JsonProperty("date_created")
                    final String dateCreated, 
                    @JsonProperty("date_updated")
                    final String dateUpdated, 
                    @JsonProperty("was_edited")
                    final Boolean wasEdited, 
                    @JsonProperty("from")
                    final String from, 
                    @JsonProperty("body")
                    final String body, 
                    @JsonProperty("index")
                    final Integer index, 
                    @JsonProperty("url")
                    final URI url) {
        this.sid = sid;
        this.accountSid = accountSid;
        this.attributes = attributes;
        this.serviceSid = serviceSid;
        this.to = to;
        this.channelSid = channelSid;
        this.dateCreated = DateConverter.iso8601DateTimeFromString(dateCreated);
        this.dateUpdated = DateConverter.iso8601DateTimeFromString(dateUpdated);
        this.wasEdited = wasEdited;
        this.from = from;
        this.body = body;
        this.index = index;
        this.url = url;
    }

    /**
     * Returns The The sid.
     * 
     * @return The sid
     */
    public final String getSid() {
        return this.sid;
    }

    /**
     * Returns The The account_sid.
     * 
     * @return The account_sid
     */
    public final String getAccountSid() {
        return this.accountSid;
    }

    /**
     * Returns The The attributes.
     * 
     * @return The attributes
     */
    public final String getAttributes() {
        return this.attributes;
    }

    /**
     * Returns The The service_sid.
     * 
     * @return The service_sid
     */
    public final String getServiceSid() {
        return this.serviceSid;
    }

    /**
     * Returns The The to.
     * 
     * @return The to
     */
    public final String getTo() {
        return this.to;
    }

    /**
     * Returns The The channel_sid.
     * 
     * @return The channel_sid
     */
    public final String getChannelSid() {
        return this.channelSid;
    }

    /**
     * Returns The The date_created.
     * 
     * @return The date_created
     */
    public final DateTime getDateCreated() {
        return this.dateCreated;
    }

    /**
     * Returns The The date_updated.
     * 
     * @return The date_updated
     */
    public final DateTime getDateUpdated() {
        return this.dateUpdated;
    }

    /**
     * Returns The The was_edited.
     * 
     * @return The was_edited
     */
    public final Boolean getWasEdited() {
        return this.wasEdited;
    }

    /**
     * Returns The The from.
     * 
     * @return The from
     */
    public final String getFrom() {
        return this.from;
    }

    /**
     * Returns The The body.
     * 
     * @return The body
     */
    public final String getBody() {
        return this.body;
    }

    /**
     * Returns The The index.
     * 
     * @return The index
     */
    public final Integer getIndex() {
        return this.index;
    }

    /**
     * Returns The The url.
     * 
     * @return The url
     */
    public final URI getUrl() {
        return this.url;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Message other = (Message) o;

        return Objects.equals(sid, other.sid) && 
               Objects.equals(accountSid, other.accountSid) && 
               Objects.equals(attributes, other.attributes) && 
               Objects.equals(serviceSid, other.serviceSid) && 
               Objects.equals(to, other.to) && 
               Objects.equals(channelSid, other.channelSid) && 
               Objects.equals(dateCreated, other.dateCreated) && 
               Objects.equals(dateUpdated, other.dateUpdated) && 
               Objects.equals(wasEdited, other.wasEdited) && 
               Objects.equals(from, other.from) && 
               Objects.equals(body, other.body) && 
               Objects.equals(index, other.index) && 
               Objects.equals(url, other.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sid,
                            accountSid,
                            attributes,
                            serviceSid,
                            to,
                            channelSid,
                            dateCreated,
                            dateUpdated,
                            wasEdited,
                            from,
                            body,
                            index,
                            url);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sid", sid)
                          .add("accountSid", accountSid)
                          .add("attributes", attributes)
                          .add("serviceSid", serviceSid)
                          .add("to", to)
                          .add("channelSid", channelSid)
                          .add("dateCreated", dateCreated)
                          .add("dateUpdated", dateUpdated)
                          .add("wasEdited", wasEdited)
                          .add("from", from)
                          .add("body", body)
                          .add("index", index)
                          .add("url", url)
                          .toString();
    }
}