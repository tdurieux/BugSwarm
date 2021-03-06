/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.javawriter.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javawriter.TypeName;
import com.squareup.javawriter.TypeNames;
import com.squareup.javawriter.VoidName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.lang.model.element.Modifier;

import static com.google.common.base.Preconditions.checkNotNull;

/** A generated method declaration. */
public final class MethodSpec {
  public final ImmutableList<AnnotationSpec> annotations;
  public final ImmutableSet<Modifier> modifiers;
  public final TypeName returnType;
  public final Name name;
  public final ImmutableList<ParameterSpec> parameters;
  public final ImmutableList<Snippet> snippets;

  private MethodSpec(Builder builder) {
    this.annotations = ImmutableList.copyOf(builder.annotations);
    this.modifiers = ImmutableSet.copyOf(builder.modifiers);
    this.returnType = builder.returnType;
    this.name = checkNotNull(builder.name);
    this.parameters = ImmutableList.copyOf(builder.parameters);
    this.snippets = ImmutableList.copyOf(builder.snippets);
  }

  void emit(CodeWriter codeWriter) {
    codeWriter.emitAnnotations(annotations);
    codeWriter.emitModifiers(modifiers);
    codeWriter.emit("$T $L(", returnType, name);

    boolean firstParameter = true;
    for (ParameterSpec parameterSpec : parameters) {
      if (!firstParameter) codeWriter.emit(", ");
      parameterSpec.emit(codeWriter);
      firstParameter = false;
    }
    codeWriter.emit(") {\n");

    codeWriter.indent();
    for (Snippet snippet : snippets) {
      codeWriter.emit(snippet);
    }
    codeWriter.unindent();

    codeWriter.emit("}\n");
  }

  public static final class Builder {
    private final List<AnnotationSpec> annotations = new ArrayList<>();
    private final List<Modifier> modifiers = new ArrayList<>();
    private TypeName returnType = VoidName.VOID;
    private Name name;
    private final List<ParameterSpec> parameters = new ArrayList<>();
    private final List<Snippet> snippets = new ArrayList<>();

    public Builder addAnnotation(AnnotationSpec annotationSpec) {
      this.annotations.add(annotationSpec);
      return this;
    }

    public Builder addAnnotation(Class<? extends Annotation> annotation) {
      this.annotations.add(AnnotationSpec.of(annotation));
      return this;
    }

    public Builder addModifiers(Modifier... modifiers) {
      this.modifiers.addAll(Arrays.asList(modifiers));
      return this;
    }

    public Builder returns(Class<?> returnType) {
      return returns(TypeNames.forClass(returnType));
    }

    public Builder returns(TypeName returnType) {
      this.returnType = returnType;
      return this;
    }

    public Builder name(String name) {
      this.name = new Name(name);
      return this;
    }

    public Builder name(Name name) {
      this.name = name;
      return this;
    }

    public Builder addParameter(ParameterSpec parameterSpec) {
      this.parameters.add(parameterSpec);
      return this;
    }

    public Builder addParameter(Class<?> type, String name) {
      return addParameter(new ParameterSpec.Builder().type(type).name(name).build());
    }

    public Builder addParameter(TypeName type, String name) {
      return addParameter(new ParameterSpec.Builder().type(type).name(name).build());
    }

    public Builder addCode(String format, Object... args) {
      snippets.add(new Snippet(format, args));
      return this;
    }

    public MethodSpec build() {
      return new MethodSpec(this);
    }
  }
}
