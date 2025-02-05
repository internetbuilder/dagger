/*
 * Copyright (C) 2019 The Dagger Authors.
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

package dagger.internal.codegen.base;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static com.google.auto.common.MoreTypes.asTypeElement;
import static com.google.common.base.Preconditions.checkArgument;
import static dagger.internal.codegen.base.MoreAnnotationValues.asAnnotationValues;
import static dagger.internal.codegen.extension.DaggerStreams.toImmutableList;
import static dagger.internal.codegen.langmodel.DaggerElements.getAnyAnnotation;

import com.google.auto.common.MoreTypes;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import dagger.internal.codegen.javapoet.TypeNames;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/** A {@code @Module} or {@code @ProducerModule} annotation. */
@AutoValue
public abstract class ModuleAnnotation {
  private static final ImmutableSet<ClassName> MODULE_ANNOTATIONS =
      ImmutableSet.of(TypeNames.MODULE, TypeNames.PRODUCER_MODULE);

  /** The annotation itself. */
  // This does not use AnnotationMirrors.equivalence() because we want the actual annotation
  // instance.
  public abstract AnnotationMirror annotation();

  /** The simple name of the annotation. */
  public String annotationName() {
    return annotation().getAnnotationType().asElement().getSimpleName().toString();
  }

  /**
   * The types specified in the {@code includes} attribute.
   *
   * @throws IllegalArgumentException if any of the values are error types
   */
  @Memoized
  public ImmutableList<TypeElement> includes() {
    return includesAsAnnotationValues().stream()
        .map(MoreAnnotationValues::asType)
        .map(MoreTypes::asTypeElement)
        .collect(toImmutableList());
  }

  /** The values specified in the {@code includes} attribute. */
  @Memoized
  public ImmutableList<AnnotationValue> includesAsAnnotationValues() {
    return asAnnotationValues(getAnnotationValue(annotation(), "includes"));
  }

  /**
   * The types specified in the {@code subcomponents} attribute.
   *
   * @throws IllegalArgumentException if any of the values are error types
   */
  @Memoized
  public ImmutableList<TypeElement> subcomponents() {
    return subcomponentsAsAnnotationValues().stream()
        .map(MoreAnnotationValues::asType)
        .map(MoreTypes::asTypeElement)
        .collect(toImmutableList());
  }

  /** The values specified in the {@code subcomponents} attribute. */
  @Memoized
  public ImmutableList<AnnotationValue> subcomponentsAsAnnotationValues() {
    return asAnnotationValues(getAnnotationValue(annotation(), "subcomponents"));
  }

  /** Returns {@code true} if the argument is a {@code @Module} or {@code @ProducerModule}. */
  public static boolean isModuleAnnotation(AnnotationMirror annotation) {
    return MODULE_ANNOTATIONS.stream()
        .map(ClassName::canonicalName)
        .anyMatch(asTypeElement(annotation.getAnnotationType()).getQualifiedName()::contentEquals);
  }

  /** The module annotation types. */
  public static ImmutableSet<ClassName> moduleAnnotations() {
    return MODULE_ANNOTATIONS;
  }

  /**
   * Creates an object that represents a {@code @Module} or {@code @ProducerModule}.
   *
   * @throws IllegalArgumentException if {@link #isModuleAnnotation(AnnotationMirror)} returns
   *     {@code false}
   */
  public static ModuleAnnotation moduleAnnotation(AnnotationMirror annotation) {
    checkArgument(
        isModuleAnnotation(annotation),
        "%s is not a Module or ProducerModule annotation",
        annotation);
    return new AutoValue_ModuleAnnotation(annotation);
  }

  /**
   * Returns an object representing the {@code @Module} or {@code @ProducerModule} annotation if one
   * annotates {@code typeElement}.
   */
  public static Optional<ModuleAnnotation> moduleAnnotation(Element element) {
    return getAnyAnnotation(element, TypeNames.MODULE, TypeNames.PRODUCER_MODULE)
        .map(ModuleAnnotation::moduleAnnotation);
  }
}
