// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/google/protobuf/descriptor.proto
package com.google.protobuf;

import com.squareup.wire.ExtendableMessage;
import com.squareup.wire.Extension;
import com.squareup.wire.ProtoField;
import java.util.Collections;
import java.util.List;

import static com.squareup.wire.Message.Datatype.BOOL;
import static com.squareup.wire.Message.Label.REPEATED;

public final class MessageOptions extends ExtendableMessage<MessageOptions> {
  private static final long serialVersionUID = 0L;

  public static final Boolean DEFAULT_MESSAGE_SET_WIRE_FORMAT = false;
  public static final Boolean DEFAULT_NO_STANDARD_DESCRIPTOR_ACCESSOR = false;
  public static final List<UninterpretedOption> DEFAULT_UNINTERPRETED_OPTION = Collections.emptyList();

  /**
   * Set true to use the old proto1 MessageSet wire format for extensions.
   * This is provided for backwards-compatibility with the MessageSet wire
   * format.  You should not use this for any other reason:  It's less
   * efficient, has fewer features, and is more complicated.
   *
   * The message must be defined exactly as follows:
   *   message Foo {
   *     option message_set_wire_format = true;
   *     extensions 4 to max;
   *   }
   * Note that the message cannot have any defined fields; MessageSets only
   * have extensions.
   *
   * All extensions of your type must be singular messages; e.g. they cannot
   * be int32s, enums, or repeated messages.
   *
   * Because this is an option, the above two restrictions are not enforced by
   * the protocol compiler.
   */
  @ProtoField(tag = 1, type = BOOL)
  public final Boolean message_set_wire_format;

  /**
   * Disables the generation of the standard "descriptor()" accessor, which can
   * conflict with a field of the same name.  This is meant to make migration
   * from proto1 easier; new code should avoid fields named "descriptor".
   */
  @ProtoField(tag = 2, type = BOOL)
  public final Boolean no_standard_descriptor_accessor;

  /**
   * The parser stores options it doesn't recognize here. See above.
   */
  @ProtoField(tag = 999, label = REPEATED, messageType = UninterpretedOption.class)
  public final List<UninterpretedOption> uninterpreted_option;

  public MessageOptions(Boolean message_set_wire_format, Boolean no_standard_descriptor_accessor, List<UninterpretedOption> uninterpreted_option) {
    this.message_set_wire_format = message_set_wire_format;
    this.no_standard_descriptor_accessor = no_standard_descriptor_accessor;
    this.uninterpreted_option = immutableCopyOf(uninterpreted_option);
  }

  private MessageOptions(Builder builder) {
    this(builder.message_set_wire_format, builder.no_standard_descriptor_accessor, builder.uninterpreted_option);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof MessageOptions)) return false;
    MessageOptions o = (MessageOptions) other;
    if (!extensionsEqual(o)) return false;
    return equals(message_set_wire_format, o.message_set_wire_format)
        && equals(no_standard_descriptor_accessor, o.no_standard_descriptor_accessor)
        && equals(uninterpreted_option, o.uninterpreted_option);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = extensionsHashCode();
      result = result * 37 + (message_set_wire_format != null ? message_set_wire_format.hashCode() : 0);
      result = result * 37 + (no_standard_descriptor_accessor != null ? no_standard_descriptor_accessor.hashCode() : 0);
      result = result * 37 + (uninterpreted_option != null ? uninterpreted_option.hashCode() : 1);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends ExtendableBuilder<MessageOptions> {

    public Boolean message_set_wire_format;
    public Boolean no_standard_descriptor_accessor;
    public List<UninterpretedOption> uninterpreted_option;

    public Builder() {
    }

    public Builder(MessageOptions message) {
      super(message);
      if (message == null) return;
      this.message_set_wire_format = message.message_set_wire_format;
      this.no_standard_descriptor_accessor = message.no_standard_descriptor_accessor;
      this.uninterpreted_option = copyOf(message.uninterpreted_option);
    }

    /**
     * Set true to use the old proto1 MessageSet wire format for extensions.
     * This is provided for backwards-compatibility with the MessageSet wire
     * format.  You should not use this for any other reason:  It's less
     * efficient, has fewer features, and is more complicated.
     *
     * The message must be defined exactly as follows:
     *   message Foo {
     *     option message_set_wire_format = true;
     *     extensions 4 to max;
     *   }
     * Note that the message cannot have any defined fields; MessageSets only
     * have extensions.
     *
     * All extensions of your type must be singular messages; e.g. they cannot
     * be int32s, enums, or repeated messages.
     *
     * Because this is an option, the above two restrictions are not enforced by
     * the protocol compiler.
     */
    public Builder message_set_wire_format(Boolean message_set_wire_format) {
      this.message_set_wire_format = message_set_wire_format;
      return this;
    }

    /**
     * Disables the generation of the standard "descriptor()" accessor, which can
     * conflict with a field of the same name.  This is meant to make migration
     * from proto1 easier; new code should avoid fields named "descriptor".
     */
    public Builder no_standard_descriptor_accessor(Boolean no_standard_descriptor_accessor) {
      this.no_standard_descriptor_accessor = no_standard_descriptor_accessor;
      return this;
    }

    /**
     * The parser stores options it doesn't recognize here. See above.
     */
    public Builder uninterpreted_option(List<UninterpretedOption> uninterpreted_option) {
      this.uninterpreted_option = checkForNulls(uninterpreted_option);
      return this;
    }

    @Override
    public <E> Builder setExtension(Extension<MessageOptions, E> extension, E value) {
      super.setExtension(extension, value);
      return this;
    }

    @Override
    public MessageOptions build() {
      return new MessageOptions(this);
    }
  }
}
