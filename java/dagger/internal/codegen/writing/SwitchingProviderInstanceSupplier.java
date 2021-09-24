/*
 * Copyright (C) 2021 The Dagger Authors.
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

package dagger.internal.codegen.writing;

import static dagger.internal.codegen.binding.BindingRequest.bindingRequest;
import static dagger.internal.codegen.writing.BindingRepresentations.scope;
import static dagger.internal.codegen.writing.ProvisionBindingRepresentation.requiresMethodEncapsulation;
import static dagger.internal.codegen.writing.ProvisionBindingRepresentation.usesDirectInstanceExpression;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import dagger.internal.codegen.binding.BindingGraph;
import dagger.internal.codegen.binding.BindingRequest;
import dagger.internal.codegen.binding.ProvisionBinding;
import dagger.internal.codegen.compileroption.CompilerOptions;
import dagger.internal.codegen.writing.FrameworkFieldInitializer.FrameworkInstanceCreationExpression;
import dagger.spi.model.RequestKind;

/**
 * An object that initializes a framework-type component field for a binding using instances created
 * by switching providers.
 */
final class SwitchingProviderInstanceSupplier implements FrameworkInstanceSupplier {
  private final FrameworkInstanceSupplier frameworkInstanceSupplier;

  @AssistedInject
  SwitchingProviderInstanceSupplier(
      @Assisted ProvisionBinding binding,
      @Assisted DirectInstanceBindingRepresentation directInstanceBindingRepresentation,
      SwitchingProviders switchingProviders,
      BindingGraph graph,
      ComponentImplementation componentImplementation,
      PrivateMethodRequestRepresentation.Factory privateMethodRequestRepresentationFactory,
      UnscopedDirectInstanceRequestRepresentationFactory
          unscopedDirectInstanceRequestRepresentationFactory,
      FrameworkInstanceBindingRepresentation.Factory frameworkInstanceBindingRepresentationFactory,
      CompilerOptions compilerOptions) {
    // First try to get the instance expression via getRequestRepresentation(). However, if that
    // expression is a DerivedFromFrameworkInstanceRequestRepresentation (e.g. fooProvider.get()),
    // then we can't use it to create an instance within the SwitchingProvider since that would
    // cause a cycle. In such cases, we try to use the unscopedDirectInstanceRequestRepresentation
    // directly.
    BindingRequest instanceRequest = bindingRequest(binding.key(), RequestKind.INSTANCE);
    FrameworkInstanceCreationExpression frameworkInstanceCreationExpression =
        switchingProviders.newFrameworkInstanceCreationExpression(
            binding,
            usesDirectInstanceExpression(RequestKind.INSTANCE, binding, graph, true)
                ? directInstanceBindingRepresentation.getRequestRepresentation(instanceRequest)
                : (requiresMethodEncapsulation(binding)
                    ? privateMethodRequestRepresentationFactory.create(
                        instanceRequest,
                        binding,
                        unscopedDirectInstanceRequestRepresentationFactory.create(binding))
                    : unscopedDirectInstanceRequestRepresentationFactory.create(binding)));
    this.frameworkInstanceSupplier =
        new FrameworkFieldInitializer(
            componentImplementation,
            binding,
            binding.scope().isPresent()
                ? scope(binding, frameworkInstanceCreationExpression)
                : frameworkInstanceCreationExpression);
  }

  @Override
  public MemberSelect memberSelect() {
    return frameworkInstanceSupplier.memberSelect();
  }

  @AssistedFactory
  static interface Factory {
    SwitchingProviderInstanceSupplier create(
        ProvisionBinding binding,
        DirectInstanceBindingRepresentation directInstanceBindingRepresentation);
  }
}
